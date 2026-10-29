# CampusHub 数据库设计

本目录是 CampusHub 第一版的微服务数据库设计。每个业务微服务拥有独立数据库，只维护自己的表；数据库脚本不修改 `backend/` 或 `frontend/`。

## 服务与数据库映射

| 微服务 | 数据库 | 所有表 |
|---|---|---|
| `campus-user-service` | `campushub_user` | `tb_user`、`tb_address` |
| `campus-activity-service` | `campushub_activity` | `tb_activity`、`tb_signup`、`tb_checkin` |
| `campus-community-service` | `campushub_community` | `tb_question`、`tb_tag`、`tb_question_tag`、`tb_answer`、`tb_comment`、`tb_like`、`tb_follow` |
| `campus-secondhand-service` | `campushub_secondhand` | `tb_category`、`tb_item` |
| `campus-cart-service` | `campushub_cart` | `tb_cart` |
| `campus-order-service` | `campushub_order` | `tb_order`、`tb_order_detail` |
| `campus-payment-service` | `campushub_payment` | `tb_pay_order`、`tb_pay_event_outbox` |

## 脚本执行顺序

```text
00_create_databases.sql
01_user.sql
02_activity.sql
03_community.sql
04_secondhand.sql
05_cart.sql
06_order.sql
07_payment.sql
```

每个脚本只使用自己的数据库，不依赖其他脚本中的表。先执行 `00_create_databases.sql`，再按编号执行其余脚本。

`05_trade.sql`、`06_pay.sql` 和 `06_payment.sql` 是旧文件名提示文件，不执行任何 SQL；实际脚本分别为 `06_order.sql` 和 `07_payment.sql`。

## 数据隔离规则

1. **数据库归属唯一**：一张表只能属于一个微服务；服务不得直接读写其他服务的表。
2. **不跨库联表**：脚本不创建跨数据库外键，也不允许跨库 `JOIN`。
3. **只传业务 ID**：服务间只传递 `user_id`、`activity_id`、`question_id`、`item_id`、`order_no`、`payment_no` 等业务标识。
4. **订单保存快照**：订单明细保存物品名称、价格、图片、卖家 ID 等下单快照，避免订单服务回查二手物品库。
5. **支付通过业务号关联**：支付服务使用 `biz_order_no` 关联订单业务，不读取订单库；订单服务保存 `payment_no`。
6. **用户信息不复制成主数据**：其他服务只保存 `user_id`；展示昵称等信息通过用户服务接口获取，未来缓存也只能作为非权威副本。
7. **同库关系使用本地 ID**：例如订单明细的 `order_id` 只在 `campushub_order` 内使用，不代表跨服务外键。

## 业务范围

已纳入：

- 用户、用户地址
- 校园活动、活动报名、活动签到
- 问答、回答、评论、点赞、关注、标签
- 校园二手物品与分类
- 二手交易购物车
- 二手交易订单
- 支付单与支付成功事件 Outbox

明确不纳入：

- 管理员业务
- 通知业务
- AI / Spring AI 业务

## 设计说明

- 金额统一使用整数分，避免浮点误差。
- `order_no`、`payment_no` 使用字符串业务号，便于服务间传递和后续接入不同支付渠道。
- `tb_pay_event_outbox` 用于保证支付状态变更与支付成功事件记录在同一支付库事务内；消息发布由支付服务负责，订单服务只消费事件。
- 当前二手交易是唯一订单来源，`order_type`、`biz_type` 仅为后续扩展预留字段，本版不新增其他业务表。


联调演示数据：完成建表后执行 08_demo_data.sql，前端首页即可读取二手商品、活动和社区问题。
