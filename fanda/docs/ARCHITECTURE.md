# 饭搭 — 项目架构设计文档

---

## 一、架构总览

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                            │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐  │
│  │ 首页  │ │ 日历 │ │ 预算  │ │ 拼饭  │ │ 我的 │ │ AI  │ │统计  │  │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘  │
│     │        │        │        │        │        │        │         │
│  ┌──┴────────┴────────┴────────┴────────┴────────┴────────┴────┐   │
│  │                  Components (fd-xxx)                          │   │
│  └─────────────────────────┬─────────────────────────────────── ┘   │
├───────────────────────────┼─────────────────────────────────────────┤
│                            │    Business Layer                        │
│  ┌─────────────────────────┴────────────────────────┐               │
│  │              Composables (useXxx)                 │               │
│  │   useRecommend / useRecord / useBudget            │               │
│  └─────────────────────────┬────────────────────────┘               │
│  ┌─────────────────────────┴────────────────────────┐               │
│  │              Stores (Pinia)                       │               │
│  │  food / record / budget / preference / achievement│               │
│  └─────────────────────────┬────────────────────────┘               │
├───────────────────────────┼─────────────────────────────────────────┤
│                            │    Data Layer                            │
│  ┌─────────────────────────┴────────────────────────┐               │
│  │              Service (api/xxx.js)                 │ ← 抽象接口    │
│  └────────┬─────────────────────────┬───────────────┘               │
│  ┌─────────┴──────┐        ┌────────┴────────┐                      │
│  │   LocalRepo    │        │   RemoteRepo     │ ← HTTP + Spring Boot │
│  │  (localStorage)│        │  (JWT + MyBatis) │                      │
│  └────────────────┘        └─────────────────┘                      │
├─────────────────────────────────────────────────────────────────────┤
│                       Backend (fanda-server)                          │
│  ┌───────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │  REST API │ │ WebSocket│ │ Spring AI│ │ Scheduler│              │
│  │Controllers│ │  Social  │ │  (Qwen)  │ │ (周报)   │              │
│  └───────────┘ └──────────┘ └──────────┘ └──────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 二、目录结构

### 前端（fanda/src/）

```
fanda/src/
├── pages/
│   ├── index/index.vue             # 首页-转盘推荐（加权随机、今日总结卡）
│   ├── calendar/calendar.vue       # 饮食日历（骨架屏、记录查看/删除）
│   ├── budget/budget.vue           # 预算管家（支出删除）
│   ├── social/social.vue           # 拼饭广场（骨架屏）
│   ├── social-detail/              # 拼饭详情（投票、AA账单、实时聊天、活动评价）
│   ├── friends/friends.vue         # 好友中心（搜索/申请/好友列表）
│   ├── profile/profile.vue         # 个人中心
│   ├── ai/ai.vue                   # AI 饮食顾问（多功能；小程序SSE降级）
│   ├── stats/stats.vue             # 饮食统计报告
│   └── login/login.vue             # 登录页
│
├── components/
│   ├── recommend/
│   │   ├── fd-wheel.vue            # 转盘组件（触觉反馈 vibrateShort）
│   │   ├── fd-food-result.vue      # 推荐结果（含推荐理由气泡）
│   │   └── fd-exclude-tags.vue     # 排除标签
│   ├── common/
│   │   ├── fd-nav-bar.vue
│   │   ├── fd-empty.vue
│   │   └── fd-skeleton.vue         # 骨架屏组件（shimmer 动画，rows 参数）
│
├── composables/
│   └── useRecommend.js             # 加权推荐算法、推荐理由生成
│
├── stores/modules/
│   ├── food.js                     # 菜品（离线缓存兜底，24h TTL + refresh()）
│   ├── record.js / budget.js
│   ├── preference.js / achievement.js
│   └── auth.js                     # JWT 登录态管理
│
├── utils/
│   ├── http.js                     # Axios 封装（JWT 拦截器；isOffline 错误标记）
│   ├── offlineQueue.js             # 离线操作队列（localStorage持久化，flush 回放）
│   ├── websocket.js                # WebSocket 工具（指数退避断线重连）
│   └── storage.js / date.js / format.js
│
└── config/index.js                 # dataMode: 'local' | 'remote'
```

