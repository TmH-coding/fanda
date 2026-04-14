# 饭搭 (FanDa) 项目文档

> 打工人智能饮食决策助手 — 全栈技术文档

---

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [后端架构](#后端架构)
- [前端架构](#前端架构)
- [数据库设计](#数据库设计)
- [API 接口文档](#api-接口文档)
- [WebSocket 实时通信](#websocket-实时通信)
- [启动指南](#启动指南)
- [功能模块](#功能模块)

---

## 项目概述

**饭搭**是一款面向都市打工人的移动端饮食助手，提供：

| 模块 | 功能 |
|------|------|
| 转盘推荐 | 智能推荐、排除法、偏好过滤、一键记录 |
| 饮食日历 | 月历视图、三餐记录、营养标签 |
| 预算管家 | 预算设定、消费追踪、超支预警 |
| 拼饭广场 | 发起拼饭、实时投票、WebSocket 群聊 |
| 数据报告 | 消费趋势图、营养摄入分析、用餐习惯统计 |
| 个人中心 | 口味偏好、忌口设置、成就系统、收藏夹 |

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | 核心框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| Spring Security | 6.x | 认证授权 |
| Spring WebSocket | 6.x | 实时通信 |
| JJWT | 0.12.5 | JWT 令牌 |
| MySQL | 8.0+ | 关系型数据库 |
| Lombok | latest | 代码简化 |
| Java | 17 | 运行环境 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| uni-app | 3.0.0 | 跨平台框架 |
| Vue | 3.4.38 | 前端框架 |
| Pinia | 2.1.7 | 状态管理 |
| Vite | 5.2.8 | 构建工具 |
| Day.js | 1.11.12 | 日期处理 |
| SCSS | 1.77.8 | 样式预处理 |

---

## 项目结构

```
fanda-project/
├── fanda/                          # 前端 (uni-app)
│   ├── src/
│   │   ├── pages/                  # 页面
│   │   │   ├── index/              # 首页 - 转盘推荐
│   │   │   ├── calendar/           # 饮食日历
│   │   │   ├── budget/             # 预算管家
│   │   │   ├── social/             # 拼饭广场
│   │   │   ├── social-detail/      # 拼饭详情（WebSocket）
│   │   │   ├── stats/              # 数据统计报告
│   │   │   ├── profile/            # 个人中心
│   │   │   └── login/              # 登录/注册
│   │   ├── stores/                 # Pinia 状态管理
│   │   │   └── modules/
│   │   │       ├── auth.js         # 认证状态
│   │   │       ├── food.js         # 菜品数据
│   │   │       ├── record.js       # 用餐记录
│   │   │       ├── budget.js       # 预算管理
│   │   │       ├── preference.js   # 用户偏好
│   │   │       ├── social.js       # 拼饭社交
│   │   │       └── achievement.js  # 成就系统
│   │   ├── services/               # 数据服务层
│   │   │   ├── local/              # 本地存储实现
│   │   │   └── remote/             # 远程 API 实现
│   │   ├── utils/
│   │   │   ├── http.js             # HTTP 封装（JWT 自动刷新）
│   │   │   ├── websocket.js        # WebSocket 封装（自动重连）
│   │   │   ├── storage.js          # 本地存储工具
│   │   │   └── date.js             # 日期工具
│   │   ├── components/             # 公共组件
│   │   ├── config/
│   │   │   └── index.js            # 全局配置（dataMode: remote）
│   │   └── styles/                 # 全局样式变量
│   ├── vite.config.js              # Vite 配置（含 API 代理）
│   └── package.json
│
├── fanda-server/                   # 后端 (Spring Boot)
│   └── src/main/java/com/fanda/
│       ├── FandaApplication.java   # 启动类（@MapperScan）
│       ├── config/
│       │   ├── SecurityConfig.java # Spring Security + JWT 配置
│       │   ├── CorsConfig.java     # 跨域配置
│       │   └── WebSocketConfig.java# WebSocket 端点注册
│       ├── controller/             # REST API 控制器（10个）
│       │   ├── AuthController.java
│       │   ├── UserController.java
│       │   ├── FoodController.java
│       │   ├── RecordController.java
│       │   ├── BudgetController.java
│       │   ├── ExpenseController.java
│       │   ├── PreferenceController.java
│       │   ├── SocialController.java
│       │   ├── AchievementController.java
│       │   └── StatsController.java
│       ├── entity/                 # 数据实体（MyBatis-Plus）
│       ├── mapper/                 # Mapper 接口（BaseMapper）
│       ├── dto/
│       │   ├── request/            # 请求 DTO（含校验注解）
│       │   └── response/           # 响应 DTO
│       ├── websocket/              # WebSocket 实时通信
│       │   ├── WsMessage.java      # 消息模型
│       │   ├── SocialRoomManager.java  # 房间管理
│       │   └── SocialWebSocketHandler.java # 连接处理
│       ├── security/               # JWT 认证
│       │   ├── JwtTokenProvider.java
│       │   ├── JwtAuthenticationFilter.java
│       │   └── UserDetailsServiceImpl.java
│       ├── exception/              # 全局异常处理
│       ├── init/                   # 数据初始化（80+ 内置菜品）
│       └── config/                 # MetaObjectHandler（时间戳自动填充）
│
├── ARCHITECTURE.md                 # 架构文档（本文件）
├── STARTUP_GUIDE.md                # 启动指南
└── ARCHITECTURE.md
```

---

## 后端架构

### 分层设计

```
HTTP 请求
    ↓
JwtAuthenticationFilter        ← 验证 Bearer Token
    ↓
Controller（REST API）         ← 入参校验、权限校验
    ↓
Mapper（MyBatis-Plus）         ← 数据库 CRUD
    ↓
MySQL 数据库
```

### 安全机制

- **无状态 JWT**：Access Token（2小时）+ Refresh Token（7天）
- **BCrypt 密码加密**
- **Spring Security**：公开路由白名单 + 全局鉴权
- **WebSocket 鉴权**：连接时通过 query 参数传 Token，服务端验证

### 关键设计

**MyBatis-Plus 自动填充**（`MetaObjectHandler`）

```java
// createdAt：INSERT 时自动填充
// updatedAt：INSERT + UPDATE 时自动填充
```

**统一响应格式**（`ApiResponse<T>`）

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**全局异常处理**（`GlobalExceptionHandler`）

- `BusinessException` → 业务错误码
- `MethodArgumentNotValidException` → 参数校验失败
- `Exception` → 兜底处理

---

## 前端架构

### 数据流

```
页面组件 → Pinia Store → Service（local/remote）→ HTTP/Storage
```

### 数据模式切换

```javascript
// src/config/index.js
dataMode: 'remote'   // 'local' | 'remote'
apiBaseUrl: ''       // H5 开发模式通过 Vite proxy 代理到 :8080
```

### HTTP 工具（`utils/http.js`）

- 自动附加 `Authorization: Bearer <token>` 请求头
- 401 自动刷新 Token 并重试原请求
- 刷新失败自动跳转登录页

### Vite 开发代理

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

### WebSocket 工具（`utils/websocket.js`）

```javascript
const socket = createSocialSocket(groupId, {
  onOpen: () => {},
  onMessage: (msg) => {},  // msg.type: JOIN | VOTE | CHAT | FULL
  onClose: () => {},
})
socket.sendChat('消息内容')
socket.close()
```

---

## 数据库设计

### 表结构总览

| 表名 | 说明 |
|------|------|
| `fd_user` | 用户基本信息 |
| `fd_food_item` | 菜品主表（含系统菜品 + 用户自定义） |
| `fd_food_tag` | 菜品口味标签 |
| `fd_food_nutrition` | 菜品营养类型 |
| `fd_food_allergen` | 菜品过敏原 |
| `fd_food_meal_time` | 菜品适用餐次 |
| `fd_meal_record` | 用餐记录 |
| `fd_record_nutrition` | 用餐营养记录 |
| `fd_budget` | 用户预算设置 |
| `fd_expense` | 消费记录 |
| `fd_user_preference` | 用户偏好（辣度等） |
| `fd_user_allergy` | 忌口设置 |
| `fd_user_fav_category` | 偏好分类 |
| `fd_user_dislike` | 不喜欢的食物 |
| `fd_user_favorite_food` | 收藏的菜品 |
| `fd_social_group` | 拼饭活动 |
| `fd_social_tag` | 拼饭标签 |
| `fd_social_candidate` | 候选餐厅/菜品（含票数） |
| `fd_social_member` | 拼饭成员 |
| `fd_social_vote` | 投票记录 |
| `fd_achievement_unlock` | 成就解锁记录 |

### 关键关系

```
fd_user (1) ──→ (N) fd_meal_record
fd_meal_record (1) ──→ (N) fd_record_nutrition
fd_user (1) ──→ (N) fd_expense
fd_user (1) ──→ (1) fd_budget
fd_user (1) ──→ (1) fd_user_preference
fd_food_item (1) ──→ (N) fd_food_tag / fd_food_nutrition / fd_food_allergen / fd_food_meal_time
fd_social_group (1) ──→ (N) fd_social_candidate / fd_social_member / fd_social_tag
```

---

## API 接口文档

> 所有接口前缀：`http://localhost:8080`
> 认证接口以外，请求头需携带：`Authorization: Bearer <token>`

### 认证 `/api/auth`

| 方法 | 路径 | 说明 | 公开 |
|------|------|------|------|
| POST | `/api/auth/register` | 注册 | ✅ |
| POST | `/api/auth/login` | 登录 | ✅ |
| POST | `/api/auth/refresh` | 刷新 Token | ✅ |

**注册请求体**
```json
{ "username": "test", "password": "123456", "nickname": "测试用户" }
```

**登录响应**
```json
{
  "code": 200,
  "data": {
    "token": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 7200000,
    "user": { "id": 1, "username": "test", "nickname": "测试用户" }
  }
}
```

---

### 菜品 `/api/foods`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/api/foods` | 获取菜品列表 | `category`, `keyword` |
| GET | `/api/foods/{foodCode}` | 获取单个菜品 | - |
| POST | `/api/foods` | 新增自定义菜品 | 需登录 |

**菜品响应结构**
```json
{
  "id": "n007",
  "name": "红烧肉",
  "category": "中式",
  "priceRange": [20, 35],
  "tags": ["咸鲜", "下饭"],
  "nutrition": ["protein", "fat"],
  "allergens": [],
  "mealTime": ["lunch", "dinner"]
}
```

---

### 用餐记录 `/api/records`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/api/records` | 获取记录列表 | `date`, `year`, `month` |
| POST | `/api/records` | 新增记录 | - |
| DELETE | `/api/records/{id}` | 删除记录 | - |

**新增记录请求体**
```json
{
  "date": "2026-04-13",
  "mealType": "lunch",
  "foodName": "红烧肉",
  "foodCode": "n007",
  "cost": 25.00,
  "nutrition": ["protein", "fat"]
}
```

---

### 预算 `/api/budget` & 消费 `/api/expenses`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/budget` | 获取预算 |
| PUT | `/api/budget` | 更新预算 |
| GET | `/api/expenses` | 获取消费记录（支持 `year`, `month` 筛选） |
| POST | `/api/expenses` | 新增消费 |
| DELETE | `/api/expenses/{id}` | 删除消费 |

---

### 用户偏好 `/api/preferences`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/preferences` | 获取偏好 |
| PUT | `/api/preferences` | 更新偏好 |
| POST | `/api/preferences/favorite/{foodId}` | 切换收藏 |

---

### 拼饭 `/api/social/groups`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/social/groups` | 获取活动列表（支持 `status` 筛选） |
| GET | `/api/social/groups/{id}` | 获取活动详情 |
| POST | `/api/social/groups` | 发起拼饭 |
| POST | `/api/social/groups/{id}/join` | 加入拼饭 |
| POST | `/api/social/groups/{id}/vote` | 投票 |
| GET | `/api/social/groups/{id}/bill` | 获取AA账单（满员后可用） |

**发起拼饭请求体**
```json
{
  "title": "今天去吃火锅！",
  "time": "12:00",
  "location": "楼下美食广场",
  "maxPeople": 4,
  "candidates": ["海底捞", "小龙坎", "呷哺呷哺"],
  "tags": ["火锅", "聚餐"]
}
```

---

### 数据统计 `/api/stats`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/api/stats/expense` | 消费趋势 | `year`, `month` |
| GET | `/api/stats/nutrition` | 营养摄入 | `year`, `month` |
| GET | `/api/stats/habit` | 用餐习惯 | - |

**消费趋势响应**
```json
{
  "byDay": { "01": 35.5, "02": 0, "03": 42.0, "...": "..." },
  "byMealType": { "breakfast": 120.5, "lunch": 300.0, "dinner": 200.0 },
  "total": 620.5,
  "avgPerDay": 20.68,
  "activeDays": 30
}
```

**用餐习惯响应**
```json
{
  "totalRecords": 89,
  "streak": 7,
  "topFoods": [
    { "name": "红烧肉", "count": 12 },
    { "name": "炒饭", "count": 9 }
  ],
  "mealTypeCount": { "lunch": 45, "dinner": 30, "breakfast": 14 }
}
```

---

### 成就 `/api/achievements`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/achievements` | 获取已解锁成就 |
| POST | `/api/achievements/check` | 触发成就检查 |

---

## WebSocket 实时通信

### 连接地址

```
ws://localhost:8080/ws/social?token=<JWT>&groupId=<群组ID>
```

### 消息类型

| type | 触发时机 | 携带数据 |
|------|----------|---------|
| `JOIN` | 有用户加入群组 | `sender`（昵称）, `content` |
| `VOTE` | 有用户投票 | `sender`, `candidateName`, `data`（最新候选列表） |
| `CHAT` | 群成员发送消息 | `sender`, `content` |
| `FULL` | 群组人数已满 | `content`（提示文字） |

### 消息格式

```json
{
  "type": "CHAT",
  "groupId": 1,
  "sender": "张三",
  "content": "今天去吃火锅吧！",
  "data": null,
  "timestamp": "2026-04-13T12:00:00"
}
```

### 发送消息

客户端只支持发送聊天消息：

```json
{ "type": "CHAT", "content": "消息内容" }
```

### 自动重连

网络断开后 3 秒自动重连，前端通过 `wsConnected` 状态显示连接状态。

---

## 启动指南

### 环境要求

| 工具 | 版本 |
|------|------|
| Java | 17+ |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Node.js | 18.0+ |

### 启动步骤

**第一步：准备数据库**

```bash
mysql -u root -p
CREATE DATABASE IF NOT EXISTS fanda DEFAULT CHARACTER SET utf8mb4;
exit;

mysql -u root -p fanda < fanda-server/src/main/resources/db/schema.sql
```

**第二步：配置数据库密码**

修改 `fanda-server/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:你的MySQL密码}
```

**第三步：启动后端**

```bash
cd fanda-server
mvn spring-boot:run
# 启动成功: Started FandaApplication in X.X seconds
```

**第四步：启动前端**

```bash
cd fanda
npm install
npm run dev:h5
# 浏览器打开 http://localhost:5173
```

### 验证后端

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","nickname":"测试"}'

# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

---

## 功能模块

### 已完成

- ✅ JWT 无状态认证（登录、注册、Token 刷新）
- ✅ 菜品管理（80+ 内置菜品 + 用户自定义）
- ✅ 用餐记录（三餐记录、日历视图）
- ✅ 预算管家（月预算、消费追踪、超支预警）
- ✅ 用户偏好（口味、忌口、收藏夹）
- ✅ 拼饭广场（发起、加入、投票）
- ✅ WebSocket 实时通信（入群通知、投票同步、群聊）
- ✅ 数据统计（消费趋势图、营养摄入、用餐习惯）
- ✅ 成就系统

### 规划中

- ⬜ 暗黑模式主题切换
- ⬜ 好友系统（关注/粉丝）
- ⬜ 微信授权登录
- ⬜ AI 智能推荐（Claude API）
- ⬜ 图片识别点餐
- ⬜ LBS 附近餐厅推荐（高德地图）
- ⬜ 消息推送通知

---

## 设计规范

### 色彩系统

| 变量 | 色值 | 用途 |
|------|------|------|
| `$fd-primary` | `#FF6B6B` | 主色（珊瑚红） |
| `$fd-secondary` | `#FFE66D` | 辅色（暖黄） |
| `$fd-accent` | `#4ECDC4` | 点缀色（薄荷绿） |
| `$fd-bg` | `#FFF5F5` | 背景色 |
| `$fd-success` | `#2ED573` | 成功 |
| `$fd-danger` | `#FF4757` | 危险 |

### 圆角规范

| 变量 | 大小 |
|------|------|
| `$fd-radius-sm` | 12rpx |
| `$fd-radius` | 24rpx |
| `$fd-radius-lg` | 36rpx |
| `$fd-radius-round` | 999rpx |

### 组件命名

所有自定义组件统一使用 `fd-` 前缀，如 `fd-nav-bar`、`fd-modal`。
