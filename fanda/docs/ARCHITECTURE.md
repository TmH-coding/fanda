# 饭搭 — 项目架构设计文档

---

## 一、架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐              │
│  │ 首页  │ │ 日历 │ │ 预算  │ │ 拼饭  │ │ 我的 │   Pages     │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘              │
│     │        │        │        │        │                    │
│  ┌──┴────────┴────────┴────────┴────────┴───┐               │
│  │         Components (fd-xxx)               │               │
│  └──────────────────┬───────────────────────┘               │
├─────────────────────┼───────────────────────────────────────┤
│                     │     Business Layer                      │
│  ┌──────────────────┴───────────────────────┐               │
│  │           Composables (useXxx)            │               │
│  │  useRecommend / useRecord / useBudget     │               │
│  └──────────────────┬───────────────────────┘               │
│  ┌──────────────────┴───────────────────────┐               │
│  │           Stores (Pinia)                  │               │
│  │  foodStore / recordStore / budgetStore     │               │
│  └──────────────────┬───────────────────────┘               │
├─────────────────────┼───────────────────────────────────────┤
│                     │     Data Layer                          │
│  ┌──────────────────┴───────────────────────┐               │
│  │           Service (api/xxx.js)            │  ← 抽象接口    │
│  └─────┬────────────────────────┬───────────┘               │
│  ┌─────┴─────┐          ┌──────┴──────┐                     │
│  │ LocalRepo │          │  RemoteRepo  │  ← 可切换实现       │
│  │ (storage) │          │  (HTTP/云)   │                     │
│  └───────────┘          └─────────────┘                     │
├─────────────────────────────────────────────────────────────┤
│                     Infrastructure                           │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌───────────┐ │
│  │ Router │ │ Theme  │ │ Event  │ │ Logger │ │ Platform  │ │
│  │ Guard  │ │ Engine │ │  Bus   │ │        │ │ Adapter   │ │
│  └────────┘ └────────┘ └────────┘ └────────┘ └───────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、目录结构

