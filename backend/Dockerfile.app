# CodeForge AI - App Service Dockerfile
# 用于快速部署的一体化构建

FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 复制 pom 文件
COPY pom.xml .
COPY common/pom.xml common/
COPY ai/pom.xml ai/
COPY app/pom.xml app/
COPY app/app-api/pom.xml app/app-api/
COPY app/app-service/pom.xml app/app-service/

# 下载依赖
RUN mvn dependency:go-offline -pl app/app-service -am

# 复制源代码
COPY common/ common/
COPY ai/ ai/
COPY app/ app/

# 构建
ARG MODULE_PATH=app/app-service
RUN mvn clean package -pl ${MODULE_PATH} -am -Dmaven.test.skip=true

# 运行时镜像
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安装 Node.js 和 npm（用于构建生成的 Vue 项目）
RUN apk add --no-cache nodejs npm

# 复制 jar
COPY --from=builder /build/app/app-service/target/*.jar app.jar

# 创建工作目录
RUN mkdir -p /app/tmp/code_output /app/tmp/code_deploy

# 暴露端口
EXPOSE 8124 50053

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
