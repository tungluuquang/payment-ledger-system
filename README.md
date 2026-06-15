# Payment Ledger System

A payment platform for studying ledger consistency, distributed transactions,
event-driven processing, reconciliation, analytics, and auditability.

## Architecture

The system contains a React/Nginx frontend, an API gateway, OAuth authorization,
service discovery, user/account/payment services, saga orchestration, fraud
checking, ledger recording, query projections, reconciliation, analytics, and
audit services. PostgreSQL stores service state and Kafka carries domain events.

Zipkin receives distributed traces. Prometheus discovers annotated Spring Boot
services and scrapes `/actuator/prometheus`; Grafana is provisioned with
Prometheus and Zipkin data sources.

## Docker Compose

Docker Compose remains the quickest development path:

```bash
docker compose up --build
```

The frontend is available at `http://localhost:5173`. The local admin account is
`admin` / `change-me-now`.

Observability endpoints:

* Grafana: `http://localhost:3000` (`admin` / `admin`)
* Prometheus: `http://localhost:9090`
* Zipkin: `http://localhost:9411`

## Minikube

### Prerequisites

Install Docker, Minikube, and `kubectl`. The full local platform is intentionally
large, so allocate enough resources:

```bash
minikube start --driver=docker --cpus=6 --memory=10240 --disk-size=30g
```

### Build Images

Build every Spring Boot service and the frontend into Minikube's Docker daemon:

```bash
./scripts/k8s-build-images.sh
```

The script is equivalent to selecting `minikube docker-env` and running
`docker build -f Dockerfile.service --build-arg MODULE=<service>` for every
service, followed by the frontend build. Set `MINIKUBE_PROFILE` when using a
profile other than `minikube`.

### Local Hostname

The local overlay uses a stable browser origin for OAuth. Add the Minikube IP to
your hosts file:

```bash
echo "$(minikube ip) payment-ledger.local" | sudo tee -a /etc/hosts
```

### Deploy

Deploy the complete platform:

```bash
kubectl apply -k deploy/k8s/overlays/local
```

Watch startup:

```bash
kubectl -n payment-ledger get pods -w
```

Open:

* Frontend: `http://payment-ledger.local:30080`
* API gateway health: `http://$(minikube ip):30081/actuator/health`

Only the frontend and API gateway are NodePort services. Authorization and all
other backend services remain ClusterIP and are reached through Kubernetes DNS.

For observability UIs, use port forwarding:

```bash
kubectl -n payment-ledger port-forward service/grafana 3000:3000
kubectl -n payment-ledger port-forward service/prometheus 9090:9090
kubectl -n payment-ledger port-forward service/zipkin 9411:9411
```

### Smoke Test

Run readiness, rollout, frontend, gateway, and proxy checks:

```bash
./scripts/k8s-smoke-test.sh
```

To include an end-to-end account creation and payment projection check, provide
a valid user access token:

```bash
K8S_SMOKE_ACCESS_TOKEN='<token>' ./scripts/k8s-smoke-test.sh
```

### Remove

```bash
kubectl delete -k deploy/k8s/overlays/local
```

PersistentVolumeClaims are retained separately. Delete them only when the local
database and Kafka data are no longer needed:

```bash
kubectl -n payment-ledger delete pvc --all
```

## Configuration

`deploy/k8s/base` contains environment-neutral resources and placeholder
secrets. `deploy/k8s/overlays/local` supplies local credentials, image tags,
browser origins, and NodePorts. Replace the local Secret values before using
these assets outside a workstation.

Every Spring Boot workload has:

* ConfigMap and Secret-based environment injection
* readiness and liveness probes through Spring Boot Actuator
* CPU and memory requests/limits
* graceful shutdown settings
* Prometheus scrape annotations
* trace-correlated logfmt console output

## Troubleshooting

Check failed pods and recent logs:

```bash
kubectl -n payment-ledger get pods
kubectl -n payment-ledger describe pod <pod-name>
kubectl -n payment-ledger logs <pod-name> --previous
```

If an application image is missing, rebuild it and restart the deployment:

```bash
./scripts/k8s-build-images.sh
kubectl -n payment-ledger rollout restart deployment
```

If PostgreSQL initialization changed after the first deployment, the init script
will not rerun against an existing volume. Remove the PostgreSQL PVC, then
reapply the overlay.