```
fanda/
├── src/
│   ├── pages/                          # 页面层
│   │   ├── index/
│   │   │   └── index.vue               # 首页-转盘推荐
│   │   ├── calendar/
│   │   │   └── calendar.vue            # 饮食日历
│   │   ├── budget/
│   │   │   └── budget.vue              # 预算管家
│   │   ├── social/
│   │   │   └── social.vue              # 拼饭广场
│   │   ├── social-detail/
│   │   │   └── social-detail.vue       # 拼饭详情
│   │   └── profile/
│   │       └── profile.vue             # 个人中心
│   │
│   ├── components/                     # 组件层（按领域分组）
│   │   ├── recommend/
│   │   │   ├── fd-wheel.vue            # 转盘
│   │   │   ├── fd-food-result.vue      # 推荐结果弹窗
│   │   │   └── fd-exclude-tags.vue     # 排除标签
│   │   ├── record/
│   │   │   ├── fd-calendar-grid.vue    # 日历网格
│   │   │   ├── fd-meal-card.vue        # 餐次卡片
│   │   │   └── fd-record-form.vue      # 记录表单
│   │   ├── budget/
│   │   │   ├── fd-budget-ring.vue      # 预算环形图
│   │   │   ├── fd-trend-chart.vue      # 趋势图
│   │   │   └── fd-expense-list.vue     # 消费明细
│   │   ├── social/
│   │   │   ├── fd-group-card.vue       # 拼饭卡片
│   │   │   ├── fd-vote-panel.vue       # 投票面板
│   │   │   └── fd-create-group.vue     # 发起拼饭
│   │   └── common/
│   │       ├── fd-tabbar.vue           # 底部导航
│   │       ├── fd-empty.vue            # 空状态
│   │       ├── fd-toast.vue            # 轻提示
│   │       ├── fd-modal.vue            # 弹窗
│   │       └── fd-badge.vue            # 成就徽章
│   │
│   ├── composables/                    # 组合式函数（业务逻辑复用）
│   │   ├── useRecommend.js             # 推荐算法逻辑
│   │   ├── useRecord.js                # 记录操作逻辑
│   │   ├── useBudget.js                # 预算计算逻辑
│   │   ├── useAchievement.js           # 成就判定逻辑
│   │   └── useCountdown.js             # 倒计时/动画控制
│   │
│   ├── stores/                         # 状态管理（Pinia）
│   │   ├── modules/
│   │   │   ├── food.js                 # 菜品数据
│   │   │   ├── record.js               # 用餐记录
│   │   │   ├── budget.js               # 预算管理
│   │   │   ├── preference.js           # 用户偏好
│   │   │   ├── social.js               # 拼饭社交
│   │   │   └── achievement.js          # 成就系统
│   │   └── index.js                    # Store 统一导出
│   │
│   ├── services/                       # 数据服务层（核心扩展点）
│   │   ├── interface.js                # 接口定义（约定方法签名）
│   │   ├── local/                      # 本地存储实现
│   │   │   ├── foodService.js
│   │   │   ├── recordService.js
│   │   │   ├── budgetService.js
│   │   │   └── socialService.js
│   │   ├── remote/                     # 远程API实现（后续扩展）
│   │   │   ├── foodService.js
│   │   │   ├── recordService.js
│   │   │   ├── budgetService.js
│   │   │   └── socialService.js
│   │   └── factory.js                  # 服务工厂（切换local/remote）
│   │
│   ├── data/                           # 静态数据
│   │   ├── foods.js                    # 菜品数据库
│   │   ├── achievements.js             # 成就定义
│   │   └── mockSocial.js               # 拼饭模拟数据
│   │
│   ├── utils/                          # 工具函数
│   │   ├── storage.js                  # 本地存储封装
│   │   ├── date.js                     # 日期工具
│   │   ├── format.js                   # 格式化工具
│   │   └── platform.js                 # 平台差异适配
│   │
│   ├── config/                         # 配置
│   │   ├── index.js                    # 主配置（环境变量、开关）
│   │   ├── theme.js                    # 主题配置
│   │   └── constants.js                # 常量定义
│   │
│   ├── styles/                         # 全局样式
│   │   ├── variables.scss              # SCSS变量（颜色、尺寸）
│   │   ├── mixins.scss                 # 常用mixin
│   │   ├── animation.scss              # 动画定义
│   │   └── common.scss                 # 公共样式
│   │
│   ├── static/                         # 静态资源
│   │   ├── icons/                      # 图标
│   │   ├── images/                     # 插画
│   │   └── tabbar/                     # TabBar图标
│   │
│   ├── plugins/                        # 插件（后续扩展点）
│   │   └── index.js                    # 插件注册入口
│   │
│   ├── App.vue
│   ├── main.js
│   ├── pages.json                      # 路由配置
│   ├── manifest.json                   # 应用配置
│   └── uni.scss                        # uni-app全局变量
│
├── docs/
│   ├── PRD.md
│   └── ARCHITECTURE.md
│
└── package.json
```

---

## 三、分层设计详解

### 3.1 Presentation Layer（展示层）

**职责：** 只负责UI渲染和用户交互，不包含业务逻辑。

```
Pages   →  组装页面，调用 composables 获取数据和方法
Components →  纯UI组件，通过 props/emit 通信，不直接访问 store
```

**规范：**
- 页面通过 `composables` 获取业务能力，不直接操作 `store` 或 `service`
- 组件保持纯净，只接收 props，通过 emit 向上通信
- 组件命名统一 `fd-` 前缀，避免与 uni-ui 冲突

**示例：页面如何使用**
```vue
<!-- pages/index/index.vue -->
<script setup>
import { useRecommend } from '@/composables/useRecommend'

const { 
  currentFood, 
  spin, 
  exclude, 
  confirm 
} = useRecommend()
</script>

<template>
  <fd-wheel :items="foods" @spin-end="onResult" />
  <fd-exclude-tags :tags="categories" @exclude="exclude" />
  <fd-food-result :food="currentFood" @confirm="confirm" />
</template>
```

### 3.2 Business Layer（业务层）

**职责：** 封装所有业务逻辑，向上提供可复用的能力。

#### Composables（组合式函数）

页面与业务逻辑的桥梁，封装一个完整的业务场景：

