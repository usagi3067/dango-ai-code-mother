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
ARG SERVICE_MODULE
RUN mvn clean package -pl ${SERVICE_MODULE} -am -Dmaven.test.skip=true -B

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_MODULE
ARG SERVICE_NAME
COPY --from=build /app/${SERVICE_MODULE}/target/${SERVICE_NAME}-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx256m} -jar app.jar"]
