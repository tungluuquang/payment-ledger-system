#!/usr/bin/env bash
set -euo pipefail

EXECUTE=false

usage() {
  cat <<'EOF'
Usage:
  GCP_PROJECT_ID=<project> GKE_LOCATION=<region-or-zone> \
    GAR_LOCATION=<region> ./scripts/gke-destroy-all.sh [--execute]

Default mode is a dry run. Pass --execute to delete:
  - GKE Ingress and its Google Cloud load balancer
  - GKE cluster, node VMs, boot disks, and Kubernetes workloads
  - Cloud SQL instance and all databases
  - global static IP
  - Artifact Registry repository and all images
  - orphaned Persistent Disks previously backing PVCs

Secret Manager secrets and the runtime service account are retained by default.
Set DELETE_SECRETS=true and/or DELETE_SERVICE_ACCOUNT=true to remove them too.
EOF
}

case "${1:-}" in
  "")
    ;;
  --execute)
    EXECUTE=true
    ;;
  -h|--help)
    usage
    exit 0
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac

GCP_PROJECT_ID="${GCP_PROJECT_ID:?GCP_PROJECT_ID is required}"
GKE_CLUSTER="${GKE_CLUSTER:-payment-ledger-prod}"
GKE_LOCATION="${GKE_LOCATION:-${GCP_REGION:-}}"
CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE:-payment-ledger-prod}"
GAR_LOCATION="${GAR_LOCATION:-${GCP_REGION:-}}"
GAR_REPOSITORY="${GAR_REPOSITORY:-payment-ledger}"
GKE_STATIC_IP_NAME="${GKE_STATIC_IP_NAME:-payment-ledger-prod-ip}"
K8S_NAMESPACE="${K8S_NAMESPACE:-payment-ledger}"
GKE_INGRESS_NAME="${GKE_INGRESS_NAME:-payment-ledger}"
DELETE_ARTIFACT_REGISTRY="${DELETE_ARTIFACT_REGISTRY:-true}"
DELETE_SECRETS="${DELETE_SECRETS:-false}"
DELETE_SERVICE_ACCOUNT="${DELETE_SERVICE_ACCOUNT:-false}"

for command in gcloud kubectl; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Required command not found: ${command}" >&2
    exit 1
  }
done

if [[ -z "${GKE_LOCATION}" ]]; then
  echo "GKE_LOCATION or GCP_REGION is required." >&2
  exit 1
fi

if [[ -z "${GAR_LOCATION}" ]]; then
  echo "GAR_LOCATION or GCP_REGION is required." >&2
  exit 1
fi

cluster_exists() {
  gcloud container clusters describe "${GKE_CLUSTER}" \
    --location="${GKE_LOCATION}" \
    --project="${GCP_PROJECT_ID}" >/dev/null 2>&1
}

sql_exists() {
  gcloud sql instances describe "${CLOUD_SQL_INSTANCE}" \
    --project="${GCP_PROJECT_ID}" >/dev/null 2>&1
}

address_exists() {
  gcloud compute addresses describe "${GKE_STATIC_IP_NAME}" \
    --global \
    --project="${GCP_PROJECT_ID}" >/dev/null 2>&1
}

repository_exists() {
  gcloud artifacts repositories describe "${GAR_REPOSITORY}" \
    --location="${GAR_LOCATION}" \
    --project="${GCP_PROJECT_ID}" >/dev/null 2>&1
}

runtime_service_account() {
  printf 'payment-ledger-runtime@%s.iam.gserviceaccount.com' \
    "${GCP_PROJECT_ID}"
}

echo "Destruction plan"
echo "  Project:             ${GCP_PROJECT_ID}"
echo "  GKE cluster:         ${GKE_CLUSTER} (${GKE_LOCATION})"
echo "  Kubernetes namespace:${K8S_NAMESPACE}"
echo "  Cloud SQL:           ${CLOUD_SQL_INSTANCE}"
echo "  Static IP:           ${GKE_STATIC_IP_NAME}"
echo "  Artifact Registry:   ${GAR_REPOSITORY} (${GAR_LOCATION})"
echo "  Delete registry:     ${DELETE_ARTIFACT_REGISTRY}"
echo "  Delete secrets:      ${DELETE_SECRETS}"
echo "  Delete service acct: ${DELETE_SERVICE_ACCOUNT}"

if [[ "${EXECUTE}" != "true" ]]; then
  echo
  echo "Dry run only. Re-run with --execute to delete these resources."
  exit 0
fi

active_project="$(gcloud config get-value project 2>/dev/null)"
if [[ "${active_project}" != "${GCP_PROJECT_ID}" ]]; then
  echo "Active gcloud project is '${active_project}', expected '${GCP_PROJECT_ID}'." >&2
  exit 1
fi

echo
printf 'Type the project ID to confirm permanent deletion: '
read -r confirmation
if [[ "${confirmation}" != "${GCP_PROJECT_ID}" ]]; then
  echo "Confirmation did not match. Nothing was deleted." >&2
  exit 1