### 后端（fanda-server/src/main/java/com/fanda/）

```
com/fanda/
├── controller/
│   ├── AuthController.java         # /api/auth/** 登录注册
│   ├── FoodController.java         # /api/foods（Redis 缓存 1h）
│   ├── RecordController.java       # /api/records
│   ├── BudgetController.java       # /api/budget
│   ├── ExpenseController.java      # /api/expenses
│   ├── PreferenceController.java   # /api/preferences（Redis 缓存 30min）
│   ├── SocialController.java       # /api/social/**（拼饭、投票、AA账单、活动评价）
│   ├── FriendController.java       # /api/friends/**（好友关系、搜索防枚举）
│   ├── StatsController.java        # /api/stats
│   ├── AchievementController.java  # /api/achievements
│   └── AiController.java           # /api/ai/** (7个AI端点，限流保护)
│
├── ai/
│   ├── FandaAiTools.java           # @Tool 工具集（Function Calling）
│   ├── FoodImageRecognizer.java    # qwen-vl 图片识别
│   └── WeeklyReportScheduler.java  # 每周周报定时任务
│
├── config/
│   ├── AiConfig.java               # ChatClient Bean (DashScope)
│   ├── SecurityConfig.java         # Spring Security + JWT
│   ├── CacheConfig.java            # Redis 缓存配置（@EnableCaching，分区 TTL）
│   ├── RateLimitInterceptor.java   # AI 接口限流（滑动窗口令牌桶）
│   ├── WebMvcConfig.java           # 注册拦截器
│   ├── CorsConfig.java
│   └── WebSocketConfig.java
│
├── entity/                         # MyBatis-Plus 实体
│   ├── User / FoodItem / MealRecord / Budget / Expense
│   ├── UserPreference / UserAllergy / UserDislike
│   ├── SocialGroup / SocialMember / SocialVote
│   ├── Friendship                  # 好友关系（pending/accepted）
│   ├── GroupReview                 # 拼饭活动评价（1-5星）
│   └── WeeklyReport                # 每周营养周报存储
│
├── security/
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
│
├── websocket/
│   └── SocialWebSocketHandler.java # 拼饭实时投票推送
│
└── init/
    └── DataInitializer.java        # 启动时初始化菜品数据 + 建表
```

---

## 三、AI 智能体架构

```
前端 ai.vue
     │
     ├── GET  /api/ai/weekly-report  ──→ WeeklyReportScheduler.generateReportForUser()
     ├── GET  /api/ai/budget-advice  ──→ ChatClient + FandaAiTools.getBudgetStatus()
     ├── GET  /api/ai/nutrition      ──→ ChatClient + FandaAiTools.getNutritionStats()
     ├── POST /api/ai/recommend      ──→ ChatClient.tools(fandaAiTools) [Function Calling]
     ├── POST /api/ai/chat (SSE)     ──→ ChatClient.stream() + 对话记忆(ConcurrentHashMap)
     ├── DELETE /api/ai/chat/history ──→ 清除对话记忆
     ├── POST /api/ai/recognize-food ──→ FoodImageRecognizer → DashScope qwen-vl-plus
     └── POST /api/ai/social-topic   ──→ ChatClient (破冰话题/活动总结)

FandaAiTools (@Tool 工具集)
     ├── getRecentMealRecords(userId, days)   ← MealRecordMapper
     ├── getNutritionStats(userId)            ← RecordNutritionMapper
     ├── getUserPreferences(userId)           ← UserPreference/Allergy/Dislike
     ├── getBudgetStatus(userId)              ← Budget + ExpenseMapper
     └── searchFoods(keyword)                 ← FoodItemMapper

WeeklyReportScheduler
     ├── @Scheduled(cron="0 0 2 * * MON")   ← 每周一凌晨2点触发
     ├── 查所有用户 → 调用 FandaAiTools 获取数据
     ├── 调用 ChatClient 生成报告文本
     └── 存入 fd_weekly_report 表（uk: user_id + week_start）
```