If `payment-ledger.local` does not open, verify `minikube ip`, the hosts-file
entry, and the frontend NodePort:

```bash
minikube ip
kubectl -n payment-ledger get service frontend api-gateway
```

For Docker-driver networking that cannot reach NodePorts directly, keep OAuth on
the documented hostname and use `minikube tunnel`, or use the smoke-test
port-forward checks for non-browser validation.

## Google Kubernetes Engine

### Production Architecture

Traffic enters a regional GKE cluster through a Google external Application Load
Balancer created by GKE Ingress. `/`, OAuth, and login traffic go to the
frontend; `/api` and `/actuator/health` go directly to the API gateway. All
remaining Services are ClusterIP-only.

The application path is:

```text
Internet -> Google Load Balancer -> Ingress
  -> frontend -> authorization-server
  -> api-gateway -> internal Spring services
Spring services -> Cloud SQL Auth Proxy sidecars -> Cloud SQL PostgreSQL
Spring services <-> Kafka
Spring services -> OpenTelemetry Collector -> Google Cloud Trace
GitHub Actions -> Artifact Registry -> GKE
```

The GKE overlay removes the in-cluster PostgreSQL StatefulSet. Database traffic
uses Workload Identity and a Cloud SQL Auth Proxy sidecar on every service that
owns or reads database state.

### Enable APIs

Set the target project and region:

```bash
export GCP_PROJECT_ID=my-project
export GCP_REGION=us-central1
gcloud config set project "${GCP_PROJECT_ID}"

gcloud services enable \
  artifactregistry.googleapis.com \
  container.googleapis.com \
  iamcredentials.googleapis.com \
  secretmanager.googleapis.com \
  sqladmin.googleapis.com \
  sts.googleapis.com \
  cloudtrace.googleapis.com
```

### Artifact Registry

```bash
export GAR_REPOSITORY=payment-ledger
gcloud artifacts repositories create "${GAR_REPOSITORY}" \
  --repository-format=docker \
  --location="${GCP_REGION}" \
  --description="Payment Ledger production images"
```

Images are immutable and tagged with the Git commit SHA. The registry path is
configurable as
`<region>-docker.pkg.dev/<project>/<repository>/<service>:<sha>`.

### GKE Cluster

Create a regional Standard cluster with Workload Identity and NetworkPolicy:

```bash
export GKE_CLUSTER=payment-ledger-prod
gcloud container clusters create "${GKE_CLUSTER}" \
  --region="${GCP_REGION}" \
  --release-channel=regular \
  --machine-type=e2-standard-4 \
  --num-nodes=1 \
  --enable-ip-alias \
  --enable-network-policy \
  --workload-pool="${GCP_PROJECT_ID}.svc.id.goog"

gcloud container clusters get-credentials "${GKE_CLUSTER}" \
  --region="${GCP_REGION}"
```

This creates one node per zone in a regional cluster. For regulated workloads,
also use private nodes, Cloud NAT, Binary Authorization, Shielded Nodes and a
separate production VPC.

### Cloud SQL

Create an HA PostgreSQL instance with automated backups and point-in-time
recovery:

```bash
export CLOUD_SQL_INSTANCE=payment-ledger-prod
gcloud sql instances create "${CLOUD_SQL_INSTANCE}" \
  --database-version=POSTGRES_17 \
  --region="${GCP_REGION}" \
  --availability-type=REGIONAL \
  --tier=db-custom-2-7680 \
  --storage-size=100 \
  --storage-type=SSD \
  --storage-auto-increase \
  --backup-start-time=02:00 \
  --enable-point-in-time-recovery
```

Create the service databases:

```bash
for database in command_db saga_db fraud_db account_db ledger_db \
  projection_db user_db analytics_db audit_db; do
  gcloud sql databases create "${database}" \
    --instance="${CLOUD_SQL_INSTANCE}"
done
```

Create a database user and store its username/password in Secret Manager. For a
stronger production posture, use private IP and IAM database authentication.

The connection name used by the proxy is:

```bash
export CLOUD_SQL_INSTANCE_CONNECTION_NAME="$(
  gcloud sql instances describe "${CLOUD_SQL_INSTANCE}" \
    --format='value(connectionName)'
)"
```

### Workload Identity

