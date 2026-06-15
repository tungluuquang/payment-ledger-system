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

## Production Gaps

The Kustomize base has production-oriented health, resource, configuration, and
observability conventions, but it is not a complete production platform.
Remaining work includes TLS/Ingress, external secret management, network
policies, PodDisruptionBudgets, autoscaling, multi-replica Kafka/PostgreSQL or
managed equivalents, durable Prometheus/Grafana storage, backup/restore,
certificate and signing-key persistence, and a production identity lifecycle.