```javascript
// composables/useRecommend.js
import { useFoodStore } from '@/stores/modules/food'
import { usePreferenceStore } from '@/stores/modules/preference'
import { useRecordStore } from '@/stores/modules/record'

export function useRecommend() {
  const foodStore = useFoodStore()
  const prefStore = usePreferenceStore()
  const recordStore = useRecordStore()

  // 综合偏好、历史、预算的推荐算法
  function getRecommendation(excludeList = []) {
    const allFoods = foodStore.foods
    const prefs = prefStore.preferences
    const recent = recordStore.recentFoods(3) // 最近3天

    return allFoods
      .filter(f => !excludeList.includes(f.category))  // 排除
      .filter(f => !prefs.allergies.some(a => f.allergens.includes(a))) // 忌口
      .filter(f => !recent.includes(f.id))              // 去重
      .sort(() => Math.random() - 0.5)                  // 随机
  }

  return { getRecommendation, ... }
}
```

#### Stores（Pinia 状态管理）

管理全局共享状态，调用 service 层进行数据持久化：

```javascript
// stores/modules/record.js
import { defineStore } from 'pinia'
import { getService } from '@/services/factory'

export const useRecordStore = defineStore('record', {
  state: () => ({
    records: [],
    loaded: false,
  }),

  getters: {
    // 获取某天的记录
    getByDate: (state) => (date) => {
      return state.records.filter(r => r.date === date)
    },
    // 最近N天吃过的菜品ID
    recentFoods: (state) => (days) => {
      // ...
    },
  },

  actions: {
    async load() {
      const service = getService('record')
      this.records = await service.getAll()
      this.loaded = true
    },
    async add(record) {
      const service = getService('record')
      await service.save(record)
      this.records.push(record)
    },
  },
})
```

### 3.3 Data Layer（数据层） — 核心扩展点

**职责：** 隔离数据来源，让上层不关心数据存在本地还是云端。

#### 服务接口定义

```javascript
// services/interface.js
// 所有 service 实现必须遵循的方法签名

/**
 * RecordService 接口
 * @method getAll()          → Promise<MealRecord[]>
 * @method getByDate(date)   → Promise<MealRecord[]>
 * @method save(record)      → Promise<void>
 * @method delete(id)        → Promise<void>
 */

/**
 * FoodService 接口
 * @method getAll()           → Promise<FoodItem[]>
 * @method getByCategory(cat) → Promise<FoodItem[]>
 * @method search(keyword)    → Promise<FoodItem[]>
 */

/**
 * BudgetService 接口
 * @method getBudget()            → Promise<Budget>
 * @method setBudget(budget)      → Promise<void>
 * @method getExpenses(month)     → Promise<Expense[]>
 */
```

#### 本地实现

```javascript
// services/local/recordService.js
import { storage } from '@/utils/storage'

const RECORD_KEY = 'fd_records'

export default {
  async getAll() {
    return storage.get(RECORD_KEY, [])
  },
  async getByDate(date) {
    const all = await this.getAll()
    return all.filter(r => r.date === date)
  },
  async save(record) {
    const all = await this.getAll()
    all.push(record)
    storage.set(RECORD_KEY, all)
  },
  async delete(id) {
    const all = await this.getAll()
    storage.set(RECORD_KEY, all.filter(r => r.id !== id))
  },
}
```

#### 远程实现（后续扩展时填充）

```javascript
// services/remote/recordService.js
import { http } from '@/utils/http'

export default {
  async getAll() {
    return http.get('/api/records')
  },
  async save(record) {
    return http.post('/api/records', record)
  },
  // ...
}
```

#### 服务工厂（一键切换）

```javascript
// services/factory.js
import config from '@/config'

// 按配置动态加载 local 或 remote 实现
const serviceMap = {}

export function getService(name) {
  if (!serviceMap[name]) {
    const mode = config.dataMode // 'local' | 'remote'
    serviceMap[name] = require(`@/services/${mode}/${name}Service.js`).default
  }
  return serviceMap[name]
}
```

**扩展方式：** 未来接入后端时，只需：
1. 在 `services/remote/` 下实现同名 service
2. 将 `config.dataMode` 改为 `'remote'`
3. 上层代码零修改

---

## 四、数据流

```
用户操作
   │
   ▼
Page (调用 composable 方法)
   │
   ▼
Composable (编排业务逻辑)
   │
   ▼
Store (更新状态 + 调用 service 持久化)
   │
   ▼
Service (local/remote 透明切换)
   │
   ▼
Storage / API
```

