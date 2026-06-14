FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S ledger && adduser -S ledger -G ledger

WORKDIR /app
COPY build/libs/ledger-service-*.jar app.jar

USER ledger
EXPOSE 8083

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