```bash
export GCP_RUNTIME_SERVICE_ACCOUNT=payment-ledger-runtime@${GCP_PROJECT_ID}.iam.gserviceaccount.com

gcloud iam service-accounts create payment-ledger-runtime
gcloud projects add-iam-policy-binding "${GCP_PROJECT_ID}" \
  --member="serviceAccount:${GCP_RUNTIME_SERVICE_ACCOUNT}" \
  --role=roles/cloudsql.client
gcloud projects add-iam-policy-binding "${GCP_PROJECT_ID}" \
  --member="serviceAccount:${GCP_RUNTIME_SERVICE_ACCOUNT}" \
  --role=roles/cloudtrace.agent

kubectl apply -f deploy/k8s/base/namespace.yaml
```

Bind both Kubernetes service accounts before deploying workloads:

```bash
for ksa in payment-ledger-runtime otel-collector; do
  gcloud iam service-accounts add-iam-policy-binding \
    "${GCP_RUNTIME_SERVICE_ACCOUNT}" \
    --role=roles/iam.workloadIdentityUser \
    --member="serviceAccount:${GCP_PROJECT_ID}.svc.id.goog[payment-ledger/${ksa}]"
done
```

### Secret Manager

Create these secrets and add values without committing them:

```text
payment-ledger-postgres-user
payment-ledger-postgres-password
payment-ledger-auth-client-secret
payment-ledger-bootstrap-admin-password
```

Grant the deployment service account `roles/secretmanager.secretAccessor`, then
materialize the Kubernetes Secret:

```bash
export GCP_PROJECT_ID=my-project
./scripts/sync-gke-secrets.sh
```

The GKE overlay deliberately omits `platform-secrets`; deployment fails closed
until this synchronization has happened. Secret names can be overridden using
the variables documented in
`deploy/k8s/overlays/gke/secret-manager.env.example`.

### Ingress and DNS

Reserve a global address and point the application DNS record to it:

```bash
export GKE_STATIC_IP_NAME=payment-ledger-prod-ip
gcloud compute addresses create "${GKE_STATIC_IP_NAME}" --global
gcloud compute addresses describe "${GKE_STATIC_IP_NAME}" \
  --global --format='value(address)'
```

Set an `A` record for the production hostname to that address. The overlay uses
a Google-managed certificate and redirects HTTP to HTTPS.

### Manual Deployment

Set all rendering variables:

```bash
export GAR_LOCATION="${GCP_REGION}"
export IMAGE_TAG='<git-sha>'
export GKE_HOSTNAME=payments.example.com
export GAR_REPOSITORY=payment-ledger
export GKE_STATIC_IP_NAME=payment-ledger-prod-ip
export GCP_RUNTIME_SERVICE_ACCOUNT
export CLOUD_SQL_INSTANCE_CONNECTION_NAME
```

Render, inspect, and apply:

```bash
./scripts/render-gke-manifests.sh > /tmp/payment-ledger-gke.yaml
kubectl apply --server-side -f /tmp/payment-ledger-gke.yaml
kubectl -n payment-ledger rollout status deployment --all --timeout=15m
GKE_BASE_URL="https://${GKE_HOSTNAME}" ./scripts/gke-smoke-test.sh
```

To run the payment flow, set `GKE_SMOKE_ACCESS_TOKEN` to a valid user token.

### GitHub Actions

`build.yml` runs backend tests and the frontend build, validates manifests, then
builds all 14 images in parallel and pushes SHA-tagged images to Artifact
Registry.

`deploy-gke.yml` runs after a successful main-branch build or manual dispatch.
It authenticates with Workload Identity Federation, gets GKE credentials,
synchronizes Secret Manager, renders the exact image SHA, applies it using
server-side apply, verifies rollouts, and runs the GKE smoke test.

Create a dedicated CI service account and grant only the deployment roles:

```bash
export GITHUB_OWNER=my-org
export GITHUB_REPOSITORY=payment-ledger-system
export GCP_CICD_SERVICE_ACCOUNT=payment-ledger-cicd@${GCP_PROJECT_ID}.iam.gserviceaccount.com

gcloud iam service-accounts create payment-ledger-cicd
for role in roles/artifactregistry.writer roles/container.developer \
  roles/secretmanager.secretAccessor; do
  gcloud projects add-iam-policy-binding "${GCP_PROJECT_ID}" \
    --member="serviceAccount:${GCP_CICD_SERVICE_ACCOUNT}" \
    --role="${role}"
done
```

Create a GitHub OIDC provider restricted to this repository:

```bash
export GCP_PROJECT_NUMBER="$(
  gcloud projects describe "${GCP_PROJECT_ID}" \
    --format='value(projectNumber)'
)"

gcloud iam workload-identity-pools create github \
  --location=global \
  --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc github \
  --location=global \
  --workload-identity-pool=github \
  --issuer-uri=https://token.actions.githubusercontent.com \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
  --attribute-condition="assertion.repository=='${GITHUB_OWNER}/${GITHUB_REPOSITORY}'"

gcloud iam service-accounts add-iam-policy-binding \
  "${GCP_CICD_SERVICE_ACCOUNT}" \
  --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/${GCP_PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/attribute.repository/${GITHUB_OWNER}/${GITHUB_REPOSITORY}"

export GCP_WORKLOAD_IDENTITY_PROVIDER="projects/${GCP_PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/providers/github"
```

Configure these GitHub repository or production-environment variables:

```text
GCP_PROJECT_ID
GAR_LOCATION
GAR_REPOSITORY
GCP_WORKLOAD_IDENTITY_PROVIDER
GCP_CICD_SERVICE_ACCOUNT
GCP_RUNTIME_SERVICE_ACCOUNT
GKE_CLUSTER
GKE_LOCATION
GKE_HOSTNAME
GKE_STATIC_IP_NAME
CLOUD_SQL_INSTANCE_CONNECTION_NAME
```

Optionally configure the protected environment secret
`GKE_SMOKE_ACCESS_TOKEN`. Require reviewers on the `production` GitHub
environment and restrict the Workload Identity provider to this repository and
main branch.

The CI service account needs Artifact Registry writer, GKE deploy permissions,
Secret Manager accessor, and Workload Identity user permissions. Avoid
service-account JSON keys.

### Observability

The default GKE deployment runs two OpenTelemetry Collector replicas and exports
traces to Google Cloud Trace. Spring services continue sending Zipkin-compatible
traces to the collector, allowing an incremental migration to OTLP.

Prometheus and Grafana are prepared but optional:

```bash
export GRAFANA_ADMIN_USER_SECRET_ID=payment-ledger-grafana-admin-user
export GRAFANA_ADMIN_PASSWORD_SECRET_ID=payment-ledger-grafana-admin-password
./scripts/sync-gke-secrets.sh
kubectl apply -k deploy/k8s/overlays/gke/monitoring
```

This package uses persistent disks and private ClusterIP services. For larger
installations, prefer Google Managed Service for Prometheus and managed Grafana.

### Rollback

Redeploy a previously successful immutable SHA:

```bash
gh workflow run deploy-gke.yml -f image_tag='<previous-git-sha>'
```

For an immediate Kubernetes rollback before diagnosing a bad rollout:

```bash
kubectl -n payment-ledger rollout undo deployment/<service>
kubectl -n payment-ledger rollout status deployment/<service>
```

Database schema changes must remain backward compatible across at least one
application release because application rollback does not roll back Cloud SQL.

### Disaster Recovery

* Enable Cloud SQL regional HA, PITR, automated backups, and cross-region backup
  export or replica according to recovery objectives.
* Regularly restore backups into an isolated project and run application smoke
  tests; an untested backup is not a recovery plan.
* Retain Artifact Registry images and Git tags for the full rollback window.
* Export Kubernetes configuration through Git; do not treat the cluster as the
  source of truth.
* Define Kafka durability and recovery separately. The included single-broker
  Kafka StatefulSet is not sufficient for production disaster recovery.
* Persist authorization signing keys before scaling the authorization server or
  relying on cross-region recovery.

## Production Assessment

The Kustomize base has production-oriented health, resource, configuration, and
observability conventions. The GKE overlay adds HTTPS Ingress, external Cloud
SQL connectivity, Workload Identity, immutable Artifact Registry images,
default-deny networking, non-root containers, HPA, PDB, anti-affinity and
automated rollout verification.

The largest remaining gaps are production Kafka, persistent authorization
signing keys, managed schema migration controls, SLO-based alerting, image
vulnerability policy enforcement, supply-chain signing, multi-region failover
and tested recovery automation.

## Future Plan

* Automated reconciliation and consistency verification.
* Real-time fraud detection using Machine Learning and risk scoring.
* Large-scale analytics and reporting.
* Chaos testing and resilience validation.
* Creating documentation and architecture guides.
