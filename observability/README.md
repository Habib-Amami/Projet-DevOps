# Observability on EKS

The cluster runs the same Grafana LGTM backend as local Docker Compose:

| Signal | Collection path | Storage |
| --- | --- | --- |
| Metrics | Spring Boot -> OTLP HTTP `4318` -> collector | Prometheus |
| Traces | Spring Boot -> OTLP HTTP `4318` -> collector | Tempo |
| Logs | Pod stdout/stderr -> Alloy -> Loki HTTP `3100` | Loki |

Grafana displays all three. Profiling and Kubernetes node metrics are not
configured. The LGTM image includes Pyroscope, but the application does not
send profiles.

The setup is for the AWS Learner Lab demo. `grafana/otel-lgtm:0.38.0` and
`grafana/alloy:v1.21.2` match the local images inspected during implementation.
Grafana and collector endpoints are internal `ClusterIP` services.

## What gets deployed

- `k8s/grafana-lgtm.yaml`: one LGTM Deployment and a Service for Grafana,
  OTLP, and Loki. Its startup probe waits for all bundled services.
- `k8s/alloy.yaml`: one Alloy Deployment, its configuration, and namespace-scoped
  RBAC. It reads backend, frontend, and MySQL pod logs in `achat` through the
  Kubernetes API. It needs no Docker socket or host filesystem mounts.
- `k8s/configmap.yaml`: enables backend metrics and tracing and points their
  exporters at `http://grafana-lgtm:4318`.

One Alloy instance collects logs across the namespace, including pods on
different nodes. Keep one replica to avoid duplicate log collection. Its
ServiceAccount can discover pods and read logs; it cannot read Secrets.

## Deploy with Jenkins

1. Start the Learner Lab and create/update the EKS cluster using `terraform/`.
2. Update Jenkins' `aws-learner-lab` credential with the current lab credentials.
3. Make sure the Jenkins job's SCM branch specifier is `*/develop`, so it loads
   the pipeline with the monitoring deployment.
4. Run **Build with Parameters** with:

   | Parameter | Value |
   | --- | --- |
   | `BRANCH` | `develop` |
   | `DOCKERHUB_USER` | your Docker Hub namespace |
   | `DOCKER_PUSH` | checked |
   | `DEPLOY_K8S` | checked |
   | `EKS_CLUSTER` | `achat-eks`, unless overridden in Terraform |
   | `AWS_REGION` | `us-east-1`, unless overridden in Terraform |
   | `EMAIL_TO` | your notification recipient, if desired |

The deployment stage applies all top-level `k8s/*.yaml`, including monitoring.
It restarts Alloy to load configuration changes and waits for LGTM, Alloy, and
the application deployments. The new backend image starts with telemetry enabled.
The log prints the application URL and port-forward commands for Grafana and Swagger.

## Add monitoring to an application already running on EKS

From the repository root, with working AWS CLI credentials:

```bash
aws eks update-kubeconfig --region us-east-1 --name achat-eks
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/grafana-lgtm.yaml -f k8s/alloy.yaml
kubectl -n achat rollout restart deployment/alloy
kubectl -n achat rollout status deployment/grafana-lgtm --timeout=10m
kubectl -n achat rollout status deployment/alloy --timeout=5m
kubectl apply -f k8s/configmap.yaml
kubectl -n achat rollout restart deployment/backend
kubectl -n achat rollout status deployment/backend --timeout=10m
```

Restarting the existing backend is necessary because ConfigMap environment
variables are read at pod startup. This path keeps the already deployed app
images; the Jenkins path builds and deploys new images.

## Open Grafana

Run this on your PC in a terminal with AWS credentials and keep it open:

```bash
kubectl -n achat port-forward svc/grafana-lgtm 3001:3000
```

Open <http://localhost:3001>, sign in with `admin` / `admin`, and set a new
password when prompted. Anonymous access is disabled. Local port `3001` avoids
conflicting with Compose's Grafana on `3000`.

Use the application to generate some requests. Metrics may take about a minute
to arrive. In Grafana:

- **Dashboards**: open the bundled JVM Overview and RED Metrics dashboards.
- **Explore -> Prometheus**: try `{service_name="achat"}`.
- **Explore -> Tempo**: search for service `achat`.
- **Explore -> Loki**: try `{namespace="achat", service="backend"}` or
  `{job="kubernetes", service="frontend"}`.

The Docker-specific query `{job="docker", service="backend"}` applies to local
Compose logs. Cluster logs use `job="kubernetes"` and include namespace, pod,
container, and service labels.

Custom dashboards created locally do not transfer automatically. Export their
JSON from Grafana and import it into the cluster instance.

## Open Swagger for the cluster backend

In another terminal, run:

```bash
kubectl -n achat port-forward svc/backend 8090:8089
```

Open <http://localhost:8090/swagger-ui/index.html>. Swagger is already enabled
by `SPRINGDOC_ENABLED=true` in the cluster ConfigMap. **Try it out** calls the
cluster backend and can generate traffic for Grafana metrics and traces.
The OpenAPI JSON is at <http://localhost:8090/v3/api-docs>.

## Storage and troubleshooting

LGTM and Alloy use `emptyDir` storage to work within the lab's existing storage
constraints. LGTM data survives a container restart in the same pod, but pod
replacement, a deployment update, node loss, or namespace deletion removes
dashboards, password changes, and telemetry history. Export custom dashboards
before ending the demo. Durable history requires persistent storage.

The monitoring pods request an additional 600m CPU and 1152Mi memory. LGTM's
memory limit is 2Gi; Alloy's is 512Mi. Check available node resources if pods
remain Pending or are OOMKilled:

```bash
kubectl -n achat get pods,svc
kubectl -n achat describe pod -l app=grafana-lgtm
kubectl -n achat logs deployment/grafana-lgtm --tail=100
kubectl -n achat logs deployment/alloy --tail=100
```

For missing logs, check Alloy for `forbidden` errors or failed writes to Loki.
For missing metrics or traces, check backend logs and the ConfigMap's OTLP
endpoints. Alloy readiness confirms it loaded its configuration; inspect its
logs to confirm collection and delivery.

When editing `alloy-config` manually, run `kubectl -n achat rollout restart
deployment/alloy` to load the new configuration. Jenkins does this automatically.

Deleting namespace `achat` also removes the monitoring resources:

```bash
kubectl delete namespace achat
```

References: [Grafana's demo Kubernetes deployment](https://github.com/grafana/docker-otel-lgtm/blob/main/k8s/lgtm.yaml)
and [Alloy Kubernetes log collection](https://grafana.com/docs/alloy/latest/reference/components/loki/loki.source.kubernetes/).
