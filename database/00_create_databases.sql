-- CampusHub 第一版数据库初始化
-- 仅包含现有用户、活动、问答社区、二手交易、购物车、订单、支付业务。
-- 不包含管理员、通知、AI 业务。
CREATE DATABASE IF NOT EXISTS campushub_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_activity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_secondhand DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS campushub_pay DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
