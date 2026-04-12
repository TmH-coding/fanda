package com.fanda.init;

import com.fanda.entity.*;
import com.fanda.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final FoodItemMapper foodItemMapper;
    private final FoodTagMapper foodTagMapper;
    private final FoodNutritionMapper foodNutritionMapper;
    private final FoodAllergenMapper foodAllergenMapper;
    private final FoodMealTimeMapper foodMealTimeMapper;

    @Override
    public void run(String... args) {
        if (foodItemMapper.selectCount(null) > 0) {
            log.info("菜品数据已存在，跳过初始化");
            return;
        }
        log.info("开始初始化菜品数据...");
        List<FoodItem> foods = buildFoods();
        for (FoodItem f : foods) {
            foodItemMapper.insert(f);
            Long id = f.getId();
            for (FoodTag t : f.getTags()) { t.setFoodId(id); foodTagMapper.insert(t); }
            for (FoodNutrition n : f.getNutritions()) { n.setFoodId(id); foodNutritionMapper.insert(n); }
            for (FoodAllergen a : f.getAllergens()) { a.setFoodId(id); foodAllergenMapper.insert(a); }
            for (FoodMealTime m : f.getMealTimes()) { m.setFoodId(id); foodMealTimeMapper.insert(m); }
        }
        log.info("菜品数据初始化完成，共 {} 条", foods.size());
    }

    private List<FoodItem> buildFoods() {
        return List.of(
            food("c001","黄焖鸡米饭","chinese","米饭",15,25,t("微辣","肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("c002","红烧肉套饭","chinese","米饭",18,28,t("肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("c003","鱼香肉丝饭","chinese","米饭",15,22,t("微辣","肉类"),n("carb","protein","veggie"),a(),m("lunch","dinner")),
            food("c004","宫保鸡丁饭","chinese","米饭",16,24,t("辣","肉类"),n("carb","protein"),a("花生"),m("lunch","dinner")),
            food("c005","番茄炒蛋饭","chinese","米饭",12,18,t("清淡"),n("carb","protein","veggie"),a("鸡蛋"),m("lunch","dinner")),
            food("c006","回锅肉饭","chinese","米饭",16,25,t("辣","肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("c007","酸菜鱼饭","chinese","米饭",22,35,t("酸辣","海鲜"),n("carb","protein"),a("鱼"),m("lunch","dinner")),
            food("c008","卤肉饭","chinese","米饭",14,20,t("肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("c009","咖喱鸡饭","chinese","米饭",18,26,t("微辣","肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("c010","麻婆豆腐饭","chinese","米饭",12,18,t("辣","素食"),n("carb","protein"),a("豆制品"),m("lunch","dinner")),
            food("c011","蛋炒饭","chinese","米饭",10,16,t("清淡"),n("carb","protein"),a("鸡蛋"),m("lunch","dinner")),
            food("c012","扬州炒饭","chinese","米饭",14,22,t("清淡","肉类"),n("carb","protein","veggie"),a("鸡蛋"),m("lunch","dinner")),
            food("c013","青椒肉丝","chinese","炒菜",14,20,t("微辣","肉类"),n("protein","veggie"),a(),m("lunch","dinner")),
            food("c014","地三鲜","chinese","炒菜",12,18,t("清淡","素食"),n("veggie"),a(),m("lunch","dinner")),
            food("c015","干锅花菜","chinese","炒菜",16,24,t("辣"),n("veggie"),a(),m("lunch","dinner")),
            food("n001","牛肉拉面","noodle","面条",15,25,t("清淡","肉类"),n("carb","protein"),a("麸质"),m("breakfast","lunch","dinner")),
            food("n002","炸酱面","noodle","面条",12,20,t("肉类"),n("carb","protein"),a("麸质","豆制品"),m("lunch","dinner")),
            food("n003","重庆小面","noodle","面条",10,16,t("辣"),n("carb"),a("麸质"),m("breakfast","lunch")),
            food("n004","刀削面","noodle","面条",14,22,t("肉类"),n("carb","protein"),a("麸质"),m("lunch","dinner")),
            food("n005","热干面","noodle","面条",8,14,t("微辣"),n("carb"),a("麸质","花生"),m("breakfast")),
            food("n006","担担面","noodle","面条",12,18,t("辣","肉类"),n("carb","protein"),a("麸质","花生"),m("lunch","dinner")),
            food("n007","阳春面","noodle","面条",8,12,t("清淡"),n("carb"),a("麸质"),m("breakfast","lunch")),
            food("n008","油泼面","noodle","面条",12,18,t("辣"),n("carb"),a("麸质"),m("lunch","dinner")),
            food("n009","肉夹馍+凉皮","noodle","面食",15,22,t("肉类"),n("carb","protein"),a("麸质"),m("lunch")),
            food("w001","芝士汉堡","western","汉堡",18,35,t("肉类"),n("carb","protein"),a("麸质","乳制品"),m("lunch","dinner")),
            food("w002","意大利面","western","意面",22,38,t("肉类"),n("carb","protein"),a("麸质","乳制品"),m("lunch","dinner")),
            food("w003","披萨","western","披萨",30,60,t("肉类"),n("carb","protein"),a("麸质","乳制品"),m("lunch","dinner")),
            food("w004","牛排","western","牛排",58,128,t("肉类"),n("protein"),a(),m("dinner")),
            food("w005","三明治","western","三明治",15,28,t("清淡"),n("carb","protein","veggie"),a("麸质"),m("breakfast","lunch")),
            food("w006","凯撒沙拉","western","沙拉",25,40,t("清淡"),n("veggie","protein"),a("乳制品"),m("lunch","dinner")),
            food("w007","炸鱼薯条","western","炸物",25,40,t("海鲜"),n("carb","protein"),a("麸质","鱼"),m("lunch","dinner")),
            food("j001","三文鱼寿司","japanese","寿司",28,50,t("海鲜","清淡"),n("carb","protein"),a("鱼"),m("lunch","dinner")),
            food("j002","日式拉面","japanese","拉面",28,45,t("肉类"),n("carb","protein"),a("麸质"),m("lunch","dinner")),
            food("j003","牛肉饭(吉野家)","japanese","盖饭",22,35,t("肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("j004","鳗鱼饭","japanese","盖饭",35,58,t("海鲜"),n("carb","protein"),a("鱼"),m("lunch","dinner")),
            food("j005","韩式拌饭","japanese","拌饭",22,35,t("微辣"),n("carb","protein","veggie"),a("鸡蛋"),m("lunch","dinner")),
            food("j006","韩式炸鸡","japanese","炸鸡",25,45,t("肉类"),n("protein"),a("麸质"),m("lunch","dinner")),
            food("j007","石锅拌饭","japanese","拌饭",25,38,t("微辣","肉类"),n("carb","protein","veggie"),a("鸡蛋"),m("lunch","dinner")),
            food("j008","寿喜锅","japanese","火锅",45,80,t("肉类"),n("protein","veggie"),a(),m("dinner")),
            food("f001","炸鸡套餐","fastfood","炸鸡",20,35,t("肉类"),n("carb","protein"),a("麸质"),m("lunch","dinner")),
            food("f002","鸡肉卷","fastfood","卷饼",15,25,t("肉类"),n("carb","protein"),a("麸质"),m("lunch","dinner")),
            food("f003","炸鸡汉堡","fastfood","汉堡",15,28,t("肉类"),n("carb","protein"),a("麸质"),m("lunch","dinner")),
            food("f004","薯条小食","fastfood","小食",8,15,t("素食"),n("carb"),a(),m("lunch","dinner")),
            food("f005","盖浇饭","fastfood","盖饭",12,20,t("肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("f006","鸡排饭","fastfood","盖饭",15,22,t("肉类"),n("carb","protein"),a("麸质"),m("lunch","dinner")),
            food("l001","鸡胸肉沙拉","light","沙拉",22,38,t("清淡","肉类"),n("protein","veggie"),a(),m("lunch","dinner")),
            food("l002","牛油果吐司","light","吐司",18,30,t("清淡"),n("carb","veggie"),a("麸质"),m("breakfast")),
            food("l003","酸奶水果碗","light","甜品",15,28,t("清淡"),n("fruit","protein"),a("乳制品"),m("breakfast")),
            food("l004","全麦三明治","light","三明治",16,26,t("清淡"),n("carb","protein","veggie"),a("麸质"),m("breakfast","lunch")),
            food("l005","荞麦面沙拉","light","沙拉",20,32,t("清淡"),n("carb","veggie"),a("麸质"),m("lunch")),
            food("l006","杂粮粥","light","粥",8,14,t("清淡"),n("carb"),a(),m("breakfast")),
            food("s001","煎饼果子","snack","煎饼",8,15,t("清淡"),n("carb","protein"),a("鸡蛋","麸质"),m("breakfast")),
            food("s002","肉夹馍","snack","饼夹",10,18,t("肉类"),n("carb","protein"),a("麸质"),m("breakfast","lunch")),
            food("s003","凉皮","snack","凉拌",8,14,t("微辣"),n("carb"),a("麸质"),m("lunch")),
            food("s004","麻辣烫","snack","烫煮",15,30,t("辣"),n("carb","protein","veggie"),a(),m("lunch","dinner")),
            food("s005","烤冷面","snack","烤制",8,14,t("微辣"),n("carb"),a("麸质","鸡蛋"),m("lunch","dinner")),
            food("s006","生煎包","snack","包点",10,18,t("肉类"),n("carb","protein"),a("麸质"),m("breakfast","lunch")),
            food("s007","酸辣粉","snack","粉",10,18,t("酸辣"),n("carb"),a(),m("lunch","dinner")),
            food("s008","鸡蛋灌饼","snack","饼",6,12,t("清淡"),n("carb","protein"),a("鸡蛋","麸质"),m("breakfast")),
            food("s009","臭豆腐","snack","炸制",8,15,t("辣"),n("protein"),a("豆制品"),m("lunch","dinner")),
            food("s010","串串香","snack","串串",20,40,t("辣","肉类"),n("protein","veggie"),a(),m("dinner")),
            food("h001","川味火锅","hotpot","火锅",50,100,t("辣","肉类"),n("protein","veggie"),a(),m("dinner")),
            food("h002","潮汕牛肉锅","hotpot","火锅",60,120,t("清淡","肉类"),n("protein"),a(),m("dinner")),
            food("h003","番茄锅","hotpot","火锅",45,90,t("清淡"),n("protein","veggie"),a(),m("dinner")),
            food("h004","椰子鸡","hotpot","火锅",55,100,t("清淡","肉类"),n("protein"),a(),m("dinner")),
            food("h005","鱼火锅","hotpot","火锅",50,90,t("辣","海鲜"),n("protein"),a("鱼"),m("dinner")),
            food("h006","羊蝎子火锅","hotpot","火锅",60,110,t("肉类"),n("protein"),a(),m("dinner")),
            food("b001","皮蛋瘦肉粥","chinese","粥",8,15,t("清淡","肉类"),n("carb","protein"),a("鸡蛋"),m("breakfast")),
            food("b002","豆浆油条","chinese","早点",6,12,t("清淡"),n("carb","protein"),a("豆制品","麸质"),m("breakfast")),
            food("b003","小笼包","chinese","包点",10,20,t("肉类"),n("carb","protein"),a("麸质"),m("breakfast","lunch")),
            food("b004","肠粉","chinese","广式",10,18,t("清淡"),n("carb","protein"),a(),m("breakfast")),
            food("b005","包子+粥","chinese","早点",6,12,t("清淡","肉类"),n("carb","protein"),a("麸质"),m("breakfast")),
            food("k001","烤肉饭","chinese","烧烤",18,30,t("肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("k002","烧烤拼盘","chinese","烧烤",40,80,t("辣","肉类"),n("protein"),a(),m("dinner")),
            food("c016","水煮鱼","chinese","川菜",35,55,t("辣","海鲜"),n("protein"),a("鱼"),m("lunch","dinner")),
            food("c017","糖醋排骨","chinese","炒菜",25,40,t("酸甜","肉类"),n("protein"),a(),m("lunch","dinner")),
            food("c018","可乐鸡翅","chinese","炒菜",20,32,t("甜","肉类"),n("protein"),a(),m("lunch","dinner")),
            food("c019","东坡肉","chinese","炒菜",28,45,t("肉类"),n("protein"),a(),m("lunch","dinner")),
            food("c020","白切鸡","chinese","粤菜",25,40,t("清淡","肉类"),n("protein"),a(),m("lunch","dinner")),
            food("c021","叉烧饭","chinese","粤菜",18,28,t("肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("c022","烧鹅饭","chinese","粤菜",25,40,t("肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("c023","煲仔饭","chinese","粤菜",22,35,t("肉类"),n("carb","protein"),a(),m("lunch","dinner")),
            food("c024","兰州拌面","chinese","面食",14,22,t("肉类"),n("carb","protein"),a("麸质"),m("lunch","dinner")),
            food("c025","饺子","chinese","面食",12,22,t("肉类"),n("carb","protein"),a("麸质"),m("lunch","dinner")),
            food("c026","馄饨","chinese","面食",10,18,t("清淡","肉类"),n("carb","protein"),a("麸质"),m("breakfast","lunch")),
            food("c027","炒河粉","chinese","粤菜",14,22,t("肉类"),n("carb","protein"),a(),m("lunch","dinner"))
        );
    }

    private FoodItem food(String code, String name, String cat, String sub, int pMin, int pMax,
                          List<String> tags, List<String> nutri, List<String> allergens, List<String> meals) {
        FoodItem f = new FoodItem();
        f.setFoodCode(code);
        f.setName(name);
        f.setCategory(cat);
        f.setSubCategory(sub);
        f.setPriceMin(BigDecimal.valueOf(pMin));
        f.setPriceMax(BigDecimal.valueOf(pMax));
        f.setIsSystem(true);
        f.setTags(tags.stream().map(s -> { FoodTag ft = new FoodTag(); ft.setTag(s); return ft; }).toList());
        f.setNutritions(nutri.stream().map(s -> { FoodNutrition fn = new FoodNutrition(); fn.setNutritionType(s); return fn; }).toList());
        f.setAllergens(allergens.stream().map(s -> { FoodAllergen fa = new FoodAllergen(); fa.setAllergen(s); return fa; }).toList());
        f.setMealTimes(meals.stream().map(s -> { FoodMealTime fm = new FoodMealTime(); fm.setMealTime(s); return fm; }).toList());
        return f;
    }

    private List<String> t(String... s) { return List.of(s); }
    private List<String> n(String... s) { return List.of(s); }
    private List<String> a(String... s) { return List.of(s); }
    private List<String> m(String... s) { return List.of(s); }
}
