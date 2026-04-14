package com.fanda.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.entity.*;
import com.fanda.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 饭搭 AI 工具集 — Spring AI Function Calling
 * AI Agent 会在需要时自动调用这些方法查询用户真实数据
 */
@Component
@RequiredArgsConstructor
public class FandaAiTools {

    private final MealRecordMapper mealRecordMapper;
    private final RecordNutritionMapper recordNutritionMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserAllergyMapper userAllergyMapper;
    private final UserDislikeMapper userDislikeMapper;
    private final UserFavCategoryMapper userFavCategoryMapper;
    private final FoodItemMapper foodItemMapper;
    private final BudgetMapper budgetMapper;
    private final ExpenseMapper expenseMapper;

    @Tool(description = "查询用户最近N天的用餐记录，包括菜名、餐次、日期、花费")
    public String getRecentMealRecords(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "查询最近多少天，默认7") int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
        List<MealRecord> records = mealRecordMapper.selectList(
                new LambdaQueryWrapper<MealRecord>()
                        .eq(MealRecord::getUserId, userId)
                        .ge(MealRecord::getRecordDate, cutoff)
                        .orderByDesc(MealRecord::getRecordDate));
        if (records.isEmpty()) return "最近" + days + "天没有用餐记录";
        return records.stream()
                .map(r -> String.format("%s %s 吃了【%s】花费%.1f元",
                        r.getRecordDate(), mealTypeLabel(r.getMealType()),
                        r.getFoodName(), r.getCost() != null ? r.getCost().doubleValue() : 0))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "查询用户近30天各营养类型摄入次数，用于营养分析")
    public String getNutritionStats(@ToolParam(description = "用户ID") Long userId) {
        LocalDate cutoff = LocalDate.now().minusDays(30);
        List<MealRecord> records = mealRecordMapper.selectList(
                new LambdaQueryWrapper<MealRecord>()
                        .eq(MealRecord::getUserId, userId)
                        .ge(MealRecord::getRecordDate, cutoff));
        if (records.isEmpty()) return "近30天没有营养数据";

        java.util.Map<String, Long> nutritionCount = new java.util.HashMap<>();
        for (MealRecord record : records) {
            List<RecordNutrition> nutritions = recordNutritionMapper.selectList(
                    new LambdaQueryWrapper<RecordNutrition>()
                            .eq(RecordNutrition::getRecordId, record.getId()));
            for (RecordNutrition n : nutritions) {
                nutritionCount.merge(nutritionLabel(n.getNutritionType()), 1L, Long::sum);
            }
        }
        if (nutritionCount.isEmpty()) return "近30天营养标签数据为空";
        return "近30天营养摄入统计（次数）：" + nutritionCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> e.getKey() + ":" + e.getValue() + "次")
                .collect(Collectors.joining("，"));
    }

    @Tool(description = "查询用户的口味偏好、忌口食材和不喜欢的口味标签")
    public String getUserPreferences(@ToolParam(description = "用户ID") Long userId) {
        StringBuilder sb = new StringBuilder();

        UserPreference pref = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));
        if (pref != null) {
            sb.append("辣度偏好：").append(spicyLabel(pref.getSpicyLevel())).append("；");
        }

        List<UserAllergy> allergies = userAllergyMapper.selectList(
                new LambdaQueryWrapper<UserAllergy>().eq(UserAllergy::getUserId, userId));
        if (!allergies.isEmpty()) {
            sb.append("忌口/过敏：")
              .append(allergies.stream().map(UserAllergy::getAllergy).collect(Collectors.joining("、")))
              .append("；");
        }

        List<UserDislike> dislikes = userDislikeMapper.selectList(
                new LambdaQueryWrapper<UserDislike>().eq(UserDislike::getUserId, userId));
        if (!dislikes.isEmpty()) {
            sb.append("不喜欢的口味：")
              .append(dislikes.stream().map(UserDislike::getDislike).collect(Collectors.joining("、")))
              .append("；");
        }

        List<UserFavCategory> favCats = userFavCategoryMapper.selectList(
                new LambdaQueryWrapper<UserFavCategory>().eq(UserFavCategory::getUserId, userId));
        if (!favCats.isEmpty()) {
            sb.append("偏好菜系：")
              .append(favCats.stream().map(UserFavCategory::getCategory).collect(Collectors.joining("、")));
        }

        return sb.length() > 0 ? sb.toString() : "暂无偏好设置";
    }

    @Tool(description = "查询用户当月预算使用情况，包括月预算、已花费、剩余")
    public String getBudgetStatus(@ToolParam(description = "用户ID") Long userId) {
        Budget budget = budgetMapper.selectOne(
                new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId));
        if (budget == null) return "尚未设置月预算";

        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        List<Expense> expenses = expenseMapper.selectList(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getUserId, userId)
                        .between(Expense::getExpenseDate, start, end));
        double spent = expenses.stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0)
                .sum();
        double remaining = budget.getMonthly().doubleValue() - spent;
        return String.format("月预算：%.0f元，本月已花：%.1f元，剩余：%.1f元，%s",
                budget.getMonthly().doubleValue(), spent, remaining,
                remaining < 0 ? "已超支！" : "预算充足");
    }

    @Tool(description = "从菜品库搜索符合关键词的菜品，返回菜名、分类、价格区间")
    public String searchFoods(@ToolParam(description = "搜索关键词，如菜系、口味、菜名") String keyword) {
        List<FoodItem> foods = foodItemMapper.selectList(
                new LambdaQueryWrapper<FoodItem>()
                        .like(FoodItem::getName, keyword)
                        .or()
                        .like(FoodItem::getCategory, keyword)
                        .last("LIMIT 10"));
        if (foods.isEmpty()) return "未找到相关菜品：" + keyword;
        return foods.stream()
                .map(f -> String.format("【%s】%s，价格%d-%d元",
                        f.getName(), f.getCategory(),
                        f.getPriceMin() != null ? f.getPriceMin() : 0,
                        f.getPriceMax() != null ? f.getPriceMax() : 0))
                .collect(Collectors.joining("\n"));
    }

    // ── 辅助方法 ──────────────────────────────────────────
    private String mealTypeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "breakfast" -> "早餐";
            case "lunch" -> "午餐";
            case "dinner" -> "晚餐";
            case "snack" -> "加餐";
            default -> type;
        };
    }

    private String nutritionLabel(String key) {
        if (key == null) return "";
        return switch (key) {
            case "protein" -> "蛋白质";
            case "carbs" -> "碳水";
            case "fat" -> "脂肪";
            case "fiber" -> "膳食纤维";
            case "vitamin" -> "维生素";
            case "calcium" -> "钙质";
            default -> key;
        };
    }

    private String spicyLabel(Integer level) {
        if (level == null) return "未设置";
        return switch (level) {
            case 0 -> "不辣";
            case 1 -> "微辣";
            case 2 -> "中辣";
            case 3 -> "重辣";
            default -> "未知";
        };
    }
}
