# CampusHub backend

CampusHub 后端第一版，采用 Maven 多模块 + Spring Boot/Spring Cloud 微服务结构。

## 模块

```text
campus-gateway
campus-user-service
campus-secondhand-service
campus-cart-service
campus-order-service
campus-payment-service
campus-activity-service
campus-community-service
campus-api
campus-common
```

每个业务服务都有独立的启动类、端口和数据库配置；业务服务之间不直接访问其他服务的数据库。

## 配置

各服务的配置位于：

```text
<service>/src/main/resources/application.yml
<service>/src/main/resources/application-dev.yml
```

启动前检查 MySQL、Redis、Nacos、Elasticsearch 和 RabbitMQ 的连接配置。当前项目不包含 AI 模块。