**对话记忆方案：**

```java
// 用 ConcurrentHashMap 自维护，绕开 Spring AI Memory API 版本兼容问题
Map<Long, Deque<String>> chatHistories = new ConcurrentHashMap<>();
// 最近 20 条（10轮）注入 system prompt 的历史对话段
// 用户注销/主动清除时调用 DELETE /api/ai/chat/history 清空
```

---

## 四、加权推荐算法

位于 [useRecommend.js](../src/composables/useRecommend.js)：

```
候选菜品池（过滤忌口/标签/时段/预算后）
        │
        ▼
computeWeights(pool)
  近 7天吃过  → w = 0.3  (降权)
  近14天吃过  → w = 0.7  (轻微降权)
  未吃过      → w = 1.0  (基准)
  收藏菜品    → w × 1.2 (加权)
        │
        ▼
加权随机抽取（类轮盘赌选择）
        │
        ▼
buildReason(food)   → 生成推荐理由气泡
  "收藏菜品 ❤️"  /  "久违了 😋"  /  "尝个鲜 🆕"
```

---

## 五、数据流

```
用户操作
   │
   ▼
Page（调用 composable 方法）
   │
   ▼
Composable（编排业务逻辑）
   │
   ▼
Store（更新状态 + 调用 service 持久化）
   │
   ├─ local mode → localStorage
   └─ remote mode → HTTP → Spring Boot → MySQL
                                 │
                         Spring AI (Qwen)
                         WebSocket (拼饭)
```

**确认一餐的完整流程：**

```
用户点"就吃这个"
  → onConfirm(cost)
    → confirmChoice(cost)               [useRecommend]
      → recordStore.add(record)         [持久化记录]
      → budgetStore.addExpense(cost)    [联动更新预算]
    → budgetStore.isOverBudget?
      → 是 → showToast("⚠️ 本月预算已超支！")
      → 否 → showToast("已记录 ✅")
    → achievementStore.check(...)       [成就检查]
```

---

## 六、API 端点总表

### 认证
| Method | Path | 说明 |
|--------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录（返回 JWT） |
| POST | /api/auth/refresh | 刷新 Token |

### 核心功能
| Method | Path | 说明 |
|--------|------|------|
| GET | /api/foods | 菜品列表 |
| GET/POST/DELETE | /api/records | 用餐记录 |
| GET/PUT | /api/budget | 月预算 |
| GET/POST/DELETE | /api/expenses | 消费记录 |
| GET/PUT | /api/preferences | 用户偏好 |
| GET/POST | /api/achievements | 成就 |
| GET | /api/stats | 统计报告 |

### 拼饭社交
| Method | Path | 说明 |
|--------|------|------|
| GET/POST | /api/social/groups | 拼饭列表/创建 |
| POST | /api/social/groups/{id}/join | 加入 |
| POST | /api/social/groups/{id}/vote | 投票 |
| GET | /api/social/groups/{id}/bill | AA账单估算 |
| GET/POST | /api/social/groups/{id}/reviews | 活动评价（查询/提交） |
| WS | /ws/social/{groupId} | 实时推送（含群聊） |

### 好友关系
| Method | Path | 说明 |
|--------|------|------|
| GET | /api/friends | 好友列表 |
| GET | /api/friends/requests | 收到的好友申请 |
| GET | /api/friends/search?keyword= | 搜索用户（防枚举，每分钟20次） |
| POST | /api/friends/request | 发送好友请求 |
| POST | /api/friends/accept/{id} | 接受申请 |
| POST | /api/friends/reject/{id} | 拒绝申请 |
| DELETE | /api/friends/{id} | 删除好友 |

