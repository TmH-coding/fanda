# 饭搭 (FanDa)

> 打工人智能饮食决策助手 — 告别每天"吃什么"的灵魂拷问

---

## 项目简介

**饭搭**是一款面向都市打工人的移动端饮食助手，集转盘推荐、饮食日历、预算管家、拼饭社交于一体。采用活泼可爱的视觉风格，让选餐变得轻松有趣。

### 核心功能

| 模块 | 功能 | 状态 |
|------|------|------|
| 转盘推荐 | 智能推荐、排除法、偏好过滤、一键记录 | ✅ 已完成 |
| 饮食日历 | 月历视图、三餐记录、营养标签 | ✅ 已完成 |
| 预算管家 | 预算设定、消费追踪、超支预警、每日建议 | ✅ 已完成 |
| 拼饭广场 | 发起拼饭、投票选餐厅、加入退出（模拟数据） | ✅ 已完成 |
| 个人中心 | 口味偏好、忌口设置、成就徽章、收藏夹 | ✅ 已完成 |

### 技术栈

- **框架**: uni-app (Vue 3 + Composition API)
- **状态管理**: Pinia
- **样式**: SCSS + CSS Variables
- **数据存储**: 本地存储 (uni.storage)
- **构建工具**: Vite
- **日期处理**: Day.js

---

## 快速开始

### 环境要求

| 工具 | 最低版本 | 说明 |
|------|---------|------|
| Node.js | >= 18.0 | 推荐使用 LTS 版本 |
| npm | >= 9.0 | 随 Node.js 安装 |
| 微信开发者工具 | 最新版 | 仅小程序开发需要 |

### 安装与启动

```bash
# 1. 进入项目目录
cd fanda

# 2. 安装依赖
npm install

# 3. 启动 H5 开发服务器（浏览器预览）
npm run dev:h5

# 4. 打开浏览器访问
# 默认地址: http://localhost:5173
# 建议使用 Chrome 开发者工具切换到手机模式（F12 → 点击手机图标）
```

### 微信小程序开发

```bash
# 1. 编译小程序
npm run dev:mp-weixin

# 2. 打开微信开发者工具
# 3. 导入项目，选择编译产物目录: dist/dev/mp-weixin
# 4. 在 manifest.json 中填写你的小程序 appid
```

### 构建生产版本

```bash
# H5 生产构建
npm run build:h5
# 产物目录: dist/build/h5

# 微信小程序生产构建
npm run build:mp-weixin
# 产物目录: dist/build/mp-weixin
```

---

## 项目结构

```
fanda/
├── index.html                    # H5 入口 HTML
├── package.json                  # 依赖与脚本
├── vite.config.js                # Vite 构建配置
│
├── docs/                         # 项目文档
│   ├── PRD.md                    # 产品需求文档
│   └── ARCHITECTURE.md           # 架构设计文档
│
└── src/
    ├── main.js                   # 应用入口，注册 Pinia
    ├── App.vue                   # 根组件
    ├── pages.json                # 页面路由 & TabBar 配置
    ├── manifest.json             # 应用配置（appid、平台参数）
    ├── uni.scss                  # uni-app 全局 SCSS 变量
    │
    ├── pages/                    # 页面
    │   ├── index/                #   首页 - 转盘推荐
    │   ├── calendar/             #   饮食日历
    │   ├── budget/               #   预算管家
    │   ├── social/               #   拼饭广场
    │   ├── social-detail/        #   拼饭详情
    │   └── profile/              #   个人中心
    │
    ├── components/               # 组件（fd- 前缀）
    │   ├── common/               #   通用: fd-nav-bar, fd-modal, fd-empty
    │   └── recommend/            #   推荐: fd-wheel, fd-food-result, fd-exclude-tags
    │
    ├── composables/              # 组合式函数（业务逻辑复用）
    │   └── useRecommend.js       #   推荐算法 & 转盘逻辑
    │
    ├── stores/                   # Pinia 状态管理
    │   ├── index.js              #   统一导出
    │   └── modules/
    │       ├── food.js           #   菜品数据
    │       ├── record.js         #   用餐记录
    │       ├── budget.js         #   预算管理
    │       ├── preference.js     #   用户偏好
    │       ├── social.js         #   拼饭社交
    │       └── achievement.js    #   成就系统
    │
    ├── services/                 # 数据服务层（可切换 local/remote）
    │   ├── factory.js            #   服务工厂
    │   ├── local/                #   本地存储实现
    │   └── remote/               #   远程 API 实现（预留）
    │
    ├── data/                     # 静态数据
    │   ├── foods.js              #   100+ 内置菜品数据库
    │   ├── achievements.js       #   成就定义
    │   └── mockSocial.js         #   拼饭模拟数据
    │
    ├── config/                   # 配置
    │   ├── index.js              #   主配置 (dataMode: local/remote)
    │   ├── theme.js              #   主题 & 常量
    │   └── constants.js          #   分类、营养、成就常量
    │
    ├── utils/                    # 工具函数
    │   ├── storage.js            #   本地存储封装
    │   ├── date.js               #   日期工具 (dayjs)
    │   ├── format.js             #   格式化 & ID生成
    │   └── platform.js           #   平台适配
    │
    ├── styles/                   # 全局样式
    │   ├── variables.scss        #   颜色、尺寸、间距变量
    │   ├── mixins.scss           #   常用 mixin
    │   ├── animation.scss        #   动画定义
    │   └── common.scss           #   公共样式
    │
    ├── static/                   # 静态资源
    │   └── tabbar/               #   TabBar 图标
    │
    └── plugins/                  # 插件机制（预留）
        └── index.js
```

