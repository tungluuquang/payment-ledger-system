FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S discovery && adduser -S discovery -G discovery

WORKDIR /app
COPY build/libs/service-discovery-*.jar app.jar

USER discovery
EXPOSE 8761

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
