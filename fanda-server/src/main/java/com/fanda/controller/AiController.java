package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.ai.FandaAiTools;
import com.fanda.ai.FoodImageRecognizer;
import com.fanda.ai.WeeklyReportScheduler;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.User;
import com.fanda.entity.WeeklyReport;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.UserMapper;
import com.fanda.mapper.WeeklyReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 智能体控制器
 * POST /api/ai/chat          - 智能饮食顾问（流式对话，带多轮记忆）
 * POST /api/ai/recommend     - 菜品推荐 Agent（带 Tools）
 * GET  /api/ai/nutrition     - 营养分析报告
 * GET  /api/ai/weekly-report - 每周营养周报（查询/触发生成）
 * POST /api/ai/budget-advice - 智能预算建议
 * POST /api/ai/social-topic  - 拼饭话题助手
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ChatClient chatClient;
    private final FandaAiTools fandaAiTools;
    private final UserMapper userMapper;
    private final WeeklyReportMapper weeklyReportMapper;
    private final WeeklyReportScheduler weeklyReportScheduler;
    private final FoodImageRecognizer foodImageRecognizer;

    // 多轮对话历史：userId -> [{"role":"user/assistant","content":"..."}]
    // 每用户最多保留最近 10 轮（20条消息）
    private final Map<Long, Deque<String>> chatHistories = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 20;

    // ─────────────────────────────────────────────────────────────
    // 1. 智能饮食顾问 — 流式对话，带多轮对话记忆
    // ─────────────────────────────────────────────────────────────
    @PostMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chat(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        String message = body.getOrDefault("message", "");

        // 查询用户数据作为系统上下文
        String prefs = fandaAiTools.getUserPreferences(userId);
        String recentMeals = fandaAiTools.getRecentMealRecords(userId, 7);
        String budget = fandaAiTools.getBudgetStatus(userId);

        // 取历史对话拼入 system prompt
        Deque<String> history = chatHistories.computeIfAbsent(userId, k -> new ArrayDeque<>());
        String historyText = history.isEmpty() ? "（暂无历史对话）" : String.join("\n", history);

        String systemPrompt = """
                你是"饭搭"App的智能饮食顾问小饭，专注于帮助都市打工人做饮食决策。
                你的风格亲切、接地气，偶尔使用轻松幽默的语气，但建议要实用、具体。

                【用户当前数据】
                偏好设置：%s
                近7天用餐记录：
                %s
                预算状态：%s

                【历史对话记录（最近几轮）】
                %s

                回答要简洁，不超过200字。如果用户问今天吃什么，结合他的忌口和近期记录给出1-3个具体建议。
                """.formatted(prefs, recentMeals, budget, historyText);

        // 将本轮用户消息加入历史
        addHistory(history, "用户: " + message);

        // 流式响应，同时收集完整回复用于存入历史
        StringBuilder fullReply = new StringBuilder();
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .stream()
                .content()
                .doOnNext(fullReply::append)
                .doOnComplete(() -> addHistory(history, "小饭: " + fullReply));
    }

    // ─────────────────────────────────────────────────────────────
    // 清除当前用户的对话历史
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping("/chat/history")
    public ApiResponse<Void> clearHistory(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        chatHistories.remove(userId);
        return ApiResponse.ok(null);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. 菜品推荐 Agent — 带 Function Calling，AI 主动查数据
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/recommend")
    public ApiResponse<String> recommend(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        String scene = body.getOrDefault("scene", "午餐");

        String prompt = """
                用户ID是%d，请帮我推荐%s吃什么。
                请先查询用户的口味偏好和近3天用餐记录，避免推荐最近已经吃过的，再搜索几个合适的菜品，
                最终给出2-3个具体推荐，每条包含：菜名、推荐理由（1句话）、大概价格。
                """.formatted(userId, scene);

        String result = chatClient.prompt()
                .user(prompt)
                .tools(fandaAiTools)
                .call()
                .content();

        return ApiResponse.ok(result);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. 营养分析报告 — AI 读取近30天数据后生成报告
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/nutrition")
    public ApiResponse<String> nutritionReport(Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        String nutritionStats = fandaAiTools.getNutritionStats(userId);
        String recentMeals = fandaAiTools.getRecentMealRecords(userId, 30);
        String prefs = fandaAiTools.getUserPreferences(userId);

        String prompt = """
                根据以下用户近30天的饮食数据，生成一份简明的营养摄入分析报告：

                营养统计：%s
                近30天用餐记录（部分）：
                %s
                用户偏好：%s

                报告格式：
                1. 整体评价（1-2句）
                2. 营养亮点（做得好的地方）
                3. 营养不足（缺少的营养素）
                4. 具体建议（2-3条可执行的改进建议）

                语气友善、鼓励，不要太学术，200字以内。
                """.formatted(nutritionStats, recentMeals, prefs);

        String report = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return ApiResponse.ok(report);
    }

    // ─────────────────────────────────────────────────────────────
    // 4. 每周营养周报 — 查询本周报告，不存在则立即生成
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/weekly-report")
    public ApiResponse<String> weeklyReport(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String content = weeklyReportScheduler.generateReportForUser(userId, weekStart);
        if (content == null || content.isBlank()) {
            return ApiResponse.ok("本周暂无足够的饮食记录，多记录几餐再来看报告吧 😊");
        }
        return ApiResponse.ok(content);
    }

    // ─────────────────────────────────────────────────────────────
    // 5. 智能预算建议 — 根据历史消费给出合理月预算
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/budget-advice")
    public ApiResponse<String> budgetAdvice(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        String budget = fandaAiTools.getBudgetStatus(userId);
        String recentMeals = fandaAiTools.getRecentMealRecords(userId, 30);

        String prompt = """
                根据以下用户的预算使用情况和近30天消费记录，给出一个合理的月度餐饮预算建议：

                当前预算状态：%s
                近30天用餐记录（含花费）：
                %s

                请分析用户的实际消费习惯，给出：
                1. 建议月预算金额（给出具体数字）
                2. 分析依据（2-3句，说明为什么这样建议）
                3. 省钱小技巧（1-2条针对该用户消费习惯的具体建议）

                语气友善，不超过150字。
                """.formatted(budget, recentMeals);

        String advice = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return ApiResponse.ok(advice);
    }

    // ─────────────────────────────────────────────────────────────
    // 7. 图片识别菜品 — qwen-vl 视觉模型
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/recognize-food")
    public ApiResponse<String> recognizeFood(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String imageBase64 = body.getOrDefault("imageBase64", "");
        if (imageBase64.isBlank()) {
            return ApiResponse.ok("请提供图片数据");
        }
        String result = foodImageRecognizer.recognize(imageBase64);
        return ApiResponse.ok(result);
    }

    // ─────────────────────────────────────────────────────────────
    // 6. 拼饭话题助手 — 根据群组信息生成破冰话题或活动摘要
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/social-topic")
    public ApiResponse<String> socialTopic(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String groupTitle = body.getOrDefault("groupTitle", "拼饭活动");
        String location = body.getOrDefault("location", "");
        String candidates = body.getOrDefault("candidates", "");
        String type = body.getOrDefault("type", "icebreaker");

        String prompt = switch (type) {
            case "summary" -> """
                    帮我写一条拼饭活动结束的总结消息，发在群里。
                    活动：%s，地点：%s，候选餐厅：%s。
                    要活泼有趣，带点打工人的自嘲，60字以内。
                    """.formatted(groupTitle, location, candidates);
            default -> """
                    帮我想3条拼饭群的破冰话题，适合刚组建的拼饭小队用来活跃气氛。
                    活动主题：%s，地点：%s。
                    每条话题一行，有趣、接地气，不要太正式。
                    """.formatted(groupTitle, location);
        };

        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return ApiResponse.ok(result);
    }

    // ─────────────────────────────────────────────────────────────
    private void addHistory(Deque<String> history, String line) {
        history.addLast(line);
        while (history.size() > MAX_HISTORY) {
            history.pollFirst();
        }
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}
