#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

required=(
  GCP_PROJECT_ID
  GAR_LOCATION
  GAR_REPOSITORY
  IMAGE_TAG
  GKE_HOSTNAME
  GKE_STATIC_IP_NAME
  CLOUD_SQL_INSTANCE_CONNECTION_NAME
  GCP_RUNTIME_SERVICE_ACCOUNT
)

for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Required environment variable is not set: ${name}" >&2
    exit 1
  fi
done

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT
cp -R "${ROOT_DIR}/deploy/k8s" "${tmp_dir}/k8s"

registry="${GAR_LOCATION}-docker.pkg.dev/${GCP_PROJECT_ID}/${GAR_REPOSITORY}"

replace() {
  local token="$1"
  local value="$2"
  local escaped
  escaped="$(printf '%s' "${value}" | sed 's/[&|]/\\&/g')"
  find "${tmp_dir}/k8s/overlays/gke" -type f \
    \( -name '*.yaml' -o -name '*.env.example' \) \
    -exec sed -i "s|${token}|${escaped}|g" {} +
}

replace GAR_REGISTRY "${registry}"
replace IMAGE_TAG "${IMAGE_TAG}"
replace GKE_HOSTNAME "${GKE_HOSTNAME}"
replace GKE_STATIC_IP_NAME "${GKE_STATIC_IP_NAME}"
replace GKE_CLOUD_SQL_CONNECTION_VALUE \
  "${CLOUD_SQL_INSTANCE_CONNECTION_NAME}"
replace GCP_RUNTIME_SERVICE_ACCOUNT "${GCP_RUNTIME_SERVICE_ACCOUNT}"
replace GKE_PROJECT_VALUE "${GCP_PROJECT_ID}"

kubectl kustomize "${tmp_dir}/k8s/overlays/gke"