---

## 开发指南

### 新增页面

```bash
# 1. 创建页面文件
mkdir src/pages/new-page
touch src/pages/new-page/new-page.vue

# 2. 在 src/pages.json 的 pages 数组中注册
{
  "path": "pages/new-page/new-page",
  "style": { "navigationBarTitleText": "新页面", "navigationStyle": "custom" }
}
```

### 新增功能模块

按以下顺序添加文件，保持架构一致：

```
1. src/data/xxx.js                    — 静态数据（如果需要）
2. src/services/local/xxxService.js   — 数据服务
3. src/stores/modules/xxx.js          — Pinia Store
4. src/composables/useXxx.js          — 业务逻辑
5. src/components/xxx/fd-xxx.vue      — UI 组件
6. src/pages/xxx/xxx.vue              — 页面
```

### 切换数据源（本地 → 远程）

```javascript
// src/config/index.js
export default {
  dataMode: 'remote',  // 将 'local' 改为 'remote'
}

// 然后在 src/services/remote/ 下实现同名 service 即可
// 上层代码（store、composable、page）无需任何修改
```

### 组件命名规范

- 所有自定义组件使用 `fd-` 前缀
- 组件文件名与组件名一致: `fd-wheel.vue` → `<fd-wheel />`
- 按领域分组: `components/recommend/`、`components/budget/`

### 样式规范

```scss
// 使用全局变量（无需 import，uni.scss 自动注入）
.my-class {
  color: $fd-primary;
  background: $fd-card-bg;
  border-radius: $fd-radius;
  box-shadow: $fd-shadow;
}

// 使用 mixin
.my-card {
  @include fd-card;       // 卡片样式
  @include fd-flex-center; // 居中
}
```

### 设计规范速查

| 属性 | 值 |
|------|----|
| 主色 | `#FF6B6B` (珊瑚红) |
| 辅色 | `#FFE66D` (暖黄) |
| 点缀色 | `#4ECDC4` (薄荷绿) |
| 背景色 | `#FFF5F5` (浅粉) |
| 圆角 | 小 `12rpx` / 中 `24rpx` / 大 `36rpx` / 圆 `999rpx` |
| 字号 | 辅助 `22rpx` / 正文 `28rpx` / 中标题 `32rpx` / 大标题 `36rpx` |

---

## 可用脚本

| 命令 | 说明 |
|------|------|
| `npm run dev:h5` | 启动 H5 开发服务器（浏览器预览） |
| `npm run build:h5` | 构建 H5 生产版本 |
| `npm run dev:mp-weixin` | 启动微信小程序开发编译 |
| `npm run build:mp-weixin` | 构建微信小程序生产版本 |

---

## 数据说明

### 本地存储 Key

| Key | 内容 | 位置 |
|-----|------|------|
| `fd_foods_custom` | 用户自定义菜品 | foodService |
| `fd_records` | 用餐记录 | recordService |
| `fd_budget` | 预算设置 | budgetService |
| `fd_expenses` | 消费记录 | budgetService |
| `fd_preference` | 用户偏好(口味/忌口/收藏) | preferenceStore |
| `fd_social_groups` | 拼饭活动 | socialService |
| `fd_achievements_unlocked` | 已解锁成就 | achievementStore |

### 内置菜品数据

- 共 **80+ 菜品**，覆盖 8 大分类
- 中式(米饭/炒菜/粤菜)、面食、西式、日韩、快餐、轻食、小吃、火锅
- 每个菜品包含：名称、分类、价格区间、口味标签、营养类型、过敏原、适用餐次

---

## 后续规划

- [ ] 替换 TabBar 占位图标为正式设计图标
- [ ] 接入后端 API（实现 `services/remote/`）
- [ ] 微信授权登录 + 数据云同步
- [ ] LBS 定位 + 附近真实餐厅推荐
- [ ] 拼饭实时通信（WebSocket）
- [ ] 暗黑模式主题切换
- [ ] 消费趋势图表（接入 uCharts）

---

## 相关文档

- [产品需求文档 (PRD)](docs/PRD.md)
- [架构设计文档](docs/ARCHITECTURE.md)
