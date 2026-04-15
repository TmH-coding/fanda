package com.fanda.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.entity.User;
import com.fanda.entity.WeeklyReport;
import com.fanda.mapper.UserMapper;
import com.fanda.mapper.WeeklyReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 每周营养周报定时任务
 * 每周一凌晨 2:00 为所有有记录的用户生成上周饮食报告
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportScheduler {

    private final ChatClient chatClient;
    private final FandaAiTools fandaAiTools;
    private final UserMapper userMapper;
    private final WeeklyReportMapper weeklyReportMapper;

    // 每周一 02:00 执行
    @Scheduled(cron = "0 0 2 * * MON")
    public void generateWeeklyReports() {
        log.info("开始生成每周营养周报...");
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));

        List<User> users = userMapper.selectList(null);
        int success = 0;
        for (User user : users) {
            try {
                generateReportForUser(user.getId(), weekStart);
                success++;
            } catch (Exception e) {
                log.warn("生成用户 {} 的周报失败: {}", user.getId(), e.getMessage());
            }
        }
        log.info("每周营养周报生成完成，成功 {}/{}", success, users.size());
    }

    /**
     * 为指定用户生成本周（或指定周）的营养报告，已存在则跳过
     */
    public String generateReportForUser(Long userId, LocalDate weekStart) {
        // 检查本周报告是否已生成
        WeeklyReport existing = weeklyReportMapper.selectOne(
                new LambdaQueryWrapper<WeeklyReport>()
                        .eq(WeeklyReport::getUserId, userId)
                        .eq(WeeklyReport::getWeekStart, weekStart));
        if (existing != null) {
            return existing.getContent();
        }

        // 查询上周数据（weekStart 是上周一，往后7天）
        String nutritionStats = fandaAiTools.getNutritionStats(userId);
        String recentMeals = fandaAiTools.getRecentMealRecords(userId, 7);
        String prefs = fandaAiTools.getUserPreferences(userId);
        String budget = fandaAiTools.getBudgetStatus(userId);

        if (recentMeals.contains("没有用餐记录")) {
            return "";  // 本周无记录，不生成
        }

        String prompt = """
                根据用户上周（%s 至 %s）的饮食数据，生成一份简洁的每周营养周报：

                营养摄入统计：%s
                用餐记录：
                %s
                口味偏好：%s
                预算状况：%s

                周报格式（带 emoji，语气轻松，控制在250字内）：
                📊 本周饮食总评：（1-2句整体评价）
                ✅ 做得好的地方：（1-2条）
                ⚠️ 需要注意：（1-2条不足）
                💡 下周建议：（2条具体可行的建议）
                """.formatted(weekStart, weekStart.plusDays(6),
                nutritionStats, recentMeals, prefs, budget);

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 持久化
        WeeklyReport report = new WeeklyReport();
        report.setUserId(userId);
        report.setWeekStart(weekStart);
        report.setContent(content);
        weeklyReportMapper.insert(report);

        return content;
    }
}
