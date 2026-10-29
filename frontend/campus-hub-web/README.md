# CampusHub Web

CampusHub 的统一 Vue 3 SPA 前端。视觉语言沿用项目既有的「薄荷绿 + 大圆角卡片 + eyebrow 小标题」体系，页面按业务域组织，API 层与后端 17 个 Controller 一一对应。

## 页面结构

| 路由 | 页面 | 说明 | 需要登录 |
| --- | --- | --- | --- |
| `/home` | 首页 | 热卖好物、近期活动、社区热议、话题广场聚合 | 否 |
| `/secondhand` | 校园二手 | 关键词搜索、分类筛选、综合/价格排序、游标滚动加载 | 否 |
| `/secondhand/:id` | 物品详情 | 商品图、价格、成色、交易地点、加购/立即购买、同类推荐 | 否 |
| `/activities` | 校园活动 | 状态筛选、ES 关键词检索、分页、发布活动 | 否（发布需登录） |
| `/activities/:id` | 活动详情 | 时间线、名额进度、报名/候补/取消、发布者取消活动 | 否 |
| `/community` | 问答社区 | 最新/最热、话题标签筛选、分页、提问 | 否（提问需登录） |
| `/community/:id` | 问题详情 | 点赞、关注作者、回答、回答点赞、评论 | 否（互动需登录） |
| `/cart` | 购物车 | 服务端购物车、勾选、改数量、移除、清空 | 是 |
| `/checkout` | 确认订单 | 收货地址选择/新增、商品明细、备注、提交订单 | 是 |
| `/orders` | 我的订单 | 状态筛选、订单卡片、去支付 | 是 |
| `/orders/:id` | 订单详情 | 明细、收货信息、余额/模拟扫码支付 | 是 |
| `/profile` | 个人中心 | 资料与密码、我的报名、签到、地址、关注 | 是 |
| `/login` | 登录 / 注册 | 登录与注册双 Tab，支持 `?redirect=` 回跳 | 否 |

## 接口覆盖

金额统一使用后端返回的「分」，在展示层用 `utils/format.js` 转换为元；Long 型 ID 按后端约定一律当字符串处理，避免精度丢失。

### campus-user-service（`/api/users`）

| 接口 | 使用位置 |
| --- | --- |
| `POST /users/login`、`POST /users/register` | 登录页 |
| `GET /users/me`、`PUT /users/me` | 全局登录态、个人中心资料 |
| `PUT /users/password` | 个人中心修改密码 |
| `GET /users/addresses`、`POST`、`PUT /{id}`、`DELETE /{id}`、`PUT /{id}/default` | 个人中心地址簿、结算页地址选择 |

`PUT /users/{id}/balance/deduct` 为支付服务的 Feign 内部接口，浏览器不直接调用。

### campus-secondhand-service

| 接口 | 使用位置 |
| --- | --- |
| `GET /items` | 二手列表（前 5 页 page 分页）、详情页同类推荐 |
| `GET /items/{id}` | 物品详情 |
| `GET /items/hot` | 首页热卖 |
| `GET /items/categories` | 二手分类筛选 |

`PUT /items/stock/deduct|restore` 为交易服务的内部接口，前端不调用。

### campus-cart-service（`/api/cart`）

`GET /cart`（购物车页 + 顶部徽标）、`POST /cart`（加购）、`PUT /cart/{itemId}`（改数量）、`DELETE /cart/{itemId}`（移除）、`DELETE /cart`（清空）。

### campus-order-service / campus-payment-service

`POST /orders`（提交订单）、`GET /orders`（订单列表）、`GET /orders/{id}`（订单详情）、`POST /pay/orders/{orderId}/pay`（`payType=balance|mock`）。

### campus-activity-service

| 接口 | 使用位置 |
| --- | --- |
| `GET /activities` | 活动列表（状态筛选 + 分页）、首页近期活动 |
| `GET /activities/search` | 活动关键词检索（ES + IK） |
| `GET /activities/{id}` | 活动详情（含我的报名状态） |
| `POST /activities` | 发布活动弹窗 |
| `PUT /activities/{id}/cancel` | 发布者取消活动（详情页） |
| `POST /signups`、`POST /signups/waitlist`、`DELETE /signups/{activityId}` | 详情页报名 / 候补 / 取消 |
| `GET /signups/mine` | 个人中心「我的报名」 |
| `POST /checkins`、`GET /checkins/mine` | 个人中心签到卡片与本周签到条 |

> 网关原本只放行了 `/api/activities/**`，报名与签到属于同服务的独立路径，因此在 `campus-gateway` 中补充了 `/api/signups/**`、`/api/checkins/**` 两条路由（StripPrefix=1）。

### campus-community-service（网关前缀 `/api/community`，StripPrefix=2）

| 接口 | 使用位置 |
| --- | --- |
| `GET /questions` | 社区列表（keyword / tagId / sort=hot\|new + 分页） |
| `GET /questions/hot` | 首页社区热议 |
| `GET /questions/{id}` | 问题详情 |
| `POST /questions` | 提问弹窗 |
| `POST /questions/{id}/like`、`DELETE` | 问题点赞 / 取消 |
| `GET /questions/{questionId}/answers`、`POST` | 回答列表与发布回答 |
| `POST /answers/{id}/like`、`DELETE` | 回答点赞 / 取消 |
| `GET /answers/{answerId}/comments`、`POST` | 回答评论展开与发表 |
| `GET /tags` | 社区标签筛选、首页话题广场、提问标签选择 |
| `POST /follows/{userId}`、`DELETE`、`GET /follows/mine` | 关注作者、个人中心关注列表 |

## API 层约定

- `src/api/http.js`：Axios 实例、Token 注入、`Result{code,msg,data}` 解包、401 统一跳登录、错误提示（页面可传 `{ silent: true }` 自行渲染错误态）。
- 每个后端服务一个模块：`user.js` / `item.js` / `cart.js` / `order.js` / `activity.js` / `community.js`。
- `src/stores/auth.js` 管理 token 与 `/users/me` 缓存；`src/stores/cart.js` 以服务端购物车为唯一数据源。
- `src/composables/useRequireLogin.js`、`useAddToCart.js` 收敛「未登录 → 带去回跳地址的登录页」逻辑。

### 演示数据（可选）

后端未启动时，页面会在请求失败后回落到 `src/api/mock.js`，并在顶部显示橙色「演示数据」横幅。规则：

- 仅开发模式（`import.meta.env.DEV`）默认开启，生产构建默认关闭；
- 只拦截「连不上后端」的错误，真实业务错误（JSON `Result`）照常抛给页面；
- 数据结构与后端 VO 完全一致，接上后端不需要改页面代码；
- 需要关闭或强制开启时设置环境变量 `VITE_API_MOCK=off|on`。

## 本地运行

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # 产物在 dist/
```

开发服务器把 `/api` 代理到 `http://localhost:8080`（campus-gateway）。

## 目录

```text
src/
├── api/            http 客户端、按服务拆分的接口模块、演示数据
├── composables/    登录校验、加购等可复用逻辑
├── components/     导航、页脚、卡片、状态面板、分页、地址弹窗等
├── stores/         auth（登录态）、cart（服务端购物车）
├── utils/          金额/时间/状态映射、本地发布记录
├── views/          13 个业务页面
├── router/         路由表与登录守卫
└── styles/         index.css（设计变量 + 扩展样式 + 响应式）
```
