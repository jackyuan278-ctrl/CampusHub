# 通用运行镜像：只负责把一个 Spring Boot fat jar 跑起来
# 构建时用 --build-arg JAR_FILE=xxx.jar 指定，8 个服务共用这一份
FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE

# 时区交给 JVM 的 -Duser.timezone=Asia/Shanghai（JDK 自带 tzdata），
# 不装系统 tzdata —— 省掉 apk add 的联网等待，Alpine 源在国内经常卡死在这一步
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY ${JAR_FILE} /app/app.jar
RUN chown -R app:app /app
USER app

# 默认值兜底；实际由 docker-compose 的 .env 里 JAVA_OPTS 覆盖
ENV JAVA_OPTS="-Duser.timezone=Asia/Shanghai -Xms96m -Xmx128m -XX:MaxMetaspaceSize=128m -Xss256k -XX:MaxDirectMemorySize=32m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

# exec 让 java 成为 PID 1，docker stop 时能收到 SIGTERM 走优雅关闭
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
