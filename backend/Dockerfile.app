# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 配置阿里云 Maven 镜像加速依赖下载
RUN mkdir -p /root/.m2 && echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/central</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY ai/pom.xml ai/pom.xml
COPY user/pom.xml user/pom.xml
COPY user/api/pom.xml user/api/pom.xml
COPY user/user-service/pom.xml user/user-service/pom.xml
COPY app/pom.xml app/pom.xml
COPY app/app-api/pom.xml app/app-api/pom.xml
COPY app/app-service/pom.xml app/app-service/pom.xml
COPY screenshot/pom.xml screenshot/pom.xml
COPY screenshot/screenshot-api/pom.xml screenshot/screenshot-api/pom.xml
COPY screenshot/screenshot-app/pom.xml screenshot/screenshot-app/pom.xml
COPY supabase/pom.xml supabase/pom.xml
COPY supabase/supabase-api/pom.xml supabase/supabase-api/pom.xml
COPY supabase/supabase-service/pom.xml supabase/supabase-service/pom.xml
RUN mvn dependency:go-offline -B || true

COPY . .
RUN mvn clean package -pl app/app-service -am -Dmaven.test.skip=true -B

# ---- Runtime Stage ----
# 使用预构建的基础镜像（含 JDK + Chromium + Node.js + mermaid-cli）
# 首次部署需先构建并传输基础镜像：
#   docker build -t dango-app-base:1.0 -f Dockerfile.app-base .
#   docker save dango-app-base:1.0 | ssh B 'docker load'
FROM dango-app-base:1.0

COPY --from=build /app/app/app-service/target/app-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8124
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx384m} -jar app.jar"]
