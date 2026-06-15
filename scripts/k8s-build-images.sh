#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE="${MINIKUBE_PROFILE:-minikube}"
TAG="${IMAGE_TAG:-local}"

services=(
  service-discovery
  authorization-server
  user-service
  api-gateway
  command-service
  saga-service
  fraud-check-service
  account-service
  ledger-service
  projection-service
  reconciliation-service
  analytics-service
  audit-service
)

if ! minikube -p "${PROFILE}" status >/dev/null 2>&1; then
  echo "Minikube profile '${PROFILE}' is not running." >&2
  exit 1
fi

cd "${ROOT_DIR}"
eval "$(minikube -p "${PROFILE}" docker-env)"

for service in "${services[@]}"; do
  echo "Building payment-ledger/${service}:${TAG}"
  docker build \
    --file Dockerfile.service \
    --build-arg "MODULE=${service}" \
    --tag "payment-ledger/${service}:${TAG}" \
    .
done

echo "Building payment-ledger/frontend:${TAG}"
docker build \
  --file frontend/Dockerfile \
  --tag "payment-ledger/frontend:${TAG}" \
  frontend

