// 饭搭内置菜品数据库 (100+ 菜品)
export const foods = [
  // ===== 中式 - 米饭 =====
  { id: 'c001', name: '黄焖鸡米饭', category: 'chinese', subCategory: '米饭', priceRange: [15, 25], tags: ['微辣', '肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c002', name: '红烧肉套饭', category: 'chinese', subCategory: '米饭', priceRange: [18, 28], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c003', name: '鱼香肉丝饭', category: 'chinese', subCategory: '米饭', priceRange: [15, 22], tags: ['微辣', '肉类'], nutrition: ['carb', 'protein', 'veggie'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c004', name: '宫保鸡丁饭', category: 'chinese', subCategory: '米饭', priceRange: [16, 24], tags: ['辣', '肉类'], nutrition: ['carb', 'protein'], allergens: ['花生'], mealTime: ['lunch', 'dinner'] },
  { id: 'c005', name: '番茄炒蛋饭', category: 'chinese', subCategory: '米饭', priceRange: [12, 18], tags: ['清淡'], nutrition: ['carb', 'protein', 'veggie'], allergens: ['鸡蛋'], mealTime: ['lunch', 'dinner'] },
  { id: 'c006', name: '回锅肉饭', category: 'chinese', subCategory: '米饭', priceRange: [16, 25], tags: ['辣', '肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c007', name: '酸菜鱼饭', category: 'chinese', subCategory: '米饭', priceRange: [22, 35], tags: ['酸辣', '海鲜'], nutrition: ['carb', 'protein'], allergens: ['鱼'], mealTime: ['lunch', 'dinner'] },
  { id: 'c008', name: '卤肉饭', category: 'chinese', subCategory: '米饭', priceRange: [14, 20], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c009', name: '咖喱鸡饭', category: 'chinese', subCategory: '米饭', priceRange: [18, 26], tags: ['微辣', '肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c010', name: '麻婆豆腐饭', category: 'chinese', subCategory: '米饭', priceRange: [12, 18], tags: ['辣', '素食'], nutrition: ['carb', 'protein'], allergens: ['豆制品'], mealTime: ['lunch', 'dinner'] },
  { id: 'c011', name: '蛋炒饭', category: 'chinese', subCategory: '米饭', priceRange: [10, 16], tags: ['清淡'], nutrition: ['carb', 'protein'], allergens: ['鸡蛋'], mealTime: ['lunch', 'dinner'] },
  { id: 'c012', name: '扬州炒饭', category: 'chinese', subCategory: '米饭', priceRange: [14, 22], tags: ['清淡', '肉类'], nutrition: ['carb', 'protein', 'veggie'], allergens: ['鸡蛋'], mealTime: ['lunch', 'dinner'] },

  // ===== 中式 - 炒菜 =====
  { id: 'c013', name: '青椒肉丝', category: 'chinese', subCategory: '炒菜', priceRange: [14, 20], tags: ['微辣', '肉类'], nutrition: ['protein', 'veggie'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c014', name: '地三鲜', category: 'chinese', subCategory: '炒菜', priceRange: [12, 18], tags: ['清淡', '素食'], nutrition: ['veggie'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c015', name: '干锅花菜', category: 'chinese', subCategory: '炒菜', priceRange: [16, 24], tags: ['辣'], nutrition: ['veggie'], allergens: [], mealTime: ['lunch', 'dinner'] },

  // ===== 面食 =====
  { id: 'n001', name: '牛肉拉面', category: 'noodle', subCategory: '面条', priceRange: [15, 25], tags: ['清淡', '肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch', 'dinner'] },
  { id: 'n002', name: '炸酱面', category: 'noodle', subCategory: '面条', priceRange: [12, 20], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质', '豆制品'], mealTime: ['lunch', 'dinner'] },
  { id: 'n003', name: '重庆小面', category: 'noodle', subCategory: '面条', priceRange: [10, 16], tags: ['辣'], nutrition: ['carb'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch'] },
  { id: 'n004', name: '刀削面', category: 'noodle', subCategory: '面条', priceRange: [14, 22], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'n005', name: '热干面', category: 'noodle', subCategory: '面条', priceRange: [8, 14], tags: ['微辣'], nutrition: ['carb'], allergens: ['麸质', '花生'], mealTime: ['breakfast'] },
  { id: 'n006', name: '担担面', category: 'noodle', subCategory: '面条', priceRange: [12, 18], tags: ['辣', '肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质', '花生'], mealTime: ['lunch', 'dinner'] },
  { id: 'n007', name: '阳春面', category: 'noodle', subCategory: '面条', priceRange: [8, 12], tags: ['清淡'], nutrition: ['carb'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch'] },
  { id: 'n008', name: '油泼面', category: 'noodle', subCategory: '面条', priceRange: [12, 18], tags: ['辣'], nutrition: ['carb'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'n009', name: '肉夹馍+凉皮', category: 'noodle', subCategory: '面食', priceRange: [15, 22], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch'] },

  // ===== 西式 =====
  { id: 'w001', name: '芝士汉堡', category: 'western', subCategory: '汉堡', priceRange: [18, 35], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质', '乳制品'], mealTime: ['lunch', 'dinner'] },
  { id: 'w002', name: '意大利面', category: 'western', subCategory: '意面', priceRange: [22, 38], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质', '乳制品'], mealTime: ['lunch', 'dinner'] },
  { id: 'w003', name: '披萨', category: 'western', subCategory: '披萨', priceRange: [30, 60], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质', '乳制品'], mealTime: ['lunch', 'dinner'] },
  { id: 'w004', name: '牛排', category: 'western', subCategory: '牛排', priceRange: [58, 128], tags: ['肉类'], nutrition: ['protein'], allergens: [], mealTime: ['dinner'] },
  { id: 'w005', name: '三明治', category: 'western', subCategory: '三明治', priceRange: [15, 28], tags: ['清淡'], nutrition: ['carb', 'protein', 'veggie'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch'] },
  { id: 'w006', name: '凯撒沙拉', category: 'western', subCategory: '沙拉', priceRange: [25, 40], tags: ['清淡'], nutrition: ['veggie', 'protein'], allergens: ['乳制品'], mealTime: ['lunch', 'dinner'] },
  { id: 'w007', name: '炸鱼薯条', category: 'western', subCategory: '炸物', priceRange: [25, 40], tags: ['海鲜'], nutrition: ['carb', 'protein'], allergens: ['麸质', '鱼'], mealTime: ['lunch', 'dinner'] },

  // ===== 日韩 =====
  { id: 'j001', name: '三文鱼寿司', category: 'japanese', subCategory: '寿司', priceRange: [28, 50], tags: ['海鲜', '清淡'], nutrition: ['carb', 'protein'], allergens: ['鱼'], mealTime: ['lunch', 'dinner'] },
  { id: 'j002', name: '日式拉面', category: 'japanese', subCategory: '拉面', priceRange: [28, 45], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'j003', name: '牛肉饭(吉野家)', category: 'japanese', subCategory: '盖饭', priceRange: [22, 35], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'j004', name: '鳗鱼饭', category: 'japanese', subCategory: '盖饭', priceRange: [35, 58], tags: ['海鲜'], nutrition: ['carb', 'protein'], allergens: ['鱼'], mealTime: ['lunch', 'dinner'] },
  { id: 'j005', name: '韩式拌饭', category: 'japanese', subCategory: '拌饭', priceRange: [22, 35], tags: ['微辣'], nutrition: ['carb', 'protein', 'veggie'], allergens: ['鸡蛋'], mealTime: ['lunch', 'dinner'] },
  { id: 'j006', name: '韩式炸鸡', category: 'japanese', subCategory: '炸鸡', priceRange: [25, 45], tags: ['肉类'], nutrition: ['protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'j007', name: '石锅拌饭', category: 'japanese', subCategory: '拌饭', priceRange: [25, 38], tags: ['微辣', '肉类'], nutrition: ['carb', 'protein', 'veggie'], allergens: ['鸡蛋'], mealTime: ['lunch', 'dinner'] },
  { id: 'j008', name: '寿喜锅', category: 'japanese', subCategory: '火锅', priceRange: [45, 80], tags: ['肉类'], nutrition: ['protein', 'veggie'], allergens: [], mealTime: ['dinner'] },

  // ===== 快餐 =====
  { id: 'f001', name: '炸鸡套餐', category: 'fastfood', subCategory: '炸鸡', priceRange: [20, 35], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'f002', name: '鸡肉卷', category: 'fastfood', subCategory: '卷饼', priceRange: [15, 25], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'f003', name: '炸鸡汉堡', category: 'fastfood', subCategory: '汉堡', priceRange: [15, 28], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'f004', name: '薯条小食', category: 'fastfood', subCategory: '小食', priceRange: [8, 15], tags: ['素食'], nutrition: ['carb'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'f005', name: '盖浇饭', category: 'fastfood', subCategory: '盖饭', priceRange: [12, 20], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'f006', name: '鸡排饭', category: 'fastfood', subCategory: '盖饭', priceRange: [15, 22], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },

  // ===== 轻食 =====
  { id: 'l001', name: '鸡胸肉沙拉', category: 'light', subCategory: '沙拉', priceRange: [22, 38], tags: ['清淡', '肉类'], nutrition: ['protein', 'veggie'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'l002', name: '牛油果吐司', category: 'light', subCategory: '吐司', priceRange: [18, 30], tags: ['清淡'], nutrition: ['carb', 'veggie'], allergens: ['麸质'], mealTime: ['breakfast'] },
  { id: 'l003', name: '酸奶水果碗', category: 'light', subCategory: '甜品', priceRange: [15, 28], tags: ['清淡'], nutrition: ['fruit', 'protein'], allergens: ['乳制品'], mealTime: ['breakfast'] },
  { id: 'l004', name: '全麦三明治', category: 'light', subCategory: '三明治', priceRange: [16, 26], tags: ['清淡'], nutrition: ['carb', 'protein', 'veggie'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch'] },
  { id: 'l005', name: '荞麦面沙拉', category: 'light', subCategory: '沙拉', priceRange: [20, 32], tags: ['清淡'], nutrition: ['carb', 'veggie'], allergens: ['麸质'], mealTime: ['lunch'] },
  { id: 'l006', name: '杂粮粥', category: 'light', subCategory: '粥', priceRange: [8, 14], tags: ['清淡'], nutrition: ['carb'], allergens: [], mealTime: ['breakfast'] },

  // ===== 小吃 =====
  { id: 's001', name: '煎饼果子', category: 'snack', subCategory: '煎饼', priceRange: [8, 15], tags: ['清淡'], nutrition: ['carb', 'protein'], allergens: ['鸡蛋', '麸质'], mealTime: ['breakfast'] },
  { id: 's002', name: '肉夹馍', category: 'snack', subCategory: '饼夹', priceRange: [10, 18], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch'] },
  { id: 's003', name: '凉皮', category: 'snack', subCategory: '凉拌', priceRange: [8, 14], tags: ['微辣'], nutrition: ['carb'], allergens: ['麸质'], mealTime: ['lunch'] },
  { id: 's004', name: '麻辣烫', category: 'snack', subCategory: '烫煮', priceRange: [15, 30], tags: ['辣'], nutrition: ['carb', 'protein', 'veggie'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 's005', name: '烤冷面', category: 'snack', subCategory: '烤制', priceRange: [8, 14], tags: ['微辣'], nutrition: ['carb'], allergens: ['麸质', '鸡蛋'], mealTime: ['lunch', 'dinner'] },
  { id: 's006', name: '生煎包', category: 'snack', subCategory: '包点', priceRange: [10, 18], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch'] },
  { id: 's007', name: '酸辣粉', category: 'snack', subCategory: '粉', priceRange: [10, 18], tags: ['酸辣'], nutrition: ['carb'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 's008', name: '鸡蛋灌饼', category: 'snack', subCategory: '饼', priceRange: [6, 12], tags: ['清淡'], nutrition: ['carb', 'protein'], allergens: ['鸡蛋', '麸质'], mealTime: ['breakfast'] },
  { id: 's009', name: '臭豆腐', category: 'snack', subCategory: '炸制', priceRange: [8, 15], tags: ['辣'], nutrition: ['protein'], allergens: ['豆制品'], mealTime: ['lunch', 'dinner'] },
  { id: 's010', name: '串串香', category: 'snack', subCategory: '串串', priceRange: [20, 40], tags: ['辣', '肉类'], nutrition: ['protein', 'veggie'], allergens: [], mealTime: ['dinner'] },

  // ===== 火锅 =====
  { id: 'h001', name: '川味火锅', category: 'hotpot', subCategory: '火锅', priceRange: [50, 100], tags: ['辣', '肉类'], nutrition: ['protein', 'veggie'], allergens: [], mealTime: ['dinner'] },
  { id: 'h002', name: '潮汕牛肉锅', category: 'hotpot', subCategory: '火锅', priceRange: [60, 120], tags: ['清淡', '肉类'], nutrition: ['protein'], allergens: [], mealTime: ['dinner'] },
  { id: 'h003', name: '番茄锅', category: 'hotpot', subCategory: '火锅', priceRange: [45, 90], tags: ['清淡'], nutrition: ['protein', 'veggie'], allergens: [], mealTime: ['dinner'] },
  { id: 'h004', name: '椰子鸡', category: 'hotpot', subCategory: '火锅', priceRange: [55, 100], tags: ['清淡', '肉类'], nutrition: ['protein'], allergens: [], mealTime: ['dinner'] },
  { id: 'h005', name: '鱼火锅', category: 'hotpot', subCategory: '火锅', priceRange: [50, 90], tags: ['辣', '海鲜'], nutrition: ['protein'], allergens: ['鱼'], mealTime: ['dinner'] },
  { id: 'h006', name: '羊蝎子火锅', category: 'hotpot', subCategory: '火锅', priceRange: [60, 110], tags: ['肉类'], nutrition: ['protein'], allergens: [], mealTime: ['dinner'] },

  // ===== 粥/早餐 =====
  { id: 'b001', name: '皮蛋瘦肉粥', category: 'chinese', subCategory: '粥', priceRange: [8, 15], tags: ['清淡', '肉类'], nutrition: ['carb', 'protein'], allergens: ['鸡蛋'], mealTime: ['breakfast'] },
  { id: 'b002', name: '豆浆油条', category: 'chinese', subCategory: '早点', priceRange: [6, 12], tags: ['清淡'], nutrition: ['carb', 'protein'], allergens: ['豆制品', '麸质'], mealTime: ['breakfast'] },
  { id: 'b003', name: '小笼包', category: 'chinese', subCategory: '包点', priceRange: [10, 20], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch'] },
  { id: 'b004', name: '肠粉', category: 'chinese', subCategory: '广式', priceRange: [10, 18], tags: ['清淡'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['breakfast'] },
  { id: 'b005', name: '包子+粥', category: 'chinese', subCategory: '早点', priceRange: [6, 12], tags: ['清淡', '肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['breakfast'] },

  // ===== 烧烤 =====
  { id: 'k001', name: '烤肉饭', category: 'chinese', subCategory: '烧烤', priceRange: [18, 30], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'k002', name: '烧烤拼盘', category: 'chinese', subCategory: '烧烤', priceRange: [40, 80], tags: ['辣', '肉类'], nutrition: ['protein'], allergens: [], mealTime: ['dinner'] },

  // ===== 更多中式 =====
  { id: 'c016', name: '水煮鱼', category: 'chinese', subCategory: '川菜', priceRange: [35, 55], tags: ['辣', '海鲜'], nutrition: ['protein'], allergens: ['鱼'], mealTime: ['lunch', 'dinner'] },
  { id: 'c017', name: '糖醋排骨', category: 'chinese', subCategory: '炒菜', priceRange: [25, 40], tags: ['酸甜', '肉类'], nutrition: ['protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c018', name: '可乐鸡翅', category: 'chinese', subCategory: '炒菜', priceRange: [20, 32], tags: ['甜', '肉类'], nutrition: ['protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c019', name: '东坡肉', category: 'chinese', subCategory: '炒菜', priceRange: [28, 45], tags: ['肉类'], nutrition: ['protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c020', name: '白切鸡', category: 'chinese', subCategory: '粤菜', priceRange: [25, 40], tags: ['清淡', '肉类'], nutrition: ['protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c021', name: '叉烧饭', category: 'chinese', subCategory: '粤菜', priceRange: [18, 28], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c022', name: '烧鹅饭', category: 'chinese', subCategory: '粤菜', priceRange: [25, 40], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c023', name: '煲仔饭', category: 'chinese', subCategory: '粤菜', priceRange: [22, 35], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
  { id: 'c024', name: '兰州拌面', category: 'chinese', subCategory: '面食', priceRange: [14, 22], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'c025', name: '饺子', category: 'chinese', subCategory: '面食', priceRange: [12, 22], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['lunch', 'dinner'] },
  { id: 'c026', name: '馄饨', category: 'chinese', subCategory: '面食', priceRange: [10, 18], tags: ['清淡', '肉类'], nutrition: ['carb', 'protein'], allergens: ['麸质'], mealTime: ['breakfast', 'lunch'] },
  { id: 'c027', name: '炒河粉', category: 'chinese', subCategory: '粤菜', priceRange: [14, 22], tags: ['肉类'], nutrition: ['carb', 'protein'], allergens: [], mealTime: ['lunch', 'dinner'] },
]