### AI 智能体
| Method | Path | 说明 |
|--------|------|------|
| POST | /api/ai/chat | 流式对话（SSE，带记忆） |
| DELETE | /api/ai/chat/history | 清除对话记忆 |
| POST | /api/ai/recommend | 菜品推荐 Agent（Function Calling） |
| GET | /api/ai/nutrition | 30天营养分析报告 |
| GET | /api/ai/weekly-report | 本周营养周报（自动生成） |
| GET | /api/ai/budget-advice | 智能预算建议 |
| POST | /api/ai/recognize-food | 图片识别菜品（qwen-vl-plus） |
| POST | /api/ai/social-topic | 拼饭破冰话题/活动总结 |

---

## 七、技术栈

### 前端

| 层次 | 技术 |
|------|------|
| 框架 | uni-app + Vue 3 Composition API |
| 状态管理 | Pinia |
| HTTP | Axios（JWT 拦截器）|
| 样式 | SCSS + CSS 变量 |
| 平台 | H5（主）/ 小程序（条件编译适配）|
| SSE 流式 | 原生 `fetch` + `ReadableStream` |

### 后端

| 层次 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.5 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8 |
| 缓存 | Redis（Spring Cache，菜品1h/偏好30min/拼饭2min）|
| 认证 | Spring Security + JJWT 0.12.5 |
| 实时通信 | Spring WebSocket（指数退避断线重连）|
| AI | Spring AI Alibaba 1.0.0.2（通义千问 qwen-plus）|
| 视觉模型 | DashScope REST API（qwen-vl-plus）|
| 定时任务 | Spring `@Scheduled` |
| 限流 | 自实现滑动窗口令牌桶（AI 接口、搜索防枚举）|
| 连接池 | HikariCP |

---

## 八、关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 状态管理 | Pinia | Vue 3 官方推荐，API 简洁 |
| 数据层 | Service 抽象 + Factory | 一行配置切换 local/remote，迁移成本为零 |
| 推荐算法 | 加权随机（轮盘赌） | 避免重复推荐，同时保留多样性 |
| AI 记忆 | ConcurrentHashMap 自维护 | 绕开 spring-ai-alibaba 版本 Memory API 兼容问题 |
| 图片识别 | REST 直调 DashScope | qwen-vl 独立于 Spring AI ChatClient，直接调用更灵活 |
| 周报持久化 | `fd_weekly_report` + 唯一索引 | 幂等生成，重复调用不产生重复记录 |
| SSE 流式 | 原生 fetch（H5）/ POST 降级（小程序） | uni.request 不支持 SSE；小程序平台条件编译降级为普通请求 |
| JWT | access(2h) + refresh(7d) | 无感刷新，减少重新登录体验损耗 |
| Redis 缓存 | Spring Cache + 分区 TTL | 菜品/偏好高频读少写，缓存命中显著降低 DB 压力 |
| AI 限流 | 滑动窗口令牌桶（内存） | 无外部依赖，单实例适用，保护 DashScope 配额 |
| WS 重连 | 指数退避（最多5次） | 避免断网后瞬间洪泛重连，上限防止无效持续重试 |
| 好友搜索防枚举 | 频率限制 + 最短关键词 2字 | 防止遍历全量用户，兼顾性能与安全 |
| 活动评价 | 唯一索引 `(group_id, user_id)` | 保证每人只能评价一次，DB 层幂等兜底 |
| 离线写操作 | offlineQueue.js + localStorage | 网络断开时本地仍可操作，前台恢复后自动 flush 回放 |
| 菜品离线兜底 | localStorage 缓存 24h TTL | 无网络时转盘仍可使用缓存菜品，提示数据新鲜度 |
| 触觉反馈 | uni.vibrateShort（try/catch 兜底） | 转盘拨动/结果出现有震动感，不支持平台静默忽略 |
| 骨架屏 | fd-skeleton（shimmer 动画） | 数据加载中展示占位动画，避免白屏等待感 |
| 小程序图片权限 | getSetting + openSetting 引导 | 条件编译区分平台，小程序需显式申请相册/相机授权 |

