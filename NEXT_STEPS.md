# CampusHub 下一步

已完成本轮配置审查：

- 网关支付路由 YAML 已修复；
- 活动、社区、订单的开发配置已改为 CampusHub 数据库名和环境变量占位；
- 根 `.gitignore` 已补充 Maven、IDE、开发配置和前端构建产物忽略项；
- Maven `validate` 已通过；
- 前端 `npm run build` 已通过；
- 已删除项目内临时的 `.m2`、`work`、`backend/work` 目录。

仍需后续处理：

1. 旧的 `backend/campus-life-service` 与 `backend/_legacy-campus-life` 已移除；活动、报名、签到已归入 `campus-activity-service`，问答、回答、评论、点赞、关注、标签已归入 `campus-community-service`。
2. 使用现有 Maven 本地仓库执行完整编译：

```powershell
cd C:\Users\Tom\Desktop\workdemo\CampusHub\backend
mvn -Dmaven.repo.local=E:\develop\tools\maven\apache-maven-3.9.16\mvn_repo -DskipTests compile
```

3. 检查各服务启动类、Feign/Nacos 路由和数据库连接，进行真实联调。
4. 前端已接入全部可访问接口（覆盖清单见 `frontend/campus-hub-web/README.md`）；后端未启动时仅开发模式会回落到演示数据，网关起来后自动走真实接口。

## 前端联调注意

- `campus-gateway` 新增了两条路由：`/api/signups/**`、`/api/checkins/**`（指向 `campus-activity-service`，StripPrefix=1）。此前只放行了 `/api/activities/**`，报名与签到在浏览器侧不可达。
- 后端目前缺少「发布二手商品」接口（`campus-secondhand-service` 只提供查询与库存接口），前端「发布闲置」按钮暂以提示代替。
- `ActivityVO` 未返回 `publisherId`，前端在发布成功后本地记录活动 ID，用于在详情页展示「发布者取消活动」入口。

Maven 安装目录：

```text
E:\develop\tools\maven\apache-maven-3.9.16
```

Maven 本地仓库：

```text
E:\develop\tools\maven\apache-maven-3.9.16\mvn_repo
```

GitHub 不需要 Codex 连接，也没有执行提交或推送。

## 2026-10-28 字段对齐与首次真实联调

### 数据库（已对线上库执行，表内原本无数据）

`database/08_align_trade_payment.sql`（幂等，可重复执行）补齐了 4 个「实体有、表里没有」的列：

| 表 | 新增列 |
| --- | --- |
| `campushub_order.tb_order` | `payment_type`、`consign_time` |
| `campushub_order.tb_order_detail` | `spec`、`update_time` |

同时把 `07_payment.sql` 里与代码相反的 `status` 注释改为「1待支付 2已支付 3处理中」，并删除重复且无人引用的 `payment/enums/PayStatus`。

### 代码

- `Order` 补 `orderNo` / `orderType` / `paymentNo`；`OrderDetail` 补 `sellerId`；`PayOrder`、`PayEventOutbox` 补 `paymentNo`；`ItemDTO` 补 `sellerId`。
- 新增 `campus-common` 的 `BizNoGenerator`：订单号 `O + yyyyMMddHHmmss + 6 位随机`，支付号 `P + ...`。
- 下单时生成 `orderNo`、显式写 `status=1` / `payment_type=0`，明细落 `seller_id`（复用商品快照，不再回查）。
- `PayCompletionService` 写 Outbox 时带上 `payment_no`，历史空值自动补生成。
- 网关 `AuthGlobalFilter`：**剥离 `Authorization: Bearer ` 前缀**（此前所有需登录接口必然 401）；公开接口移除客户端自带的 `user-info` 并在 token 合法时重新注入。
- `ItemServiceImpl.deductStock` 写库改为 `ItemStatus.SOLD.getValue()`，此前把枚举对象直接写进 tinyint 列，报 `Incorrect integer value: 'SOLD'`，导致**下单必失败**。
- 5 个服务的 `REDIS_PASSWORD` 默认值统一为 `123`（与 `config/campushub.env` 一致），此前只有活动服务配了默认值。

### 联调结果（经网关，真实中间件）

`注册 → 登录 → 下单 → 支付(mock)` 全链路通过：

- `tb_order`：`order_no=O20261028235756499820`，`status=2`（MQ 支付通知回写成功）
- `tb_order_detail`：`seller_id=1001`、商品快照完整
- `tb_pay_order`：`payment_no=P20261028235802501693`，`status=2`
- `tb_pay_event_outbox`：`published=1`（事件已发出）
- `tb_item`：`status=3`（预扣库存生效）

测试数据已全部回滚，`tb_order` / `tb_pay_order` 归零，商品恢复在售。

### 2026-10-29 全量实体/表比对后修复（脚本 `database/09_fix_checkin_balance.sql`）

把 21 个实体逐个与线上库比对，又发现两个阻塞级问题，已修并验证：

- `campushub_user.tb_balance_deduction` **整张表不存在**，但 `UserServiceImpl.deductBalance` 用它做幂等流水，
  建表脚本里也从未出现过。导致余额支付路径必然失败，且 `uk_biz_order_no` 这道「重试不重复扣款」的保证缺失。
  已按实体补建（自增主键 + `uk_biz_order_no` 唯一键）。
- `campushub_activity.tb_checkin.activity_id BIGINT NOT NULL` 无默认值，而 `Checkin` 实体和签到逻辑都没有活动维度
  （签到是「用户 × 自然日」的连续签到）。插入必报 `1364`，签到功能 100% 失败。
  已删列、删依赖它的两个索引，改加 `uk_user_date (user_id, checkin_date)`，`02_activity.sql` 同步。

验证方式：事务内模拟签到 + 重复签到 + 扣款流水 + 同单重复扣款，四个断言全过（重复均被唯一键拦下），已回滚。
最终 20 个实体与表字段 0 差异。

另删除 `Brand` / `BrandMapper` 死代码（`tb_brand` 表不存在，也没有任何业务引用；按项目场景不需要品牌维度）。

### 仍待处理

1. 订单侧未回写 `payment_no` / `payment_type`：支付成功消息目前只带 `orderId`，需要把 `paymentNo`、`payType` 一起放进消息体。
2. 支付查询接口缺失（`GET /pay/orders/{id}`），前端拿不到支付结果。
3. 支付对账任务、支付单超时关单仍未实现。
4. 二手服务缺少「发布商品」接口。

### 本地起服务（端口会与环境变量 `SERVER__PORT` 冲突时，用命令行参数覆盖）

```powershell
java -jar campus-gateway/target/campus-gateway-1.0.0.jar --server.port=8080
java -jar campus-user-service/target/campus-user-service-1.0.0.jar --server.port=8081
java -jar campus-secondhand-service/target/campus-secondhand-service-1.0.0.jar --server.port=8082
java -jar campus-order-service/target/campus-order-service-1.0.0.jar --server.port=8084
java -jar campus-payment-service/target/campus-payment-service-1.0.0.jar --server.port=8085
```


