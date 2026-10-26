# CampusHub

面向校园场景的综合交易与生活服务平台。第一版以校园二手交易为核心，同时保留校园活动和问答社区业务，整体采用 Spring Cloud 微服务架构。

## 目录

- `backend/`：Java 微服务后端
- `frontend/campus-hub-web/`：统一 Vue 单页前端
- `database/`：按服务隔离的 MySQL 初始化脚本

## 后端服务

- `campus-gateway`：统一网关，端口 8080
- `campus-user-service`：用户与地址，端口 8081
- `campus-secondhand-service`：校园二手物品与分类，端口 8082
- `campus-cart-service`：购物车，端口 8083
- `campus-order-service`：交易订单，端口 8084
- `campus-payment-service`：支付与支付事件，端口 8085
- `campus-activity-service`：活动、报名、签到，端口 8090
- `campus-community-service`：问答、回答、评论、点赞、关注、标签，端口 8091
- `campus-api`：服务间接口和 DTO
- `campus-common`：公共响应、异常、认证和基础配置

`zg-ai` / Spring AI 不在当前项目中。后续智能能力计划独立使用 Python Agent 接入。

## 数据库边界

每个业务服务使用独立数据库：

```text
campus-user-service       -> campushub_user
campus-activity-service   -> campushub_activity
campus-community-service  -> campushub_community
campus-secondhand-service -> campushub_secondhand
campus-cart-service       -> campushub_cart
campus-order-service      -> campushub_order
campus-payment-service    -> campushub_payment
```

服务之间通过业务 ID、HTTP/Feign 或消息进行协作，不跨库 JOIN，也不共享数据库表。

## 本地运行

后端 Maven 工程根目录是：

```text
CampusHub/backend/pom.xml
```

验证 Maven 模块：

```powershell
cd C:\Users\Tom\Desktop\workdemo\CampusHub\backend
mvn validate
```

完整编译需要本机 Maven 能访问所需插件和依赖；如果默认仓库不可写，可使用项目内的 `backend/work/maven-settings.xml` 指向可写仓库。

前端：

```powershell
cd C:\Users\Tom\Desktop\workdemo\CampusHub\frontend\campus-hub-web
npm install
npm run dev
```
