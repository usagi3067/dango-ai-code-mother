# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

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
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# 换 Alpine 国内镜像源
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

# 安装 Node.js 20.x + npm + Chromium（mermaid-cli 渲染依赖）
RUN apk add --no-cache chromium nss freetype harfbuzz ca-certificates ttf-freefont curl \
    && curl -fsSL https://unofficial-builds.nodejs.org/download/release/v20.19.3/node-v20.19.3-linux-x64-musl.tar.xz \
       | tar -xJ -C /usr/local --strip-components=1 \
    && node --version && npm --version

# 安装 mermaid-cli（mmdc 依赖 chromium 做 SVG 渲染）
RUN npm config set registry https://registry.npmmirror.com && npm install -g @mermaid-js/mermaid-cli
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium-browser

COPY --from=build /app/app/app-service/target/app-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8124
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx384m} -jar app.jar"]
