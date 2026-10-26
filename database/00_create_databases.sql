-- CampusHub 数据库初始化（微服务隔离版）
-- 每个业务微服务拥有独立数据库，仅维护本服务表。
-- 不创建跨数据库外键，不允许跨数据库 JOIN；服务间只传递业务 ID。
-- 仅覆盖用户、活动报名签到、问答评论点赞关注、二手物品、购物车、订单、支付。
-- 不包含管理员、通知、AI 业务。

CREATE DATABASE IF NOT EXISTS campushub_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_activity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_secondhand DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_cart DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_payment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
