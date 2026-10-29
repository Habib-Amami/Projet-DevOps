# Achat

Achat is a full-stack purchasing-management application for small businesses. It
tracks products, stocks, suppliers, business sectors, operators, invoices and
payments, with an Angular web UI backed by a Spring Boot REST API and MySQL.

The project is developed as a DevOps exercise: both applications are
containerized, the whole stack runs with Docker Compose, a Jenkins pipeline
builds, tests, analyses (SonarQube) and publishes (Nexus, Docker Hub) every
change, and Terraform creates the Kubernetes cluster on AWS where Jenkins
deploys the application.

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [Quick start with Docker Compose](#quick-start-with-docker-compose)
- [How Compose fits in this project](#how-compose-fits-in-this-project)
- [Configuration](#configuration)
- [Local development with native tooling](#local-development-with-native-tooling)
- [API at a glance](#api-at-a-glance)
- [Testing and CI](#testing-and-ci)
- [Deploy to AWS EKS](#deploy-to-aws-eks)
- [Troubleshooting](#troubleshooting)
- [Security and production notes](#security-and-production-notes)

## Features

- **Products and categories** - CRUD, assign a product to a stock.
- **Stocks** - CRUD, minimum-quantity warning.
- **Suppliers** - CRUD, business-sector assignment, supplier invoices.
- **Business sectors and operators** - CRUD.
- **Invoices** - list, create, detail, cancel/archive, invoice lines, assign an
  operator, recovery-rate statistics.
- **Payments** - list, detail, delete, filter by invoice, revenue over a period.
- **Database migrations** - Flyway owns the schema (`V1__init.sql` baseline) and
  Hibernate validates it at startup.
- **Development data** - an idempotent seeder fills the database when
  `APP_SEED_ENABLED=true`; it is enabled by the local Compose stack and disabled
  by default for the cluster.
- **No end-user authentication** - the UI has no login page and the API is open
  for local testing; in production the API stays internal behind nginx.

## Architecture

```
                    http://localhost:4200
Browser ─────────────────────────────────┐
                                         ▼
                              ┌─────────────────────┐
                              │ frontend (nginx)    │
                              │ static Angular SPA  │
                              └──────────┬──────────┘
                                         │ /api/*  (proxy strips /api)
                                         ▼
                              ┌─────────────────────┐
                              │ backend             │
                              │ Spring Boot :8089   │
                              └──────────┬──────────┘
                                         │ JDBC :3306
                                         ▼
                              ┌─────────────────────┐
                              │ mysql:8.4           │
                              │ volume: mysql-data  │
                              └─────────────────────┘

             Observability: grafana-lgtm (:3000, :4317, :4318) + Alloy
```

The backend pushes metrics and traces to LGTM over OTLP. Alloy reads container
stdout/stderr and pushes logs to Loki, so Grafana shows metrics, traces and logs
without any logging code in the application. Profiling (Pyroscope) is included
in the LGTM image but is intentionally not configured.

The browser only talks to nginx. nginx serves the Angular bundle and forwards
`/api/*` to `http://backend:8089/*`, so there is no CORS setup. This mirrors the
Angular dev proxy (`frontend/proxy.conf.js`, which reads `BACKEND_URL` from the
root `.env`) used with `ng serve`.

## Tech stack

| Layer      | Technology |
| ---------- | ---------- |
| Frontend   | Angular 21, Angular Material/CDK, TypeScript 5.9, signals, Reactive Forms, SCSS, Vitest |
| Backend    | Java 21, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, Actuator, springdoc OpenAPI, Micrometer/OpenTelemetry |
| Database   | MySQL 8.4, Flyway migrations with Hibernate schema validation |
| Containers | Multi-stage Dockerfiles, unprivileged nginx, Docker Compose |
| CI         | Jenkins (`Jenkinsfile`) |

## Repository layout

```
.
├── compose.yaml              # full stack: MySQL + backend + frontend + LGTM + Alloy
├── .env.example              # single local template: backend, frontend, Compose
├── Jenkinsfile               # CI/CD: build, test, Sonar, Nexus, Docker Hub, EKS
├── ci/                       # local Jenkins + SonarQube + Nexus stack
├── terraform/                # EKS cluster on the AWS Academy Learner Lab
├── k8s/                      # Kubernetes manifests (MySQL, backend, frontend)
├── observability/
│   └── alloy/config.alloy    # local Docker log collection for Loki
├── backend/
│   ├── Dockerfile            # multi-stage Spring Boot image
│   ├── README.md             # backend guide
│   ├── pom.xml
│   └── src/
└── frontend/
    ├── Dockerfile            # Node build + unprivileged nginx runtime
    ├── nginx.conf.template   # SPA routing and /api reverse proxy
    ├── proxy.conf.js         # dev proxy reads BACKEND_URL from root .env
    ├── .dockerignore
    ├── README.md             # frontend guide
    ├── package.json
    └── src/
```

## Quick start with Docker Compose

**Requirements:** Docker Engine with the Compose plugin.

1. Create the local configuration and set two **different** passwords:

   ```bash
   cp .env.example .env
   openssl rand -hex 24   # paste into MYSQL_ROOT_PASSWORD
   openssl rand -hex 24   # paste into DB_PASSWORD
   ```

   Keep `DB_USERNAME=achat_user`. The MySQL image refuses `root` as the
   application user.

2. Build and start everything:

   ```bash
   docker compose up -d --build
   ```

3. Wait until the services are running (and healthy where a healthcheck exists):

   ```bash
   docker compose ps
   ```

4. Open the applications:

   | What              | URL |
   | ----------------- | --- |
   | Web UI            | <http://localhost:4200> |
   | API / Swagger UI  | <http://localhost:8089/swagger-ui/index.html> |
   | API health        | <http://localhost:8089/actuator/health> |
   | Grafana           | <http://localhost:3000> (`admin` / `admin`) |

The first start creates the `achat_mysql-data` volume, the `achatdb` database,
the `achat_user` account and the development seed data. Later starts reuse the
existing volume. Local Compose sets `APP_SEED_ENABLED=true`,
`SPRINGDOC_ENABLED=true` and the OTLP endpoints; none of that is baked into the
application image.

Useful commands:

| Command | Purpose |
| ------- | ------- |
| `docker compose logs -f backend frontend` | Follow application logs |
| `docker compose up -d --build frontend` | Rebuild only the UI after a change |
| `docker compose down` | Stop the stack, keep database data |
| `docker compose down -v` | Stop the stack **and delete the database volume** |
| `OTEL_METRICS_ENABLED=false OTEL_TRACING_ENABLED=false docker compose up -d mysql backend frontend` | Start the app without LGTM/Alloy |
| `docker compose logs -f alloy` | Follow the log shipper |

OTLP export is enabled by default in Compose because LGTM is part of the stack.
If you intentionally skip LGTM/Alloy, disable it as shown above, otherwise the
backend will log connection errors while trying to reach the collector.

## How Compose fits in this project

`compose.yaml` is the only Compose file and the canonical way to run the
project. It builds the backend and frontend images, creates MySQL and wires all
services together on one network.

The root `.env` is the single local source for values used by Compose, Maven and
the Angular dev proxy. Compose overrides only the addresses that must change
inside the Docker network:

- backend: `DB_URL` → `mysql:3306`, OTLP endpoints → `grafana-lgtm:4318`
- frontend: `BACKEND_URL` → `http://backend:8089`

The root Compose file does **not** publish MySQL to the host. When running the
backend from Maven, use a locally installed MySQL on `localhost:3306`, or use
the full Compose stack instead.

### Services

| Service | Build / image | Container port | Host port | Notes |
| ------- | ------------- | -------------- | --------- | ----- |
| `mysql` | `mysql:8.4` | `3306` | – | Healthcheck, named volume `mysql-data` |
| `backend` | `./backend` | `8089` | `8089` | Waits for MySQL, healthcheck from its Dockerfile |
| `frontend` | `./frontend` | `8080` | `4200` | Waits for the backend, nginx proxies `/api` |
| `grafana-lgtm` | `grafana/otel-lgtm:latest` | `3000`, `4317`, `4318` | same | Grafana, Loki, Tempo, Prometheus, OTel collector |
| `alloy` | `grafana/alloy:latest` | `12345` internal | – | Reads Docker logs and pushes them to Loki |

The frontend is published on **4200** because Jenkins and many other tools use
`8080`. Change it with `FRONTEND_PORT` if needed. Inside the container nginx
always listens on `8080`.

## Configuration

The root `.env` is used by Compose, Maven and the Angular dev proxy. Start from
[`.env.example`](.env.example):

| Variable | Default | Used by | Purpose |
| -------- | ------- | ------- | ------- |
| `MYSQL_DATABASE` | `achatdb` | MySQL, backend | Database created at first start |
| `MYSQL_ROOT_PASSWORD` | `replace-with-a-random-root-password` | MySQL | Admin (`root`) password |
| `DB_USERNAME` | `achat_user` | MySQL, backend | Application account; must not be `root` |
| `DB_PASSWORD` | `replace-with-a-random-app-password` | MySQL, backend | Application password (use a different value from root) |
| `DB_URL` | `jdbc:mysql://localhost:3306/achatdb...` | Maven | JDBC URL for host-side runs; Compose overrides it to `mysql:3306` |
| `SERVER_PORT` | `8089` | Maven | API port for host-side runs |
| `JPA_DDL_AUTO` | `validate` | backend | Hibernate validates the Flyway-managed schema |
| `APP_SEED_ENABLED` | `true` | backend | Insert development data on startup |
| `SPRINGDOC_ENABLED` | `true` | backend | Enable Swagger/OpenAPI; set `false` in the cluster if not needed |
| `OTEL_METRICS_ENABLED` | `true` | backend | Export metrics over OTLP |
| `OTEL_TRACING_ENABLED` | `true` | backend | Export traces over OTLP |
| `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | `http://localhost:4318/v1/metrics` | Maven | Host-side metrics endpoint; Compose overrides it |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | `http://localhost:4318/v1/traces` | Maven | Host-side traces endpoint; Compose overrides it |
| `BACKEND_URL` | `http://localhost:8089` | Angular dev proxy | Host-side proxy target; Compose overrides it to `http://backend:8089` |
| `DNS_RESOLVER` | `127.0.0.11` | frontend container | Docker DNS; set the cluster DNS in Kubernetes |
| `FRONTEND_PORT` | `4200` | Compose | Host port for the web UI |
| `BACKEND_PORT` | `8089` | Compose | Host port for the API |
| `GRAFANA_PORT` | `3000` | Compose | Host port for Grafana |

There is no Spring profile to switch: the same image runs locally and in the
cluster, and only these environment values differ. Production values use the
same names through Kubernetes `ConfigMap`/`Secret`.

> **Database credentials are applied only once**, when the volume is first
> initialized. Editing `.env` later does not change existing MySQL accounts.
> Either run `ALTER USER` as root or delete the volume with
> `docker compose down -v` (this destroys all data).

For running the backend outside Compose, see
[`backend/README.md`](backend/README.md). Maven loads the same root `.env`
through `spring.config.import=optional:file:../.env[.properties]`.

## Observability

The local Compose stack runs Grafana LGTM and Alloy by default.

| Signal | Path | Backend |
| ------ | ---- | ------- |
| Metrics | backend → OTLP → LGTM collector | Prometheus |
| Traces | backend → OTLP → LGTM collector | Tempo |
| Logs | container stdout → Alloy → Loki | Loki |
| Profiles | not configured | Pyroscope (empty) |

Open <http://localhost:3000> and use `admin` / `admin`:

- **Dashboards** → `JVM Overview (OpenTelemetry)` or `RED Metrics`
- **Explore → Prometheus** → `{service_name="achat"}`
- **Explore → Tempo** → search service `achat`
- **Explore → Loki** → `{job="docker", service="backend"}`

The application exports metrics and traces itself. It does **not** export logs:
Spring Boot's OTLP logging requires an extra Logback appender, and that would
still miss nginx, MySQL and container crash output. Alloy collects Docker logs
locally and Kubernetes pod logs in the cluster. Profiling is intentionally out
of scope.

For EKS deployment, Grafana access, and example queries, see
[`observability/README.md`](observability/README.md).

## Local development with native tooling

**Requirements:** Java 21, MySQL 8+, Node.js 22.12+ (or 24+) and npm.

1. Create the root environment file once:

   ```bash
   cp .env.example .env
   ```

   Adjust `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` if you use an existing local
   MySQL. The same file is used by Maven and the Angular dev proxy.

2. Start the API from `backend/`:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

   Spring imports `../.env`, so no second backend env file is needed.

3. Start the UI in a second terminal:

   ```bash
   cd frontend
   npm install
   npm start
   ```

4. Open <http://localhost:4200>. Angular reads `BACKEND_URL` from the root
   `.env` through `frontend/proxy.conf.js` and proxies `/api` to
   `http://localhost:8089`.

Full details: [`backend/README.md`](backend/README.md) and
[`frontend/README.md`](frontend/README.md).

## API at a glance

Base URL: `http://localhost:8089` (no context path). Errors for unknown ids are
returned as `ProblemDetail` JSON with status `404`.

| Resource | Endpoints |
| -------- | --------- |
| Stocks | `GET/POST /stocks`, `GET/PUT/DELETE /stocks/{id}` |
| Products | `GET/POST /produits`, `GET/PUT/DELETE /produits/{id}`, `PUT /produits/{id}/stock/{stockId}` |
| Product categories | `GET/POST /categories-produits`, `GET/PUT/DELETE /categories-produits/{id}` |
| Suppliers | `GET/POST /fournisseurs`, `GET/PUT/DELETE /fournisseurs/{id}`, `PUT /fournisseurs/{id}/secteurs-activite/{secteurId}` |
| Business sectors | `GET/POST /secteurs-activite`, `GET/PUT/DELETE /secteurs-activite/{id}` |
| Operators | `GET/POST /operateurs`, `GET/PUT/DELETE /operateurs/{id}` |
| Payments | `GET/POST /reglements`, `GET/PUT/DELETE /reglements/{id}`, `GET /reglements/facture/{factureId}`, `GET /reglements/revenue?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd` |
| Invoices | `GET /facture/retrieve-all-factures`, `GET /facture/retrieve-facture/{id}`, `POST /facture/add-facture`, `PUT /facture/cancel-facture/{id}`, `GET /facture/getFactureByFournisseur/{fournisseurId}`, `PUT /facture/assignOperateurToFacture/{operateurId}/{factureId}`, `GET /facture/pourcentageRecouvrement/{startDate}/{endDate}` |

Interactive documentation is available at
<http://localhost:8089/swagger-ui/index.html>, and the OpenAPI document at
<http://localhost:8089/v3/api-docs>. Known API limitations are documented in the
[backend README](backend/README.md#known-api-limitations).

Write endpoints accept validated request DTOs rather than persistence entities.
Existing frontend field names are preserved; generated IDs and entity
relationships cannot be bound from the request body. Payment creation selects
an existing invoice using `factureId`.

## Testing and CI

Run the same checks as CI locally:

```bash
# Backend unit/integration tests (Testcontainers needs a running Docker daemon)
cd backend
./mvnw -B clean verify

# Frontend unit tests and production build
cd frontend
npm ci
npm test -- --watch=false --no-progress
npm run build
```

The [`Jenkinsfile`](Jenkinsfile) builds and tests the backend, publishes the
Surefire reports as JUnit results, then runs SonarQube, Nexus, Docker Hub and
the optional EKS deployment. Setup and usage: [`ci/README.md`](ci/README.md).
The frontend image can also run its tests without a local Node installation:

```bash
docker build --target builder -t achat-frontend:test frontend
docker run --rm achat-frontend:test npm test -- --watch=false --no-progress
```

The UI is published on **4200**, so a Jenkins instance on **8080** does not
conflict with the running stack.

## Deploy to AWS EKS

Continuous delivery targets an EKS cluster in the AWS Academy Learner Lab.
Two tools share the work:

| Tool | Job | When to run it |
| --- | --- | --- |
| **Terraform** ([`terraform/`](terraform/README.md)) | Creates the infrastructure: an EKS cluster with 2 nodes | Once per lab session (`terraform apply`), and `terraform destroy` at the end |
| **Jenkins** ([`Jenkinsfile`](Jenkinsfile), [`ci/`](ci/README.md)) | Builds, tests and packages the app, pushes the images to Docker Hub, deploys [`k8s/`](k8s) to the cluster | At every code change |

```
 git push ──► Jenkins: build ─► tests ─► Sonar ─► Nexus ─► Docker Hub ─► kubectl apply
                                                                           │
 terraform apply ──► EKS cluster (achat-eks) ◄─────────────────────────────┘
                         └─ frontend (public load balancer) ─► backend ─► mysql
```

In order:

1. **Run CI first** (no AWS needed): push your branch, run Jenkins with
   `DOCKER_PUSH` checked and `DEPLOY_K8S` unchecked. All stages must be green
   and the images must appear on Docker Hub.
   → [`ci/README.md`](ci/README.md#run-the-ci-build-test-sonar-nexus-docker-hub)
2. **Create the cluster**: Start Lab, copy the AWS credentials, `terraform apply`.
   → [`terraform/README.md`](terraform/README.md)
3. **Deploy**: add the AWS and database credentials to Jenkins, then run the
   pipeline with `DEPLOY_K8S` checked. Open the URL printed at the end of the log.
   → [`ci/README.md`](ci/README.md#continuous-delivery-docker-hub-and-eks)
4. **Clean up after every demo**: `kubectl delete namespace achat`, then
   `terraform destroy`. The cluster costs about $0.20/hour even when idle and
   the lab budget is $50.

Only the frontend is public; MySQL and the API stay internal to the cluster.

### What to show at validation

| Item | Where |
| --- | --- |
| Git history | GitHub repository, branch `Asma/terraform` |
| Jenkins | Green pipeline run with every stage |
| SonarQube | http://localhost:9000, project `achat` |
| Nexus | http://localhost:8081, `maven-snapshots` → `org/example/achat` |
| Docker Hub | `<user>/achat-backend` and `<user>/achat-frontend` with build-number tags |
| Kubernetes | `kubectl -n achat get pods,svc` and the application URL |

## Troubleshooting

- **`achat-mysql-1 is unhealthy` with `MYSQL_USER="root"` in the logs** -
  `DB_USERNAME` must be a non-root user such as `achat_user`. The MySQL image
  configures root only through `MYSQL_ROOT_PASSWORD`.
- **Backend cannot log in after changing `.env`** - the account already exists
  in the volume. Update it with `ALTER USER`, or reset with
  `docker compose down -v`.
- **`port is already allocated`** - change `FRONTEND_PORT`, `BACKEND_PORT` or
  `GRAFANA_PORT` in `.env`. Port `4200` can also conflict with a running
  `ng serve`.
- **"Cannot reach the server. Is the backend running?"** - check
  `docker compose ps` and `curl http://localhost:8089/actuator/health`; in local
  development check `BACKEND_URL` in `.env` and `frontend/proxy.conf.js`.
- **Frontend container starts but API calls fail** - check the nginx target:
  `docker compose exec frontend wget -qO- http://backend:8089/actuator/health`.
- **Database changes disappear after `down -v`** - that command deletes the
  `achat_mysql-data` volume by design. Use plain `docker compose down` to keep
  data.

## Security and production notes

The root Compose setup is configured for **local development**:

- `APP_SEED_ENABLED=true` loads the development data.
- `SPRINGDOC_ENABLED=true` exposes Swagger UI.
- The same unauthenticated security configuration is used everywhere; the API
  is only safe because it stays internal.
- Credentials are provided through a local `.env` file and default values are
  not secret.
- MySQL is not exposed to the host by the root Compose file, but the API and UI
  ports are.

The backend image has no baked-in profile. For the cluster, set
`APP_SEED_ENABLED=false`, decide whether you want Swagger
(`SPRINGDOC_ENABLED`), and point the OTLP endpoints at the in-cluster collector.
Use Flyway migrations with Hibernate `validate`. The API has no end-user
authentication, so it must stay internal behind nginx; only the frontend should
be public. If Swagger stays enabled, reach it with `kubectl port-forward`. Use a
dedicated database user, put credentials in a secret store, and terminate TLS at
the ingress/load balancer. Never commit a real `.env` file.

## Documentation

- [Frontend guide](frontend/README.md)
- [Backend guide](backend/README.md)
- [Docker Compose file](compose.yaml)
- [Environment template](.env.example)
- [Jenkins pipeline](Jenkinsfile)
- [Terraform / EKS guide](terraform/README.md)
