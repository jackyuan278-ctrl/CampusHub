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

## 已知限制与设计取舍

这份清单是有意留下的缺口，不是 bug，交接时请按「当前不支持」理解。

**交易链路**
- 商品状态只有「在售 / 已售」，没有中间态。`ItemStatus.RESERVED(2)` 是预留值，全链路没有代码写入，实际流程不会出现。
- 结算时不做支付密码、不接第三方支付，只有余额与模拟扫码两条路。
- 关单与支付的竞态由 `PayReconcileTask` 每分钟对账补偿：发现「支付单已付 + 订单已关闭」就把钱退回余额。退款前用 `PAID -> REFUNDED` 的条件更新抢占，保证只退一次；若退款调用本身失败，只记 error 日志等人工处理。
- 销量字段未随支付累加，热榜权重按浏览量 + 点赞算。

**社区**
- 回答与评论列表一次返回全部（各限 200 条），没有分页。问题多了以后需要改成游标分页。
- 点赞的正确性以 `tb_like_record` 唯一键为准，Redis 里的 likeSet 只用于渲染红心；Redis 写失败时红心可能不亮，但不影响点赞本身。
- 关注列表在用户服务不可用时保留条目、昵称留空，不会整页消失。

**已知技术债**
- `PageQuery` 在 secondhand / community / order 各有一份副本，未收敛到 campus-api。
- 月度签到一次请求会读 31 次 `GETBIT`，用户量上来后应改成 `BITFIELD` 一次取回。
- outbox 的定时扫描是单机 Spring Task，多实例部署会重复投递（消费端幂等，可容忍），严格只发一次需要分布式调度。
- RabbitMQ 未开启 publisher confirm，outbox 标 `published=1` 只代表消息已交给客户端，不代表 broker 已落盘。
- 服务端口默认监听 `0.0.0.0`，下游信任 `user-info` 请求头。**部署时必须只对外暴露网关 8080**，其余端口仅内网可达。
