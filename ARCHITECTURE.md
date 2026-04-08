# 饭搭 (FanDa) 项目架构文档

## 项目概述

**饭搭**是一个全栈应用，包含前端（uni-app）和后端（Spring Boot）两部分。

- **前端**: uni-app (Vue 3) - 支持 H5 和微信小程序
- **后端**: Spring Boot 3.2.5 + MySQL + JWT 认证
- **数据库**: MySQL 8.0+

---

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                     用户设备                                  │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │   H5 浏览器      │         │  微信小程序      │          │
│  └────────┬─────────┘         └────────┬─────────┘          │
└───────────┼──────────────────────────────┼──────────────────┘
            │                              │
            └──────────────┬───────────────┘
                           │ HTTP/HTTPS
            ┌──────────────▼───────────────┐
            │   Spring Boot 后端服务       │
            │   (8080 端口)                │
            │  ┌────────────────────────┐  │
            │  │  REST API 层           │  │
            │  │  (Controller)          │  │
            │  └────────────┬───────────┘  │
            │  ┌────────────▼───────────┐  │
            │  │  业务逻辑层            │  │
            │  │  (Service)             │  │
            │  └────────────┬───────────┘  │
            │  ┌────────────▼───────────┐  │
            │  │  数据访问层            │  │
            │  │  (Repository/JPA)      │  │
            │  └────────────┬───────────┘  │
            └───────────────┼──────────────┘
                            │ JDBC
            ┌───────────────▼──────────────┐
            │   MySQL 数据库               │
            │   (localhost:3306)           │
            └──────────────────────────────┘