**单向数据流示例 — 记录一餐：**

```
用户点击"就吃这个"
  → composable.confirm(food)
    → recordStore.add({ date, mealType, food, cost })
      → recordService.save(record)      // 持久化
      → budgetStore.addExpense(cost)     // 联动更新预算
      → achievementStore.check('record') // 联动检查成就
    → UI 自动响应 store 变化，更新日历和预算
```

---

## 五、模块依赖关系

```
                    ┌─────────────┐
                    │  preference  │  (被多个模块依赖)
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │   food   │    │  record  │───▶│  budget  │
    └────┬─────┘    └────┬─────┘    └──────────┘
         │               │
         ▼               ▼
    ┌──────────┐    ┌──────────┐
    │recommend │    │achievement│
    └──────────┘    └──────────┘

    ┌──────────┐
    │  social  │  (MVP独立，后续接入 record/preference)
    └──────────┘
```

**依赖规则：**
- `preference` 是基础模块，被 `food`、`record`、`recommend` 依赖
- `record` → `budget`：每次记录自动更新消费
- `record` → `achievement`：每次记录触发成就检查
- `social` MVP阶段独立运行，不依赖其他模块

---

## 六、扩展点设计

### 6.1 数据源切换（local → remote）

```
修改 config.dataMode = 'remote'
实现 services/remote/ 下的 service
→ 零改动完成迁移
```

### 6.2 新增功能模块

按照现有模式添加即可：

```
1. pages/xxx/xxx.vue          — 新页面
2. components/xxx/            — 新组件
3. composables/useXxx.js      — 新业务逻辑
4. stores/modules/xxx.js      — 新状态
5. services/local/xxxService.js — 新数据服务
6. pages.json 注册路由
```

### 6.3 平台适配

```javascript
// utils/platform.js
export function getPlatform() {
  // #ifdef H5
  return 'h5'
  // #endif
  // #ifdef MP-WEIXIN
  return 'weixin'
  // #endif
  // #ifdef APP-PLUS
  return 'app'
  // #endif
}

// 平台差异逻辑统一在此处理
export function share(data) {
  const platform = getPlatform()
  if (platform === 'weixin') {
    // 微信分享API
  } else if (platform === 'h5') {
    // H5分享方案
  }
}
```

### 6.4 主题系统

```scss
// styles/variables.scss
// 通过CSS变量实现主题切换，后续可扩展暗黑模式

:root {
  --fd-primary: #FF6B6B;
  --fd-secondary: #FFE66D;
  --fd-accent: #4ECDC4;
  --fd-bg: #FFF5F5;
  --fd-text: #2D3436;
  --fd-card-bg: #FFFFFF;
  --fd-radius: 24rpx;
  --fd-shadow: 0 4rpx 12rpx rgba(255, 107, 107, 0.15);
}

// 暗黑模式（后续扩展）
// [data-theme="dark"] {
//   --fd-primary: #FF8A8A;
//   --fd-bg: #1A1A2E;
//   --fd-text: #EAEAEA;
//   --fd-card-bg: #16213E;
// }
```

### 6.5 插件机制（预留）

```javascript
// plugins/index.js
// 后续可注册第三方能力：地图、支付、推送等

const plugins = []

export function registerPlugin(plugin) {
  plugin.install()
  plugins.push(plugin)
}

// 使用示例（未来）:
// registerPlugin(mapPlugin)    — 接入高德地图
// registerPlugin(payPlugin)    — 接入支付
// registerPlugin(pushPlugin)   — 接入消息推送
```

---

## 七、关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 状态管理 | Pinia（非 Vuex） | Vue 3 官方推荐，API更简洁，TS支持好 |
| 数据层 | Service 抽象 + Factory | 一行配置切换 local/remote，迁移成本为零 |
| 组件通信 | Props/Emit 为主 | 保持组件纯净，避免组件直接依赖 store |
| 业务复用 | Composables 模式 | 比 Mixin 更清晰，可组合，易测试 |
| 样式方案 | SCSS + CSS变量 | SCSS编写方便，CSS变量支持运行时主题切换 |
| 条件编译 | uni-app 条件编译 | 平台差异集中在 `platform.js`，页面层无感知 |

---

*文档版本: v1.0*
*创建日期: 2026-04-02*
