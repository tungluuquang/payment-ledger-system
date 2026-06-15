#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GCP_PROJECT_ID="${GCP_PROJECT_ID:?GCP_PROJECT_ID is required}"
GAR_LOCATION="${GAR_LOCATION:?GAR_LOCATION is required}"
GAR_REPOSITORY="${GAR_REPOSITORY:?GAR_REPOSITORY is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
REGISTRY="${GAR_LOCATION}-docker.pkg.dev/${GCP_PROJECT_ID}/${GAR_REPOSITORY}"

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

for command in docker gcloud git; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Required command not found: ${command}" >&2
    exit 1
  }
done

gcloud auth configure-docker \
  "${GAR_LOCATION}-docker.pkg.dev" --quiet

cd "${ROOT_DIR}"

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Warning: the worktree is dirty; ${IMAGE_TAG} may not identify the image contents." >&2
fi

for service in "${services[@]}"; do
  image="${REGISTRY}/${service}:${IMAGE_TAG}"
  echo "Building and pushing ${image}"
  docker build \
    --file Dockerfile.service \
    --build-arg "MODULE=${service}" \
    --tag "${image}" \
    .
  docker push "${image}"
done

frontend_image="${REGISTRY}/frontend:${IMAGE_TAG}"
echo "Building and pushing ${frontend_image}"
docker build \
  --file frontend/Dockerfile \
  --tag "${frontend_image}" \
  frontend
docker push "${frontend_image}"