```

---

## 后端架构详解

### 目录结构

```
fanda-server/
├── pom.xml                          # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/com/fanda/
│   │   │   ├── FandaServerApplication.java    # 启动类
│   │   │   ├── config/                        # 配置类
│   │   │   │   ├── CorsConfig.java           # CORS 跨域配置
│   │   │   │   └── SecurityConfig.java       # Spring Security 配置
│   │   │   ├── controller/                    # REST API 控制层 (7个)
│   │   │   │   ├── AuthController.java       # 认证 (登录/注册/刷新token)
│   │   │   │   ├── UserController.java       # 用户信息
│   │   │   │   ├── FoodController.java       # 菜品管理
│   │   │   │   ├── RecordController.java     # 用餐记录
│   │   │   │   ├── BudgetController.java     # 预算管理
│   │   │   │   ├── ExpenseController.java    # 消费记录
│   │   │   │   ├── PreferenceController.java # 用户偏好
│   │   │   │   ├── SocialController.java     # 拼饭社交
│   │   │   │   └── AchievementController.java# 成就系统
│   │   │   ├── service/                      # 业务逻辑层
│   │   │   │   ├── impl/                     # 实现类
│   │   │   │   └── *.java                    # 接口定义
│   │   │   ├── repository/                   # 数据访问层 (JPA)
│   │   │   │   └── *Repository.java          # 数据库操作接口
│   │   │   ├── entity/                       # 数据模型 (JPA Entity)
│   │   │   │   ├── User.java
│   │   │   │   ├── FoodItem.java
│   │   │   │   ├── MealRecord.java
│   │   │   │   ├── Budget.java
│   │   │   │   ├── Expense.java
│   │   │   │   ├── UserPreference.java
│   │   │   │   ├── SocialGroup.java
│   │   │   │   └── AchievementUnlock.java
│   │   │   ├── dto/                          # 数据传输对象
│   │   │   │   ├── request/                  # 请求 DTO
│   │   │   │   └── response/                 # 响应 DTO
│   │   │   ├── security/                     # 安全相关
│   │   │   │   ├── JwtTokenProvider.java     # JWT 工具类
│   │   │   │   ├── UserDetailsServiceImpl.java# 用户详情服务
│   │   │   │   └── JwtAuthenticationFilter.java# JWT 过滤器
│   │   │   ├── exception/                    # 异常处理
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── BusinessException.java
│   │   │   └── init/                         # 初始化
│   │   │       └── DataInitializer.java      # 初始数据加载
│   │   └── resources/
│   │       ├── application.yml               # 应用配置
│   │       └── db/
│   │           └── schema.sql                # 数据库初始化脚本
│   └── test/                                 # 测试代码
│       └── java/com/fanda/
└── target/                                   # 编译输出目录
```

### 核心模块说明

#### 1. 认证模块 (Authentication)
- **JWT Token**: 无状态认证
- **刷新机制**: Access Token (2小时) + Refresh Token (7天)
- **密码加密**: Spring Security BCrypt
- **端点**:
  - `POST /api/auth/register` - 注册
  - `POST /api/auth/login` - 登录
  - `POST /api/auth/refresh` - 刷新 Token

#### 2. 用户模块 (User)
- 用户基本信息管理
- 头像、昵称等个人资料
- 端点: `GET/PUT /api/users/{id}`

#### 3. 菜品模块 (Food)
- 系统菜品库 (80+ 内置菜品)
- 用户自定义菜品
- 菜品标签、营养、过敏原、适用餐次
- 端点: `GET /api/foods`, `POST /api/foods`

#### 4. 用餐记录模块 (Record)
- 记录三餐饮食
- 关联菜品、成本、营养信息
- 按日期查询
- 端点: `GET/POST /api/records`

#### 5. 预算管理模块 (Budget)
- 设定每日/每月预算
- 消费追踪
- 超支预警
- 端点: `GET/PUT /api/budgets`, `GET/POST /api/expenses`

#### 6. 用户偏好模块 (Preference)
- 口味偏好 (辛辣、清淡等)
- 忌口设置 (过敏原、不喜欢的食物)
- 收藏夹
- 端点: `GET/PUT /api/preferences`

#### 7. 社交模块 (Social)
- 拼饭活动管理
- 投票选餐厅
- 成员管理
- 端点: `GET/POST /api/social/groups`

#### 8. 成就系统 (Achievement)
- 成就解锁记录
- 成就检查逻辑
- 端点: `GET /api/achievements`

---

## 数据库设计

### 核心表结构

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `fd_user` | 用户表 | id, username, password, nickname |
| `fd_food_item` | 菜品表 | id, name, category, price_min, price_max |
| `fd_food_tag` | 菜品标签 | food_id, tag |
| `fd_food_nutrition` | 菜品营养 | food_id, nutrition_type |
| `fd_food_allergen` | 菜品过敏原 | food_id, allergen |
| `fd_meal_record` | 用餐记录 | user_id, record_date, meal_type, food_id |
| `fd_budget` | 预算设置 | user_id, daily_limit, monthly_limit |
| `fd_expense` | 消费记录 | user_id, expense_date, amount |
| `fd_user_preference` | 用户偏好 | user_id, taste_tags, forbidden_foods |
| `fd_social_group` | 拼饭活动 | id, creator_id, title, status |
| `fd_achievement_unlock` | 成就解锁 | user_id, achievement_id, unlocked_at |

---

## 前端架构详解

### 目录结构

```
fanda/
├── src/
│   ├── main.js                      # 应用入口
│   ├── App.vue                      # 根组件
│   ├── pages.json                   # 路由 & TabBar 配置
│   ├── manifest.json                # 应用配置
│   ├── uni.scss                     # 全局 SCSS 变量
│   │
│   ├── pages/                       # 页面 (6个)
│   │   ├── index/                   # 首页 - 转盘推荐
│   │   ├── calendar/                # 饮食日历
│   │   ├── budget/                  # 预算管家
│   │   ├── social/                  # 拼饭广场
│   │   ├── social-detail/           # 拼饭详情
│   │   └── profile/                 # 个人中心
│   │
│   ├── components/                  # 组件库
│   │   ├── common/                  # 通用组件
│   │   │   ├── fd-nav-bar.vue      # 导航栏
│   │   │   ├── fd-modal.vue        # 弹窗
│   │   │   └── fd-empty.vue        # 空状态
│   │   └── recommend/               # 推荐相关
│   │       ├── fd-wheel.vue        # 转盘组件
│   │       ├── fd-food-result.vue  # 推荐结果
│   │       └── fd-exclude-tags.vue # 排除标签
│   │
│   ├── stores/                      # Pinia 状态管理
│   │   ├── index.js                # 统一导出
│   │   └── modules/
│   │       ├── food.js             # 菜品 Store
│   │       ├── record.js           # 记录 Store
│   │       ├── budget.js           # 预算 Store
│   │       ├── preference.js       # 偏好 Store
│   │       ├── social.js           # 社交 Store
│   │       └── achievement.js      # 成就 Store
│   │
│   ├── services/                    # 数据服务层
│   │   ├── factory.js              # 服务工厂
│   │   ├── local/                  # 本地存储实现
│   │   └── remote/                 # 远程 API 实现
│   │
│   ├── composables/                 # 组合式函数
│   │   └── useRecommend.js         # 推荐算法
│   │
│   ├── utils/                       # 工具函数
│   │   ├── storage.js              # 存储工具
│   │   ├── date.js                 # 日期工具
│   │   ├── format.js               # 格式化工具
│   │   └── platform.js             # 平台适配
│   │
│   ├── styles/                      # 全局样式
│   │   ├── variables.scss          # 变量定义
│   │   ├── mixins.scss             # Mixin 定义
│   │   ├── animation.scss          # 动画定义
│   │   └── common.scss             # 公共样式
│   │
│   ├── data/                        # 静态数据
│   │   ├── foods.js                # 菜品数据库
│   │   ├── achievements.js         # 成就定义
│   │   └── mockSocial.js           # 模拟数据
│   │
│   ├── config/                      # 配置
│   │   ├── index.js                # 主配置
│   │   ├── theme.js                # 主题配置
│   │   └── constants.js            # 常量定义
│   │
│   └── static/                      # 静态资源
│       └── tabbar/                 # TabBar 图标
│
├── index.html                       # H5 入口 HTML
├── package.json                     # 依赖配置
└── vite.config.js                   # Vite 构建配置
```

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.38 | 前端框架 |
| uni-app | 3.0.0 | 跨平台框架 |
| Pinia | 2.1.7 | 状态管理 |
| Day.js | 1.11.12 | 日期处理 |
| Vite | 5.2.8 | 构建工具 |
| SCSS | 1.77.8 | 样式预处理 |

---

## 技术栈总览

### 后端
- **框架**: Spring Boot 3.2.5
- **数据库**: MySQL 8.0+
- **ORM**: Spring Data JPA + Hibernate
- **认证**: JWT + Spring Security
- **构建**: Maven
- **Java 版本**: 17

### 前端
- **框架**: Vue 3 + uni-app
- **状态管理**: Pinia
- **样式**: SCSS + CSS Variables
- **构建**: Vite
- **Node 版本**: >= 18.0

---

## 数据流向

### 用户登录流程
```
1. 用户输入用户名/密码
   ↓
