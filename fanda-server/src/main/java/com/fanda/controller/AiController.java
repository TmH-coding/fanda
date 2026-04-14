package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.ai.FandaAiTools;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.User;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI 智能体控制器
 * POST /api/ai/chat          - 智能饮食顾问（流式对话）
 * POST /api/ai/recommend     - 菜品推荐 Agent（带 Tools）
 * GET  /api/ai/nutrition     - 营养分析报告
 * POST /api/ai/social-topic  - 拼饭话题助手
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ChatClient chatClient;
    private final FandaAiTools fandaAiTools;
    private final UserMapper userMapper;

    // ─────────────────────────────────────────────────────────────
    // 1. 智能饮食顾问 — 流式对话，返回 SSE
    // ─────────────────────────────────────────────────────────────
    @PostMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chat(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        String message = body.getOrDefault("message", "");

        // 查询用户偏好和近期记录作为上下文
        String prefs = fandaAiTools.getUserPreferences(userId);
        String recentMeals = fandaAiTools.getRecentMealRecords(userId, 7);
        String budget = fandaAiTools.getBudgetStatus(userId);

        String systemPrompt = """
                你是"饭搭"App的智能饮食顾问小饭，专注于帮助都市打工人做饮食决策。
                你的风格亲切、接地气，偶尔使用轻松幽默的语气，但建议要实用、具体。

                【用户当前数据】
                偏好设置：%s
                近7天用餐记录：
                %s
                预算状态：%s

                回答要简洁，不超过200字。如果用户问今天吃什么，结合他的忌口和近期记录给出1-3个具体建议。
                """.formatted(prefs, recentMeals, budget);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .stream()
                .content();
    }

    // ─────────────────────────────────────────────────────────────
    // 2. 菜品推荐 Agent — 带 Function Calling，AI 主动查数据
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/recommend")
    public ApiResponse<String> recommend(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        String scene = body.getOrDefault("scene", "午餐");  // 早餐/午餐/晚餐/加餐

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
    // 4. 拼饭话题助手 — 根据群组信息生成破冰话题或活动摘要
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/social-topic")
    public ApiResponse<String> socialTopic(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String groupTitle = body.getOrDefault("groupTitle", "拼饭活动");
        String location = body.getOrDefault("location", "");
        String candidates = body.getOrDefault("candidates", "");
        String type = body.getOrDefault("type", "icebreaker");  // icebreaker | summary

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
    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}
