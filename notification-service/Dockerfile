FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl notification-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/notification-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