2. 前端 POST /api/auth/login
   ↓
3. 后端验证密码 (BCrypt)
   ↓
4. 生成 JWT Token (Access + Refresh)
   ↓
5. 前端存储 Token (localStorage)
   ↓
6. 后续请求在 Header 中携带 Token
   ↓
7. JwtAuthenticationFilter 验证 Token
```

### 推荐算法流程
```
1. 用户点击转盘
   ↓
2. 获取用户偏好 (口味、忌口、收藏)
   ↓
3. 从菜品库筛选符合条件的菜品
   ↓
4. 应用排除法 (用户排除的菜品)
   ↓
5. 随机选择推荐菜品
   ↓
6. 显示推荐结果 + 记录
```

---

## 部署架构

### 开发环境
```
本地开发机
├── Node.js 18+ (前端)
├── Java 17 (后端)
├── MySQL 8.0 (本地数据库)
└── IDE (VS Code / IntelliJ)
```

### 生产环境 (规划)
```
云服务器
├── Nginx (反向代理 + 静态资源)
├── Spring Boot (Docker 容器)
├── MySQL (云数据库)
└── CDN (静态资源加速)
```

---

## 关键设计决策

1. **JWT 无状态认证**: 便于分布式部署，无需 Session 存储
2. **分层架构**: Controller → Service → Repository，职责清晰
3. **DTO 模式**: 请求/响应与实体分离，API 稳定性强
4. **Pinia 状态管理**: 集中管理前端状态，便于调试
5. **本地存储优先**: 前端优先使用本地存储，后续可切换到远程 API
6. **菜品标签化**: 灵活的标签系统支持多维度筛选

---

## 扩展点

1. **推荐算法**: 可接入 ML 模型优化推荐
2. **社交功能**: 可接入 WebSocket 实现实时通信
3. **支付集成**: 可接入微信支付/支付宝
4. **LBS 功能**: 可接入高德地图 API 推荐附近餐厅
5. **数据分析**: 可接入 ELK 或 Grafana 进行数据可视化
