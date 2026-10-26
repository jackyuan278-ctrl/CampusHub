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
4. 前端将 Mock 回退 API 逐步替换为真实接口。

Maven 安装目录：

```text
E:\develop\tools\maven\apache-maven-3.9.16
```

Maven 本地仓库：

```text
E:\develop\tools\maven\apache-maven-3.9.16\mvn_repo
```

GitHub 不需要 Codex 连接，也没有执行提交或推送。

