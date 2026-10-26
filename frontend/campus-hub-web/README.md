# CampusHub Web

CampusHub 的统一 Vue 3 SPA 前端。视觉语言参考了 `zgMall-web` 与 `campus-life-web` 的清爽校园感，但页面、组件和交互按 CampusHub 重新组织，没有把两个旧前端直接拼接。

## 当前范围

- 统一 AppShell：品牌、主导航、购物车徽标、登录态菜单、页脚
- 首页聚合：二手好物、近期活动、社区问题，并可直接进入对应业务
- 校园二手：关键词搜索、分类筛选、价格排序、详情弹窗、加入购物车
- 校园活动：类型筛选、报名进度展示、活动详情入口占位
- 问答社区：最新/最热切换、话题搜索、热门话题侧栏、提问入口占位
- 购物车：真实 Pinia 状态、数量修改、全选、删除、合计、结算入口占位
- 登录态：体验账号登录、本地 session 持久化、顶部用户菜单、退出登录
- 响应式布局：桌面、平板、移动端统一结构

## API 层与数据模型

`src/api/http.js` 提供 Axios 客户端、Token 注入和统一响应解包；`src/api/campusHubApi.js` 按业务提供 API 方法。接口未接入后端时，所有请求会自动回退到本地 mock 数据，保证前端可以独立运行：

```text
getHomeSummary()
listSecondhand(params)
getSecondhandItem(id)
listActivities()
listQuestions()
login(payload)
```

`src/models/index.js` 放置 User、SecondhandItem、Activity、Question、CartLine 的字段契约占位；`src/stores/app.js` 管理登录态与购物车状态。

## 技术栈

Vue 3、Vite、Vue Router、Pinia、Element Plus、Axios。

## 本地运行

```bash
npm install
npm run dev
```

生产构建：

```bash
npm run build
```

默认开发端口：`5174`。

## 目录

```text
src/
├── api/          Axios 客户端与业务 API 层
├── models/       前端数据模型/DTO 占位
├── stores/       Pinia 全局状态
├── components/   AppShell、业务卡片、区块标题等复用组件
├── views/        首页、二手、活动、社区、购物车、登录
├── router/       SPA 路由
├── styles/       全局视觉与响应式样式
├── App.vue
└── main.js
```

本次只修改 `frontend/campus-hub-web`，没有修改 `backend` 或 `database`。
