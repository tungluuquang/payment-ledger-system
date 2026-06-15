#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT="${ROOT_DIR}/deploy/k8s/base/spring-services.yaml"

services=(
  "service-discovery:8761"
  "authorization-server:9000"
  "user-service:8086"
  "api-gateway:8080"
  "command-service:8081"
  "saga-service:8087"
  "fraud-check-service:8084"
  "account-service:8082"
  "ledger-service:8083"
  "projection-service:8085"
  "reconciliation-service:8088"
  "analytics-service:8089"
  "audit-service:8090"
)

config_for() {
  case "$1" in
    service-discovery)
      printf '%s\n' \
        '  PORT: "8761"' \
        '  EUREKA_HOSTNAME: service-discovery'
      ;;
    authorization-server)
      printf '%s\n' \
        '  PORT: "9000"' \
        '  AUTH_DB_HOST: postgres' \
        '  AUTH_DB_PORT: "5432"' \
        '  AUTH_DB_NAME: user_db' \
        '  AUTH_SPA_CLIENT_ID: payment-ledger-spa' \
        '  AUTH_SPA_REDIRECT_URI: https://replace-me.example.com/callback' \
        '  AUTH_SPA_ORIGIN: https://replace-me.example.com'
      ;;
    user-service)
      printf '%s\n' \
        '  PORT: "8086"' \
        '  POSTGRES_DB: user_db' \
        '  USER_BOOTSTRAP_ADMIN_ENABLED: "true"' \
        '  USER_BOOTSTRAP_ADMIN_USERNAME: admin' \
        '  USER_BOOTSTRAP_ADMIN_EMAIL: admin@example.com' \
        '  USER_BOOTSTRAP_ADMIN_FULL_NAME: System Admin'
      ;;
    api-gateway)
      printf '%s\n' \
        '  PORT: "8080"' \
        '  GATEWAY_ALLOWED_ORIGINS: https://replace-me.example.com'
      ;;
    command-service)
      printf '%s\n' '  PORT: "8081"' '  POSTGRES_DB: command_db'
      ;;
    saga-service)
      printf '%s\n' '  PORT: "8087"' '  POSTGRES_DB: saga_db'
      ;;
    fraud-check-service)
      printf '%s\n' '  PORT: "8084"' '  POSTGRES_DB: fraud_db'
      ;;
    account-service)
      printf '%s\n' '  PORT: "8082"' '  POSTGRES_DB: account_db'
      ;;
    ledger-service)
      printf '%s\n' '  PORT: "8083"' '  POSTGRES_DB: ledger_db'
      ;;
    projection-service)
      printf '%s\n' '  PORT: "8085"' '  POSTGRES_DB: projection_db'
      ;;
    reconciliation-service)
      printf '%s\n' \
        '  PORT: "8088"' \
        '  ACCOUNT_POSTGRES_DB: account_db' \
        '  LEDGER_POSTGRES_DB: ledger_db'
      ;;
    analytics-service)
      printf '%s\n' '  PORT: "8089"' '  POSTGRES_DB: analytics_db'
      ;;
    audit-service)
      printf '%s\n' '  PORT: "8090"' '  POSTGRES_DB: audit_db'
      ;;
  esac
}

: > "${OUTPUT}"
separator=""

for entry in "${services[@]}"; do
  service="${entry%%:*}"
  port="${entry##*:}"

  {
    printf '%s' "${separator}"
    cat <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${service}-config
data:
$(config_for "${service}")
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${service}
spec:
  replicas: 1
  revisionHistoryLimit: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: ${service}
  template:
    metadata:
      labels:
        app.kubernetes.io/name: ${service}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: /actuator/prometheus
        prometheus.io/port: "${port}"
    spec:
      terminationGracePeriodSeconds: 45
      containers:
        - name: ${service}
          image: payment-ledger/${service}:latest
          imagePullPolicy: IfNotPresent
          envFrom:
            - configMapRef:
                name: platform-config
            - configMapRef:
                name: ${service}-config
            - secretRef:
                name: platform-secrets
          env:
            - name: JAVA_TOOL_OPTIONS
              value: -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError
          ports:
            - name: http
              containerPort: ${port}
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            initialDelaySeconds: 15
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 24
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 6
          lifecycle:
            preStop:
              exec:
                command: [sh, -c, "sleep 5"]
          resources:
            requests:
              cpu: 100m
              memory: 256Mi
            limits:
              cpu: 750m
              memory: 768Mi
---
apiVersion: v1
kind: Service
metadata:
  name: ${service}
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/path: /actuator/prometheus
    prometheus.io/port: "${port}"
spec:
  type: ClusterIP
  selector:
    app.kubernetes.io/name: ${service}
  ports:
    - name: http
      port: ${port}
      targetPort: http
EOF
  } >> "${OUTPUT}"

  separator=$'\n---\n'
done

echo "Generated ${OUTPUT}"
