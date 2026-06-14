FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S gateway && adduser -S gateway -G gateway

WORKDIR /app
COPY build/libs/api-gateway-*.jar app.jar

USER gateway
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