fi

orphan_disks=()

if cluster_exists; then
  echo "Connecting to GKE..."
  gcloud container clusters get-credentials "${GKE_CLUSTER}" \
    --location="${GKE_LOCATION}" \
    --project="${GCP_PROJECT_ID}" >/dev/null

  while IFS= read -r disk; do
    [[ -n "${disk}" ]] && orphan_disks+=("${disk}")
  done < <(
    for pv in $(
      kubectl -n "${K8S_NAMESPACE}" get pvc \
        -o jsonpath='{range .items[*]}{.spec.volumeName}{"\n"}{end}' \
        2>/dev/null || true
    ); do
      kubectl get pv "${pv}" \
        -o jsonpath='{.spec.gcePersistentDisk.pdName}{"\n"}' \
        2>/dev/null || true
      kubectl get pv "${pv}" \
        -o jsonpath='{.spec.csi.volumeHandle}{"\n"}' \
        2>/dev/null |
        sed -n 's|.*/disks/||p'
    done | sort -u
  )

  echo "Deleting Ingress first so GKE can clean up the load balancer..."
  kubectl -n "${K8S_NAMESPACE}" delete ingress "${GKE_INGRESS_NAME}" \
    --ignore-not-found --wait=true
  kubectl -n "${K8S_NAMESPACE}" delete managedcertificate \
    payment-ledger-certificate --ignore-not-found --wait=true || true
  kubectl -n "${K8S_NAMESPACE}" delete frontendconfig \
    payment-ledger-frontend --ignore-not-found --wait=true || true
  kubectl -n "${K8S_NAMESPACE}" delete backendconfig \
    frontend-backend api-gateway-backend \
    --ignore-not-found --wait=true || true

  echo "Deleting GKE cluster..."
  gcloud container clusters delete "${GKE_CLUSTER}" \
    --location="${GKE_LOCATION}" \
    --project="${GCP_PROJECT_ID}" \
    --quiet
else
  echo "GKE cluster not found; skipping."
fi

if sql_exists; then
  echo "Deleting Cloud SQL instance..."
  gcloud sql instances delete "${CLOUD_SQL_INSTANCE}" \
    --project="${GCP_PROJECT_ID}" \
    --quiet
else
  echo "Cloud SQL instance not found; skipping."
fi

if address_exists; then
  echo "Deleting global static IP..."
  gcloud compute addresses delete "${GKE_STATIC_IP_NAME}" \
    --global \
    --project="${GCP_PROJECT_ID}" \
    --quiet
else
  echo "Global static IP not found; skipping."
fi

if [[ "${DELETE_ARTIFACT_REGISTRY}" == "true" ]]; then
  if repository_exists; then
    echo "Deleting Artifact Registry repository and images..."
    gcloud artifacts repositories delete "${GAR_REPOSITORY}" \
      --location="${GAR_LOCATION}" \
      --project="${GCP_PROJECT_ID}" \
      --quiet
  else
    echo "Artifact Registry repository not found; skipping."
  fi
fi

for disk in "${orphan_disks[@]}"; do
  disk_record="$(
    gcloud compute disks list \
      --project="${GCP_PROJECT_ID}" \
      --filter="name=${disk}" \
      --format='csv[no-heading](zone.basename(),users)' \
      --limit=1
  )"
  if [[ -n "${disk_record}" ]]; then
    zone="${disk_record%%,*}"
    users="${disk_record#*,}"
  else
    zone=""
    users=""
  fi
  if [[ -n "${zone}" && -z "${users}" ]]; then
    echo "Deleting orphaned PVC disk ${disk} in ${zone}..."
    gcloud compute disks delete "${disk}" \
      --zone="${zone}" \
      --project="${GCP_PROJECT_ID}" \
      --quiet
  elif [[ -n "${users}" ]]; then
    echo "Disk ${disk} is still attached; leaving it untouched." >&2
  fi
done

if [[ "${DELETE_SECRETS}" == "true" ]]; then
  for secret in \
    payment-ledger-postgres-user \
    payment-ledger-postgres-password \
    payment-ledger-auth-client-secret \
    payment-ledger-bootstrap-admin-password; do
    gcloud secrets delete "${secret}" \
      --project="${GCP_PROJECT_ID}" \
      --quiet 2>/dev/null || true
  done
fi

if [[ "${DELETE_SERVICE_ACCOUNT}" == "true" ]]; then
  gcloud iam service-accounts delete "$(runtime_service_account)" \
    --project="${GCP_PROJECT_ID}" \
    --quiet 2>/dev/null || true
fi

echo
echo "Cleanup complete. Verify billing resources with:"
echo "  gcloud container clusters list"
echo "  gcloud sql instances list"
echo "  gcloud compute instances list"
echo "  gcloud compute disks list"
echo "  gcloud compute addresses list"
echo "  gcloud compute forwarding-rules list --global"
echo "  gcloud artifacts repositories list"
