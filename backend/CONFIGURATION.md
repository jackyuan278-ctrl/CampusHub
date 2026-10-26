# 配置说明

每个 CampusHub 服务维护自己的 Spring 配置和数据库连接：

- 用户服务 -> `campushub_user`
- 活动服务 -> `campushub_activity`
- 社区服务 -> `campushub_community`
- 二手服务 -> `campushub_secondhand`
- 购物车服务 -> `campushub_cart`
- 订单服务 -> `campushub_order`
- 支付服务 -> `campushub_payment`

启动前检查本机的 MySQL、Redis、Nacos、Elasticsearch 和 RabbitMQ。环境变量可在各服务的 `application-dev.yml` 中覆盖。

当前不包含管理员、通知和 AI 扩展业务；`zg-ai` 与 Spring AI 不迁移。
