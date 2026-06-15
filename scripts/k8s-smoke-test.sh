#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-payment-ledger}"
TIMEOUT="${K8S_READY_TIMEOUT:-10m}"
FRONTEND_PORT="${K8S_SMOKE_FRONTEND_PORT:-15173}"
GATEWAY_PORT="${K8S_SMOKE_GATEWAY_PORT:-18080}"
ACCESS_TOKEN="${K8S_SMOKE_ACCESS_TOKEN:-}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

cleanup() {
  jobs -pr | xargs -r kill 2>/dev/null || true
}
trap cleanup EXIT INT TERM

require_command kubectl
require_command curl

echo "Waiting for every pod in ${NAMESPACE} to become Ready..."
kubectl -n "${NAMESPACE}" wait \
  --for=condition=Ready pod \
  --all \
  --timeout="${TIMEOUT}"

echo "Checking deployments and stateful workloads..."
for deployment in $(kubectl -n "${NAMESPACE}" get deployment -o name); do
  kubectl -n "${NAMESPACE}" rollout status "${deployment}" \
    --timeout="${TIMEOUT}"
done
kubectl -n "${NAMESPACE}" rollout status statefulset/postgres \
  --timeout="${TIMEOUT}"
kubectl -n "${NAMESPACE}" rollout status statefulset/kafka \
  --timeout="${TIMEOUT}"

kubectl -n "${NAMESPACE}" port-forward \
  service/frontend "${FRONTEND_PORT}:80" >/tmp/payment-ledger-frontend-pf.log 2>&1 &
kubectl -n "${NAMESPACE}" port-forward \
  service/api-gateway "${GATEWAY_PORT}:8080" >/tmp/payment-ledger-gateway-pf.log 2>&1 &

for _ in $(seq 1 30); do
  if curl --silent --fail "http://127.0.0.1:${FRONTEND_PORT}/" >/dev/null &&
     curl --silent --fail \
       "http://127.0.0.1:${GATEWAY_PORT}/actuator/health" >/dev/null; then
    break
  fi
  sleep 1
done

echo "Checking frontend..."
curl --silent --show-error --fail \
  "http://127.0.0.1:${FRONTEND_PORT}/" >/dev/null

echo "Checking API gateway health..."
curl --silent --show-error --fail \
  "http://127.0.0.1:${GATEWAY_PORT}/actuator/health" |
  grep -q '"status":"UP"'

echo "Checking frontend-to-gateway proxy..."
proxy_status="$(
  curl --silent --output /dev/null --write-out '%{http_code}' \
    "http://127.0.0.1:${FRONTEND_PORT}/api/accounts"
)"
case "${proxy_status}" in
  200|401|403) ;;
  *)
    echo "Frontend proxy returned unexpected HTTP ${proxy_status}" >&2
    exit 1
    ;;
esac

if [[ -z "${ACCESS_TOKEN}" ]]; then
  echo "Payment flow skipped: set K8S_SMOKE_ACCESS_TOKEN to run it."
  echo "Core Kubernetes smoke tests passed."
  exit 0
fi

uuid() {
  cat /proc/sys/kernel/random/uuid
}

source_account_id="$(uuid)"
destination_account_id="$(uuid)"
correlation_id="$(uuid)"
idempotency_key="$(uuid)"
gateway="http://127.0.0.1:${GATEWAY_PORT}"
auth_header="Authorization: Bearer ${ACCESS_TOKEN}"

echo "Creating smoke-test accounts..."
curl --silent --show-error --fail \
  -H "${auth_header}" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":\"${source_account_id}\",\"initialBalance\":100,\"currency\":\"USD\"}" \
  "${gateway}/api/accounts" >/dev/null
curl --silent --show-error --fail \
  -H "${auth_header}" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":\"${destination_account_id}\",\"initialBalance\":0,\"currency\":\"USD\"}" \
  "${gateway}/api/accounts" >/dev/null

echo "Submitting payment..."
payment_response="$(
  curl --silent --show-error --fail \
    -H "${auth_header}" \
    -H "Content-Type: application/json" \
    -d "{
      \"sourceAccountId\":\"${source_account_id}\",
      \"destinationAccountId\":\"${destination_account_id}\",
      \"correlationId\":\"${correlation_id}\",
      \"amount\":10,
      \"currency\":\"USD\",
      \"idempotencyKey\":\"${idempotency_key}\",
      \"description\":\"Kubernetes smoke test\"
    }" \
    "${gateway}/api/payments"
)"
payment_id="$(
  printf '%s' "${payment_response}" |
    sed -n 's/.*"paymentId":"\([^"]*\)".*/\1/p'
)"

if [[ -z "${payment_id}" ]]; then
  echo "Payment response did not contain paymentId: ${payment_response}" >&2
  exit 1
fi

echo "Waiting for payment ${payment_id} projection..."
for _ in $(seq 1 60); do
  status="$(
    curl --silent --output /tmp/payment-ledger-payment.json \
      --write-out '%{http_code}' \
      -H "${auth_header}" \
      "${gateway}/api/payments/${payment_id}"
  )"
  if [[ "${status}" == "200" ]]; then
    echo "End-to-end payment flow reached the query projection."
    echo "All Kubernetes smoke tests passed."
    exit 0
  fi
  sleep 2
done

echo "Payment projection was not available before timeout." >&2
exit 1
