# ---- Build Stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -B -f pom.xml -ntp dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Build args
ARG SERVER_PORT=8084
ARG JDBC_DATABASE_URL
ARG JDBC_DATABASE_USERNAME
ARG JDBC_DATABASE_PASSWORD
ARG KAFKA_BOOTSTRAP
ARG REDIS_URL
ARG CATALOG_SERVICE_URL
ARG BOOKING_SERVICE_URL

# Bake into image as ENV
ENV SERVER_PORT=$SERVER_PORT \
    JDBC_DATABASE_URL=$JDBC_DATABASE_URL \
    JDBC_DATABASE_USERNAME=$JDBC_DATABASE_USERNAME \
    JDBC_DATABASE_PASSWORD=$JDBC_DATABASE_PASSWORD \
    KAFKA_BOOTSTRAP=$KAFKA_BOOTSTRAP \
    REDIS_URL=$REDIS_URL \
    CATALOG_SERVICE_URL=$CATALOG_SERVICE_URL \
    BOOKING_SERVICE_URL=$BOOKING_SERVICE_URL

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE ${SERVER_PORT}
ENTRYPOINT ["java","-jar","/app/app.jar"]
