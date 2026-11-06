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

## 数据库边界

每个业务服务使用独立数据库：

```text
campushub_user
campushub_activity
campushub_community
campushub_secondhand
campushub_cart
campushub_order
campushub_payment
```

服务之间通过业务 ID、HTTP/Feign 或消息进行协作，不跨库 JOIN，也不共享数据库表。
