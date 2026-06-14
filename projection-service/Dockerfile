FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S projection && adduser -S projection -G projection

WORKDIR /app
COPY build/libs/projection-service-*.jar app.jar

USER projection
EXPOSE 8085

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
