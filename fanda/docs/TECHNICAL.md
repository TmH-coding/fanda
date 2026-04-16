# 饭搭 — 技术实现文档

> 记录当前已实现的完整功能与架构，供开发维护参考。
>
> 最后更新：2026-04-16（v2.1）

---

## 目录

1. [项目总览](#1-项目总览)
2. [目录结构](#2-目录结构)
3. [前端架构](#3-前端架构)
4. [后端架构](#4-后端架构)
5. [数据库设计](#5-数据库设计)
6. [API 接口清单](#6-api-接口清单)
7. [已实现功能模块](#7-已实现功能模块)
8. [离线队列机制](#8-离线队列机制)
9. [AI 饮食顾问](#9-ai-饮食顾问)
10. [已知问题与注意事项](#10-已知问题与注意事项)

---

## 1. 项目总览

**饭搭**是一款面向都市打工人的智能饮食决策助手，提供转盘推荐、饮食记录、预算管理、拼饭社交、好友动态、AI 顾问六大核心模块。

### 技术栈

| 层 | 技术 | 说明 |
|---|---|---|
| 前端 | uni-app (Vue 3 Composition API) | 跨端，H5 + 微信小程序 |
| 状态管理 | Pinia | 按领域拆分 store |
| 后端 | Spring Boot 3 + MyBatis-Plus | REST API |
| 数据库 | MySQL 8 | 主数据库 |
| 认证 | Spring Security + JWT | 无状态鉴权 |
| AI | Spring AI Alibaba (DashScope) | 流式对话 |
| 缓存 | Spring Cache (内存) | 菜品列表缓存 |

### 运行模式

通过 `fanda/src/config/index.js` 中的 `dataMode` 切换：

- `local` — 纯本地模式，所有数据存 uni.storage，无需后端
- `remote` — 远程模式，调用 Spring Boot 后端，需要登录

---

## 2. 目录结构

### 前端 (`fanda/src/`)

```
src/
├── pages/
│   ├── login/login.vue          # 登录/注册
│   ├── index/index.vue          # 首页-转盘推荐
│   ├── calendar/calendar.vue    # 饮食日历
│   ├── budget/budget.vue        # 预算管家
│   ├── social/social.vue        # 拼饭广场
│   ├── social-detail/social-detail.vue  # 拼饭详情（投票/AA账单/WebSocket/评价）
│   ├── profile/profile.vue      # 个人中心
│   ├── stats/stats.vue          # 数据统计报告
│   ├── ai/ai.vue                # AI 饮食顾问（SSE 流式）
│   ├── friends/friends.vue      # 好友管理（动态/列表/请求/搜索）
│   └── food-create/food-create.vue  # 录入自定义食物
├── components/
│   ├── common/
│   │   ├── fd-nav-bar.vue       # 顶部导航栏（支持左/右插槽）
│   │   ├── fd-empty.vue         # 空状态占位
│   │   ├── fd-error-state.vue   # 错误状态 + 重试按钮
│   │   ├── fd-heatmap.vue       # 打卡热力图（GitHub 风格）
│   │   ├── fd-modal.vue         # 通用弹窗
│   │   ├── fd-skeleton.vue      # 骨架屏
│   │   └── fd-food-picker.vue   # 食物选择器（搜索 + 分类筛选）
│   └── recommend/
│       ├── fd-wheel.vue         # 转盘组件（4s 动画 + 震动反馈）
│       ├── fd-food-result.vue   # 推荐结果卡片（含菜品详情弹窗）
│       └── fd-exclude-tags.vue  # 排除标签选择器
├── stores/modules/
│   ├── auth.js                  # 登录状态、token
│   ├── food.js                  # 菜品数据
│   ├── record.js                # 用餐记录
│   ├── budget.js                # 预算
│   ├── preference.js            # 用户偏好（收藏/黑名单/忌口）
│   └── achievement.js           # 成就系统
├── services/
│   ├── factory.js               # getService(name) 按模式返回实现
│   ├── local/                   # 本地存储实现
│   └── remote/                  # HTTP 远程实现
├── composables/
│   └── useOfflineSync.js        # 离线队列 pendingCount 响应式状态
├── utils/
│   ├── http.js                  # uni.request 封装（get/post/put/del）
│   ├── storage.js               # uni.storage 封装
│   ├── offlineQueue.js          # 离线操作队列（enqueue/flush/peek）
│   ├── date.js                  # 日期工具、mealTypeLabel()
│   └── websocket.js             # createSocialSocket WebSocket 封装
├── config/
│   ├── index.js                 # dataMode 配置
│   ├── theme.js                 # spicyLevels 等常量
│   └── constants.js             # FOOD_CATEGORIES / NUTRITION_TYPES / MEAL_TYPES
└── data/
    └── achievements.js          # 成就定义 + ACHIEVEMENT_CATEGORIES
```

### 后端 (`fanda-server/src/main/java/com/fanda/`)

```
controller/          # REST 接口层（12 个 Controller）
dto/
  request/           # 请求 DTO（带 @Valid 校验）
  response/          # 响应 DTO（ApiResponse 统一封装）
entity/              # MyBatis-Plus 实体（对应数据库表）
mapper/              # MyBatis-Plus Mapper 接口
exception/           # BusinessException + ErrorCode 枚举
config/              # SecurityConfig / CacheConfig / AI配置
```

---

## 3. 前端架构

### Store 模式

所有写操作通过 `getService(name)` 路由到 local/remote 实现：

```js
// services/factory.js
export function getService(name) {
  return config.dataMode === 'remote'
    ? remoteServices[name]
    : localServices[name]
}
```

本地实现直接读写 `uni.storage`；远程实现调用对应 REST 接口。

### 离线队列

网络断开时，写操作通过 `enqueue(type, resource, payload)` 入队，存入 `uni.storage['offline_queue']`。联网恢复时（`uni.onNetworkStatusChange`）或手动触发，`flush(handlers)` 按序回放。

队列项结构：
```json
{ "id": "...", "type": "add|remove", "resource": "record|budget", "payload": {...}, "createdAt": "..." }
```

flush handlers 接受 `{ record: { add, remove }, budget: { add, remove } }` 形式。

### 热力图组件

`fd-heatmap.vue` 接受 `countMap`（`{ "2026-04-01": 3 }` 格式），渲染最近 N 周的 7×N 格热力图。每格颜色分 5 级（0-4），通过 `toLevel(count)` 映射。支持 `cell-tap` 事件回传 `{ date, count }`。

### 错误状态组件

`fd-error-state.vue` — Props: `icon`（默认 ⚠️）、`text`、`retryText`；emit `retry`。各数据页在 catch 中设 `loadError.value = true` 并展示此组件。

---

## 4. 后端架构

### 统一响应格式

```java
// ApiResponse<T>
{ "code": 200, "msg": "ok", "data": ... }
```

错误通过 `BusinessException(ErrorCode)` 抛出，全局异常处理器捕获并返回对应 HTTP 状态码。

### 认证流程

1. `POST /api/auth/register` / `POST /api/auth/login` 返回 JWT access token
2. 前端存入 `uni.storage['fd_access_token']`，每次请求通过 `Authorization: Bearer <token>` 携带
3. `SecurityConfig` 放行登录/注册接口，其余接口需要认证
4. Controller 通过 `Authentication.getName()` 获取当前用户名，再查库得 userId

### 缓存策略

菜品列表（无参数 + 无认证调用）缓存在内存中，key = `foods::system`。创建/删除自定义食物时 `@CacheEvict(allEntries = true)` 清空。

---

## 5. 数据库设计

全部表前缀 `fd_`，字符集 `utf8mb4`。

### 核心表

| 表名 | 说明 |
|------|------|
| `fd_user` | 用户账号（username/password/nickname/avatar） |
| `fd_food_item` | 菜品主表（isSystem=1 系统内置，isSystem=0 用户自建） |
| `fd_food_tag` | 菜品标签（多对一） |
| `fd_food_nutrition` | 菜品营养类型 |
| `fd_food_allergen` | 菜品过敏原 |
| `fd_food_meal_time` | 菜品适用餐次 |
| `fd_meal_record` | 用餐记录（含 `rating TINYINT` 评分字段） |
| `fd_record_nutrition` | 记录营养标签（多对一） |
| `fd_budget` | 用户预算（monthly/weekly） |
| `fd_expense` | 支出流水 |
| `fd_user_preference` | 偏好设置（spicy_level） |
| `fd_user_allergy` | 忌口列表 |
| `fd_user_fav_category` | 喜好分类 |
| `fd_user_dislike` | 不喜欢食材 |
| `fd_user_favorite_food` | 收藏菜品（user + food 唯一索引） |
| `fd_user_blacklist_food` | 黑名单菜品 |
| `fd_social_group` | 拼饭组 |
| `fd_social_tag` | 拼饭标签 |
| `fd_social_candidate` | 拼饭候选餐厅（含票数） |
| `fd_social_member` | 拼饭成员 |
| `fd_social_vote` | 投票记录（user + group 唯一，防重投） |
| `fd_achievement_unlock` | 已解锁成就 |
| `fd_friendship` | 好友关系（requester/addressee/status: pending\|accepted） |
| `fd_group_review` | 拼饭组评价（score + comment） |
| `fd_group_message` | 拼饭组留言板（group_id/user_id/username 冗余/content VARCHAR(300)） |

### 关键字段说明

- `fd_meal_record.rating` — TINYINT NULL，1-5 星，通过 `PATCH /api/records/{id}/rating` 更新
- `fd_food_item.is_system` — TINYINT(1)，0=用户自建，1=系统内置
- `fd_friendship.status` — `pending`（待接受）/ `accepted`（已成为好友）
- `fd_group_message.username` — 冗余字段，保存发送时的显示名（避免联表）；`content` 最长 300 字符

---

## 6. API 接口清单

### 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录，返回 JWT |

### 菜品 `/api/foods`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/foods` | 菜品列表（支持 keyword/category 筛选，认证后含自建） |
| GET | `/api/foods/mine` | 当前用户自建菜品列表 |
| GET | `/api/foods/{foodCode}` | 菜品详情 |
| POST | `/api/foods` | 创建自定义食物（需认证） |
| DELETE | `/api/foods/{foodCode}` | 删除自建食物（需是创建者） |

### 用餐记录 `/api/records`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/records` | 记录列表（支持 date / year+month 筛选） |
| POST | `/api/records` | 创建记录（含营养标签） |
| DELETE | `/api/records/{id}` | 删除记录 |
| PATCH | `/api/records/{id}/rating` | 评分（body: `{ "score": 1-5 }`） |

### 预算 `/api/budget`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/budget` | 获取预算设置 |
| PUT | `/api/budget` | 更新预算 |

### 支出 `/api/expenses`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/expenses` | 支出列表（支持 year+month 筛选） |
| POST | `/api/expenses` | 创建支出 |
| DELETE | `/api/expenses/{id}` | 删除支出 |

### 统计 `/api/stats`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/stats` | 综合统计（records / expense / nutrition） |

响应结构：
```json
{
  "records": { "total": 42, "streak": 7, "uniqueFoods": 15, "mealTypeDist": {...} },
  "expense": { "total": 1280.5, "avgPerDay": 45.0, "activeDays": 28 },
  "nutrition": { "碳水": 20, "蛋白质": 15, ... }
}
```

### 偏好 `/api/preferences`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/preferences` | 获取全量偏好 |
| PUT | `/api/preferences` | 更新偏好 |
| POST | `/api/preferences/favorites/{foodCode}` | 收藏菜品 |
| DELETE | `/api/preferences/favorites/{foodCode}` | 取消收藏 |
| POST | `/api/preferences/blacklist/{foodCode}` | 加入黑名单 |
| DELETE | `/api/preferences/blacklist/{foodCode}` | 移出黑名单 |

### 成就 `/api/achievements`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/achievements` | 已解锁成就列表 |
| POST | `/api/achievements/{id}/unlock` | 解锁成就 |

### 拼饭社交 `/api/social/groups`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/social/groups` | 拼饭列表（支持 status 筛选） |
| GET | `/api/social/groups/{id}` | 拼饭组详情 |
| POST | `/api/social/groups` | 创建拼饭组 |
| POST | `/api/social/groups/{id}/join` | 加入 |
| POST | `/api/social/groups/{id}/vote` | 投票 |
| GET | `/api/social/groups/{id}/bill` | AA 账单（人均估算） |
| GET | `/api/social/groups/{id}/messages` | 留言列表（最新 30 条，按时间正序） |
| POST | `/api/social/groups/{id}/messages` | 发送留言（content ≤ 300 字） |
| GET | `/api/social/groups/{id}/reviews` | 活动评价列表 |
| POST | `/api/social/groups/{id}/reviews` | 提交评价（需是成员，每人一次） |

### 好友 `/api/friends`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/friends/feed` | 好友最近 30 条用餐动态 |
| GET | `/api/friends/search?keyword=` | 按用户名/昵称搜索（≥2字符，限频 20次/分钟） |
| GET | `/api/friends` | 好友列表 |
| GET | `/api/friends/requests` | 收到的好友请求 |
| POST | `/api/friends/request` | 发送好友请求（body: `{ "userId": 123 }`） |
| POST | `/api/friends/accept/{id}` | 接受请求 |
| POST | `/api/friends/reject/{id}` | 拒绝请求 |
| DELETE | `/api/friends/{id}` | 删除好友 |

### AI 顾问 `/api/ai`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | 普通对话，返回完整文本 |
| GET | `/api/ai/stream?message=` | SSE 流式对话 |

H5 端使用 `fetch` + `ReadableStream` 接收 SSE；小程序端回退到 `POST /api/ai/chat`。

### 用户 `/api/user`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/me` | 当前用户信息 |
| PUT | `/api/user/me` | 更新昵称/头像 |

---

## 7. 已实现功能模块

### 7.1 登录 / 注册

- `pages/login/login.vue`
- 支持账号注册（username/password）
- 登录成功后 token 存 `uni.storage['fd_access_token']`，写入 auth store
- 未登录时 `onLaunch` 跳转到登录页

### 7.2 转盘推荐

- `pages/index/index.vue`
- 根据时段（早/中/晚/宵夜）过滤菜品
- 应用用户偏好（辣度/忌口/黑名单/排除法）
- **今日去重** — 优先排除当日已记录的菜品；若去除后候选数 < 3，则回退到完整候选池
- **预算感知价格过滤** — 超支时仅显示均价 ≤ 预算日均 × 0.8 的菜品；未超支时阈值为 1.5 倍
- **菜品详情弹窗** — 推荐结果卡片中菜品名旁显示 ℹ️，点击弹出底部抽屉，展示分类/价格区间/适合餐次/营养标签/过敏原/特色标签；弹窗内支持直接收藏或确认选择
- "就吃这个"直接记录到当日对应餐次

### 7.3 饮食日历

- `pages/calendar/calendar.vue`
- 月历视图，有记录的日期标圆点
- **快速录入** — 日历页顶部"+ 记录"按钮，弹出录入表单：
  - 食物选择栏点击打开 `fd-food-picker` 选择器（带搜索 + 分类筛选）
  - 选菜后自动填充营养标签和价格中值
  - 支持餐次切换、花费输入、多选营养标签
- 支持按日查看三餐列表（含评分）
- **营养均衡提醒** — 检查近 3 天记录，若无任何蔬菜/水果营养标签，在日历顶部显示提示横幅

### 7.4 预算管家

- `pages/budget/budget.vue`
- 月度/周度预算设置
- 环形进度图展示消耗比例
- 每日建议额度计算
- 消费流水列表

### 7.5 拼饭社交

- `pages/social/social.vue`
- 发起拼饭（标题/地点/时间/人数/候选餐厅）
- 投票选餐（每人一票，防重投）
- 加入/退出/解散
- **拼饭留言板**（远程模式）— 每个拼饭卡片下方可展开留言板，懒加载（点击"查看留言 ›"触发），发送留言实时追加，支持最新 30 条消息

### 7.6 个人中心

- `pages/profile/profile.vue`
- 用户信息展示（头像/昵称/成就进度）
- 数据概览（记录餐数/连续天数/尝试菜品/收藏数）
- **打卡热力图** — 过去 12 周记录分布
- **口味偏好** — 辣度、忌口标签
- **成就徽章** — 分类 Tab + 进度条 + 密成就
- **收藏夹** — 展示/取消收藏；支持**批量操作**（批量模式下逐项勾选/全选，一键批量取消收藏）
- **不想吃列表** — 展示/移出黑名单；同样支持**批量操作**（批量移出）
- **我录入的食物** — 展示用户自建菜品 + 删除（远程模式）
- **离线同步徽章** — 有队列时显示待同步数，点击手动同步
- 导航入口：AI 顾问 / 我的好友 / 数据报告 / 录入食物
- 退出登录（远程模式）

### 7.7 数据统计报告

- `pages/stats/stats.vue`
- Tab 切换：概览 / 营养 / 餐次分布
- 概览：总支出 / 日均 / 活跃天数
- 营养分布：各类型记录数柱状图
- 餐次分布：早/中/晚/宵夜比例
- **月度报告导出** — Canvas 绘制图片，保存到相册
- **月初智能提醒** — `App.vue` 在 `onShow` 中调用 `checkMonthlyReportReminder()`；每月 1-3 日首次启动弹窗提示查看上月报告，用 `uni.storage` key `fd_monthly_reminder_YYYY_M` 去重（每月只提示一次）
- 错误状态组件 + 重试

### 7.8 AI 饮食顾问

- `pages/ai/ai.vue`
- 系统 Prompt 包含用户偏好、近期记录、预算状态
- H5：`fetch` + `ReadableStream` 逐字流式输出
- 小程序：回退到 POST 全量响应
- 对话历史保存在 session 内（不持久化）

### 7.9 好友系统

- `pages/friends/friends.vue`
- 四个 Tab：**动态** / 好友 / 请求 / 搜索
- 动态：好友最近 30 条用餐记录，含昵称/头像/菜品/费用/评分（rating 字段由后端 FriendController 注入到 feed 响应 Map）
- 搜索：按用户名/昵称模糊搜，显示当前关系状态（none/pending/accepted）
- 好友请求：发送/接受/拒绝
- 删除好友

### 7.10 录入自定义食物

- `pages/food-create/food-create.vue`
- 食物名称（必填）/ 分类（必填，Tag 单选）/ 价格区间
- 营养类型（多选）/ 适用餐次（多选）
- 提交后刷新 food store，新食物立即出现在推荐候选中
- 仅远程模式有效（`POST /api/foods`）

### 7.11 成就实时触发

成就检查在每次用餐记录添加后自动执行，无需手动刷新：

1. `record.js` 的 `add` action 成功保存记录后，通过动态 `import()` 加载 achievement/preference store（避免循环依赖）
2. 构建当前 stats 对象（totalRecords / streak / uniqueFoods / favCategories 等）
3. 调用 `achStore.check(stats)` 返回新解锁成就数组
4. 对每个新成就调用 `uni.showToast({ title: '🏆 解锁成就：${name}！', duration: 3000 })`

```js
// record.js — add action（简化示意）
const { useAchievementStore } = await import('@/stores/modules/achievement')
const stats = { totalRecords: this.records.length, streak: this.streak, ... }
const newAchs = await achStore.check(stats)
for (const ach of newAchs) {
  uni.showToast({ title: `🏆 解锁成就：${ach.name}！`, icon: 'none', duration: 3000 })
}
```

> 动态 import 是规避 Pinia store 循环依赖的标准做法：record → achievement → record 的静态 import 链会导致模块初始化死锁。

---

## 8. 离线队列机制

### 入队

```js
import { enqueue } from '@/utils/offlineQueue'
enqueue('add', 'record', payload)    // 离线时创建记录
enqueue('remove', 'record', id)      // 离线时删除记录
enqueue('add', 'budget', expense)    // 离线时添加支出
enqueue('remove', 'budget', id)      // 离线时删除支出
```

### 回放

`App.vue` 在两处触发 `flushOfflineQueue()`：
1. `uni.onNetworkStatusChange` — 网络恢复时自动触发（仅远程模式）
2. `onShow` — 每次 App 切换到前台时触发

`profile.vue` 手动同步按钮：点击同步徽章触发 `manualSync()`。

### 状态展示

`useOfflineSync()` composable 内部以 5 秒轮询 `queueSize()`，向外暴露响应式 `pendingCount`。profile 页通过它展示徽章数字。

---

## 9. AI 饮食顾问

### 系统 Prompt 构建

每次会话从以下数据构建上下文：
- 当前用户偏好（辣度、忌口、喜好分类）
- 本月已花费与剩余预算
- 近 7 天用餐记录摘要（去重菜品列表）

### 后端实现

`AiController` 使用 Spring AI Alibaba `ChatClient`：
- `/api/ai/chat` — `chatClient.prompt(messages).call().content()`
- `/api/ai/stream` — `chatClient.prompt(messages).stream()` + SSE `SseEmitter`

### H5 流式接收

```js
const response = await fetch(url, { headers: { Authorization: ... } })
const reader = response.body.getReader()
const decoder = new TextDecoder()
while (true) {
  const { done, value } = await reader.read()
  if (done) break
  const chunk = decoder.decode(value)
  // 解析 SSE data: 行，追加到 aiReply
}
```

---

## 10. 已知问题与注意事项

### Lombok IDE 误报

VS Code Java Language Server 若未正确加载 Lombok javaagent，会在所有使用 `@Data` / `@Builder` 的实体类上报 `cannot find symbol`（`getXxx()` / `setXxx()`）。这是 IDE 问题，**不影响编译和运行**。

修复方式：在 `.vscode/settings.json` 中添加 javaagent 路径后，执行 "Java: Clean Language Server Workspace"。

### 数据库初始化

`fanda-server/src/main/resources/db/schema.sql` 包含所有建表语句（`IF NOT EXISTS`），可幂等执行。应用启动前确保 MySQL 服务可用、数据库 `fanda` 已创建（脚本第一行会自动创建）。

### 离线队列 handler key

`flush(handlers)` 内部只识别 `handler.add` 和 `handler.remove`，budget handlers 须使用这两个 key：

```js
// 正确
budget: { add: ..., remove: ... }
// 错误（旧代码遗留，已修复）
budget: { addExpense: ..., removeExpense: ... }
```

### 菜品 id vs foodCode

- `fd_food_item` 数据库主键 `id` 是 BIGINT 自增
- 对外暴露的是 `food_code`（系统菜品如 `FOOD_001`，用户自建如 `U_ABCD1234`）
- `FoodItemResponse.id` 字段实际存的是 `foodCode`，前端所有收藏/黑名单/删除操作传 `foodCode` 字符串

### Stats 接口字段名

`GET /api/stats` 响应中 expense 字段为：
- `total`（不是 `totalAmount`）
- `avgPerDay`（不是 `dailyAverage`）
- `activeDays`

前端 stats.vue 和导出函数均已对齐。

---

*文档版本: v2.1*
*更新日期: 2026-04-16*

### 变更记录（v2.1）

- 新增 `fd_group_message` 表（留言板）
- 新增 API：`GET/POST /api/social/groups/{id}/messages`、`GET/POST /api/social/groups/{id}/reviews`、`GET /api/social/groups/{id}/bill`
- 7.2 转盘推荐：今日去重 + 预算感知价格过滤
- 7.3 饮食日历：近 3 天营养均衡提醒
- 7.5 拼饭社交：留言板（懒加载）
- 7.7 数据统计报告：月初智能提醒（App.vue onShow）
- 7.9 好友系统：feed 接口补充 rating 字段
- 新增 7.11 成就实时触发机制（动态 import 避免循环依赖）