---

## 九、体验层优化

### 离线记录队列同步

```text
用户操作（add/remove record）
        │
        ▼
record.js → service.save() / service.delete()
        │
    失败且 err.isOffline?
        │
    是 → offlineQueue.enqueue(type, 'record', payload)
        │              ↓
        │       localStorage 持久化
        │
        ▼
App.vue onShow（切换前台）
        │
        ▼
offlineQueue.flush({ record: { add, remove } })
        │
    逐条调用 service，成功则 dequeue
        │
        ▼
uni.showToast("已同步 N 条离线记录")
```

**关键细节：**

- `http.js` 的 `fail` 回调在错误对象上附加 `isOffline: true`，与服务端返回的业务错误区分
- 本地状态（`this.records`）无论网络是否成功都立即更新，保证 UI 响应
- 回放失败的条目保留在队列，等下次 `onShow` 重试

### 菜品数据离线兜底

```text
food.js load()
        │
    service.getAll() 成功？
        │
    是 → storage.set('food_cache', foods)   ← 写入本地缓存
        │   storage.set('food_cache_ts', Date.now())
        │
    否 → storage.get('food_cache') 有缓存？
              │
          是 → 使用缓存，showToast 提示数据新鲜度
          否 → 保持 loaded=false，showToast 提示网络错误
```

- 缓存 TTL 24 小时（仅作展示参考，每次联网都会刷新）
- `refresh()` 方法重置 `loaded = false` 后重新拉取，强制更新缓存

### 小程序平台适配

| 功能 | H5 | 小程序 |
| --- | --- | --- |
| AI 对话流式 | `fetch` + `ReadableStream` SSE | `POST /api/ai/chat` 普通请求，一次性返回 |
| 图片选取权限 | 直接 `uni.chooseImage` | 先 `getSetting` 检查 `scope.album`，未授权引导 `openSetting` |
| 图片转 base64 | `fetch(blob)` + `FileReader` | `uni.getFileSystemManager().readFile(encoding:'base64')` |

所有平台差异均通过 `// #ifdef H5` / `// #ifndef H5` 条件编译隔离，不影响构建产物体积。

### 交互细节打磨

| 组件 | 优化 |
| --- | --- |
| `fd-skeleton.vue` | shimmer 渐变动画骨架屏，`rows` 参数控制行数，宽度随行号自动变化 |
| `calendar.vue` | `recordLoading` ref 控制骨架屏显示，`await recordStore.load()` 完成后隐藏 |
| `social.vue` | `listLoading` ref + 骨架屏，拼饭列表加载完成前不渲染空态 |
| `fd-wheel.vue` | 点击 GO 时 `vibrateShort('medium')`，结果定格时 `vibrateShort('light')`；try/catch 静默处理不支持的平台 |

---

## 十、环境配置

```yaml
# application.yml 关键配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fanda
    password: ${DB_PASSWORD:tmh123}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}   # 本地开发可留空
  cache:
    type: redis
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:your-key}   # 必须配置
      chat.options:
        model: qwen-plus
        temperature: 0.7
        max-tokens: 2048

jwt:
  secret: ${JWT_SECRET:fanda-secret-key-...}
  access-expiration: 7200000    # 2小时
  refresh-expiration: 604800000 # 7天
```

**启动前必做：**

1. 在 IntelliJ Maven 面板点"重新加载所有 Maven 项目"（下载 spring-ai-alibaba + Redis 依赖）
2. 配置环境变量 `DASHSCOPE_API_KEY=sk-xxxxxx`（从 dashscope.aliyun.com 获取）
3. 确保 MySQL 已启动，数据库会自动创建
4. 本地启动 Redis（`redis-server`，默认 6379）；生产环境通过 `REDIS_HOST/PORT/PASSWORD` 注入

---

*文档版本: v4.0*
*更新日期: 2026-04-16*
*主要变更: 体验层优化（离线队列同步、菜品离线兜底、小程序平台适配、骨架屏 + 触觉反馈）*
