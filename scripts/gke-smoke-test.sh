#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-payment-ledger}"
TIMEOUT="${GKE_READY_TIMEOUT:-15m}"
BASE_URL="${GKE_BASE_URL:?Set GKE_BASE_URL, for example https://payments.example.com}"
ACCESS_TOKEN="${GKE_SMOKE_ACCESS_TOKEN:-}"

for command in kubectl curl; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Required command not found: ${command}" >&2
    exit 1
  }
done

echo "Checking Kubernetes rollouts..."
kubectl -n "${NAMESPACE}" rollout status deployment --all \
  --timeout="${TIMEOUT}"
kubectl -n "${NAMESPACE}" rollout status statefulset/kafka \
  --timeout="${TIMEOUT}"

echo "Checking internal service DNS and health..."
smoke_pod="gke-smoke-$(date +%s)"
kubectl -n "${NAMESPACE}" run "${smoke_pod}" \
  --image=curlimages/curl:8.14.1 \
  --restart=Never \
  --rm \
  --attach \
  --command -- sh -ec '
    for endpoint in \
      http://service-discovery:8761/actuator/health/readiness \
      http://authorization-server:9000/actuator/health/readiness \
      http://account-service:8082/actuator/health/readiness \
      http://command-service:8081/actuator/health/readiness \
      http://projection-service:8085/actuator/health/readiness; do
      curl --silent --show-error --fail "${endpoint}" |
        grep -q "\"status\":\"UP\""
    done
  '

echo "Waiting for the Google Cloud Load Balancer..."
for _ in $(seq 1 60); do
  if curl --silent --show-error --fail \
    "${BASE_URL}/actuator/health" |
    grep -q '"status":"UP"'; then
    break
  fi
  sleep 10
done

curl --silent --show-error --fail "${BASE_URL}/" >/dev/null
curl --silent --show-error --fail \
  "${BASE_URL}/actuator/health" |
  grep -q '"status":"UP"'

echo "Checking gateway-to-account-service routing..."
status="$(
  curl --silent --output /dev/null --write-out '%{http_code}' \
    "${BASE_URL}/api/accounts"
)"
case "${status}" in
  200|401|403) ;;
  *)
    echo "Core service route returned unexpected HTTP ${status}." >&2
    exit 1
    ;;
esac

if [[ -z "${ACCESS_TOKEN}" ]]; then
  echo "Payment flow skipped: set GKE_SMOKE_ACCESS_TOKEN to run it."
  echo "GKE smoke tests passed."
  exit 0
fi

uuid() {
  cat /proc/sys/kernel/random/uuid
}

source_account_id="$(uuid)"
destination_account_id="$(uuid)"
auth_header="Authorization: Bearer ${ACCESS_TOKEN}"

echo "Creating accounts for the payment smoke test..."
curl --silent --show-error --fail \
  -H "${auth_header}" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":\"${source_account_id}\",\"initialBalance\":100,\"currency\":\"USD\"}" \
  "${BASE_URL}/api/accounts" >/dev/null
curl --silent --show-error --fail \
  -H "${auth_header}" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":\"${destination_account_id}\",\"initialBalance\":0,\"currency\":\"USD\"}" \
  "${BASE_URL}/api/accounts" >/dev/null

payment_response="$(
  curl --silent --show-error --fail \
    -H "${auth_header}" \
    -H "Content-Type: application/json" \
    -d "{
      \"sourceAccountId\":\"${source_account_id}\",
      \"destinationAccountId\":\"${destination_account_id}\",
      \"correlationId\":\"$(uuid)\",
      \"amount\":10,
      \"currency\":\"USD\",
      \"idempotencyKey\":\"$(uuid)\",
      \"description\":\"GKE smoke test\"
    }" \
    "${BASE_URL}/api/payments"
)"
payment_id="$(
  printf '%s' "${payment_response}" |
    sed -n 's/.*"paymentId":"\([^"]*\)".*/\1/p'
)"

if [[ -z "${payment_id}" ]]; then
  echo "Payment response did not contain paymentId." >&2
  exit 1
fi

for _ in $(seq 1 60); do
  status="$(
    curl --silent --output /dev/null --write-out '%{http_code}' \
      -H "${auth_header}" \
      "${BASE_URL}/api/payments/${payment_id}"
  )"
  if [[ "${status}" == "200" ]]; then
    echo "End-to-end payment flow reached the projection service."
    echo "All GKE smoke tests passed."
    exit 0
  fi
  sleep 2
done

echo "Payment projection was not available before timeout." >&2
exit 1
