FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S fraud && adduser -S fraud -G fraud

WORKDIR /app
COPY build/libs/fraud-check-service-*.jar app.jar

USER fraud
EXPOSE 8084

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
