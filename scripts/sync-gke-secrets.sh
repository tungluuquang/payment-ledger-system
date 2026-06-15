#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-payment-ledger}"
GCP_PROJECT_ID="${GCP_PROJECT_ID:?GCP_PROJECT_ID is required}"
POSTGRES_USER_SECRET_ID="${POSTGRES_USER_SECRET_ID:-payment-ledger-postgres-user}"
POSTGRES_PASSWORD_SECRET_ID="${POSTGRES_PASSWORD_SECRET_ID:-payment-ledger-postgres-password}"
AUTH_CLIENT_SECRET_ID="${AUTH_CLIENT_SECRET_ID:-payment-ledger-auth-client-secret}"
BOOTSTRAP_ADMIN_PASSWORD_SECRET_ID="${BOOTSTRAP_ADMIN_PASSWORD_SECRET_ID:-payment-ledger-bootstrap-admin-password}"
GRAFANA_ADMIN_USER_SECRET_ID="${GRAFANA_ADMIN_USER_SECRET_ID:-}"
GRAFANA_ADMIN_PASSWORD_SECRET_ID="${GRAFANA_ADMIN_PASSWORD_SECRET_ID:-}"

for command in gcloud kubectl; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Required command not found: ${command}" >&2
    exit 1
  }
done

secret_value() {
  gcloud secrets versions access latest \
    --project="${GCP_PROJECT_ID}" \
    --secret="$1"
}

tmp_file="$(mktemp)"
trap 'rm -f "${tmp_file}"' EXIT
chmod 600 "${tmp_file}"

postgres_user="$(secret_value "${POSTGRES_USER_SECRET_ID}")"
postgres_password="$(secret_value "${POSTGRES_PASSWORD_SECRET_ID}")"

{
  printf 'POSTGRES_USER=%s\n' "${postgres_user}"
  printf 'POSTGRES_PASSWORD=%s\n' "${postgres_password}"
  printf 'AUTH_DB_USER=%s\n' "${postgres_user}"
  printf 'AUTH_DB_PASSWORD=%s\n' "${postgres_password}"
  printf 'AUTH_CLIENT_SECRET=%s\n' \
    "$(secret_value "${AUTH_CLIENT_SECRET_ID}")"
  printf 'USER_BOOTSTRAP_ADMIN_PASSWORD=%s\n' \
    "$(secret_value "${BOOTSTRAP_ADMIN_PASSWORD_SECRET_ID}")"
} > "${tmp_file}"

kubectl create namespace "${NAMESPACE}" \
  --dry-run=client \
  --output=yaml |
  kubectl apply -f -

kubectl -n "${NAMESPACE}" create secret generic platform-secrets \
  --from-env-file="${tmp_file}" \
  --dry-run=client \
  --output=yaml |
  kubectl apply -f -

if [[ -n "${GRAFANA_ADMIN_USER_SECRET_ID}" &&
      -n "${GRAFANA_ADMIN_PASSWORD_SECRET_ID}" ]]; then
  grafana_file="$(mktemp)"
  trap 'rm -f "${tmp_file}" "${grafana_file}"' EXIT
  chmod 600 "${grafana_file}"
  {
    printf 'GF_SECURITY_ADMIN_USER=%s\n' \
      "$(secret_value "${GRAFANA_ADMIN_USER_SECRET_ID}")"
    printf 'GF_SECURITY_ADMIN_PASSWORD=%s\n' \
      "$(secret_value "${GRAFANA_ADMIN_PASSWORD_SECRET_ID}")"
  } > "${grafana_file}"
  kubectl -n "${NAMESPACE}" create secret generic grafana-secrets \
    --from-env-file="${grafana_file}" \
    --dry-run=client \
    --output=yaml |
    kubectl apply -f -
fi

echo "Synchronized platform-secrets in namespace ${NAMESPACE}."
