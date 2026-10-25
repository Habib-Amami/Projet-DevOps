# Achat - Backend

Spring Boot REST API for the Achat purchasing application. It manages products,
stocks, product categories, suppliers, business sectors, operators, invoices and
payments, and exposes the API consumed by the [Angular frontend](../frontend).

- Spring Boot 4.1.1 on Java 21
- MySQL 8+ with Spring Data JPA/Hibernate
- Flyway database migrations with Hibernate schema validation
- OpenAPI/Swagger documentation, Actuator health/metrics
- Idempotent development seeder controlled by `APP_SEED_ENABLED`
- Unit and slice tests with JUnit, Mockito and Testcontainers

- [Features](#features)
- [Tech stack](#tech-stack)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Local development](#local-development)
- [Configuration](#configuration)
- [Database and seed data](#database-and-seed-data)
- [API documentation](#api-documentation)
- [Endpoints](#endpoints)
- [Known API limitations](#known-api-limitations)
- [Testing](#testing)
- [Docker](#docker)
- [Project structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Production](#production)

## Features

- CRUD REST resources for stocks, products, categories, suppliers, sectors,
  operators and payments.
- Invoice workflow: create, retrieve, cancel/archive, list by supplier, assign
  an operator and compute the recovery rate for a period.
- DTO responses and `@EntityGraph` fetches for lazy relations (open-in-view is
  disabled).
- Bean Validation and `ProblemDetail` error responses (`404` for unknown ids).
- OpenAPI document and Swagger UI via springdoc.
- Actuator health, info, metrics and Prometheus endpoints.
- Idempotent development seeding from JSON resources.

## Tech stack

| Area | Technology |
| ---- | ---------- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation, Actuator) |
| Database | MySQL 8+ (`mysql-connector-j`), Flyway migrations |
| API docs | springdoc OpenAPI 3.1.0 + Swagger UI |
| Observability | Micrometer, Prometheus registry, Spring Boot OpenTelemetry |
| Build | Maven Wrapper (`./mvnw`) |
| Tests | JUnit 5, Mockito, Spring Boot Test, Testcontainers (MySQL, Grafana LGTM) |
| Runtime image | Multi-stage `eclipse-temurin:21-jdk-alpine` → `21-jre-alpine`, non-root |

## Requirements

- Java 21 (for running from Maven).
- MySQL 8+ (locally installed or in Docker).
- Docker, for the Compose workflow and for Testcontainers during tests.
- Node.js 22.12+ only if you also run the frontend.

## Quick start

### Full stack with Docker Compose (recommended)

From the **repository root**:

```bash
cp .env.example .env    # set MYSQL_ROOT_PASSWORD and DB_PASSWORD
docker compose up -d --build
```

This starts MySQL, the backend on <http://localhost:8089>, the frontend on
<http://localhost:4200>, and the local observability stack (Grafana LGTM +
Alloy). Grafana is at <http://localhost:3000> (`admin` / `admin`). Configuration
and service details are explained in the
[root README](../README.md), especially
[How Compose fits](../README.md#how-compose-fits-in-this-project).

### Backend only, from Maven

Create the root environment file once:

```bash
cp .env.example .env    # configure DB_URL, DB_USERNAME, DB_PASSWORD
```

Then run from `backend/`:

```bash
cd backend
./mvnw spring-boot:run
```

Spring imports `../.env`, so there is no backend-specific env file.

## Local development

The Maven path needs a MySQL instance reachable on `localhost:3306`. Use an
existing local server, or use the full Compose stack instead.

### Option A - existing local MySQL server

Create the database and an application user:

```sql
CREATE DATABASE achatdb CHARACTER SET utf8mb4;
CREATE USER 'achat_user'@'localhost' IDENTIFIED BY 'a-strong-password';
GRANT ALL PRIVILEGES ON achatdb.* TO 'achat_user'@'localhost';
```

Then adjust the root `.env`:

```dotenv
SERVER_PORT=8089
DB_URL=jdbc:mysql://localhost:3306/achatdb?serverTimezone=UTC
DB_USERNAME=achat_user
DB_PASSWORD=a-strong-password
JPA_DDL_AUTO=validate
APP_SEED_ENABLED=true
SPRINGDOC_ENABLED=true
```

`root` is also acceptable on a **locally installed** MySQL for development, but
never in the root Compose file (the MySQL image reserves root for
`MYSQL_ROOT_PASSWORD`).

### Option B - full Compose stack

If you do not have a local MySQL server, use the root Compose stack:

```bash
docker compose up -d --build
```

The backend then runs in its container. The root Compose file does not publish
MySQL to the host, so Maven cannot use the containerized database.

### Start the API and frontend

With a host MySQL configured in root `.env`:

```bash
./mvnw spring-boot:run
```

Optionally start the frontend in another terminal:

```bash
cd ../frontend
npm install
npm start
```

Open <http://localhost:4200>; the dev proxy reads `BACKEND_URL` from the root
`.env` and proxies `/api` to port 8089.

> **Setup note:** Spring Boot imports the root file through
> `spring.config.import=optional:file:../.env[.properties]`. Run Maven from
> `backend/` and never commit `.env`.

If you prefer exported environment variables:

```bash
set -a
source ../.env
set +a
./mvnw spring-boot:run
```

## Configuration

The local values live in the repository-root `.env`, which is shared with
Compose and the Angular dev proxy. The defaults below are fallbacks used when a
variable is not set.

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `SERVER_PORT` | `8089` | HTTP port of the API |
| `DB_URL` | `jdbc:mysql://localhost:3306/achatdb?serverTimezone=UTC` | JDBC URL |
| `DB_USERNAME` | `achat_user` | Database account |
| `DB_PASSWORD` | `change-me` | Database password |
| `JPA_DDL_AUTO` | `validate` | Hibernate schema strategy; Flyway owns schema changes |
| `APP_SEED_ENABLED` | `false` | Insert development data on startup |
| `SPRINGDOC_ENABLED` | `false` | Enable Swagger UI and the OpenAPI document |
| `OTEL_METRICS_ENABLED` | `false` | Export metrics over OTLP |
| `OTEL_TRACING_ENABLED` | `false` | Export traces over OTLP |
| `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | `http://localhost:4318/v1/metrics` | OTLP metrics endpoint |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP traces endpoint |

When using the root Compose stack, MySQL is bootstrapped from `DB_USERNAME` and
`DB_PASSWORD`, and the backend receives the same values plus a containerised
`DB_URL`. Root and application credentials must be different, and
`DB_USERNAME` must not be `root` (the MySQL image reserves root for
`MYSQL_ROOT_PASSWORD`).

API security permits all requests. There is one `SecurityConfig`: it disables the
generated Spring Security password, form login, HTTP Basic and sessions. The API
has no end-user authentication, so the backend must remain internal
(`ClusterIP`) behind the nginx frontend. Swagger is enabled locally with
`SPRINGDOC_ENABLED=true`; set it to `true` in the cluster only if you still want
to reach it with `kubectl port-forward`.

## Database and seed data

- Flyway owns the schema: `db/migration/V1__init.sql` is the baseline and future
  changes are added as `V2__...sql`.
- Hibernate runs with `ddl-auto=validate`, so an entity/schema mismatch stops
  startup instead of silently changing the database.
- Existing databases created by the old Hibernate `update` setup are baselined
  at `V1` (`baseline-on-migrate`); fresh databases run `V1` normally.
- Seeding runs only when `APP_SEED_ENABLED=true`; it is enabled by the root
  Compose stack and disabled by default.
- Seed files live in `src/main/resources/seed/` (`stocks.json`,
  `products.json`, `categories.json`, `fournisseurs.json`, `secteurs.json`,
  `operateurs.json`, `factures.json`) and are loaded by
  `config/DevelopmentDataSeeder`.
- The seeder is idempotent: restarting the application does not duplicate rows.
- Disable it with `APP_SEED_ENABLED=false` and restart. Do not enable it in the
  cluster unless you deliberately want demo data.
- With Compose, credentials and database contents persist in the
  `achat_mysql-data` volume. `docker compose down -v` deletes that volume and
  re-runs initialization (including seeding) on the next start.

## API documentation

| Resource | URL |
| -------- | --- |
| Swagger UI | <http://localhost:8089/swagger-ui/index.html> |
| OpenAPI JSON | <http://localhost:8089/v3/api-docs> |
| Actuator health | <http://localhost:8089/actuator/health> |
| Prometheus metrics | <http://localhost:8089/actuator/prometheus> |

A Postman collection is available in
[`postman/achat-api.postman_collection.json`](postman/achat-api.postman_collection.json).
Copy `postman/achat-local.postman_environment.example.json` to a local
environment file and adjust its variables if necessary.

## Observability

- Metrics and traces are exported over OTLP when `OTEL_METRICS_ENABLED` and
  `OTEL_TRACING_ENABLED` are `true`.
- Logs are **not** exported by the application. The root Compose stack runs
  Alloy, which reads container stdout/stderr and pushes it to Loki.
- The root README has the Grafana URLs and example queries.
- Profiling (Pyroscope) is not configured.

## Endpoints

All paths are relative to `http://localhost:8089` (no context path). Errors for
unknown ids are returned as `ProblemDetail` JSON with status `404`.

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

Request bodies use the entity field names (for example a stock is
`{"libelleStock": "Main", "qte": 10, "qteMin": 2}`). Dates are sent as
`yyyy-MM-dd`.

## Known API limitations

- `POST`/`PUT /reglements` always answer `400`: `Reglement.facture` is required
  (`@NotNull`) but ignored in JSON (`@JsonIgnore`), so a payment cannot be
  created or edited through the API. The web UI therefore only lists, shows and
  deletes payments.
- Invoices have no update or delete endpoint; they can only be cancelled
  (archived). Invoice lines and the supplier of an invoice cannot be set
  through `add-facture`.
- A product's category cannot be set through the API (`categorieProduit` is
  ignored in JSON); only its stock can be assigned.
- Creating a supplier requires `detailFournisseur.adresse` and
  `detailFournisseur.matricule`. On update, omitting `detailFournisseur` keeps
  the existing details.
- An operator's `password` is required on create **and** on update, and is
  never returned.
- `retrieve-facture/{id}` answers `200` with an empty body for an unknown id.

## Testing

```bash
# Unit and slice tests
./mvnw -B test

# Full verification, as used by CI
./mvnw -B clean verify
```

Testcontainers starts MySQL and Grafana LGTM containers for the context test, so
a running Docker daemon is required. Surefire XML reports are written to
`target/surefire-reports/` and published by the Jenkins pipeline. The Docker
image build skips tests (`-DskipTests`) because CI already runs them.

## Docker

The [`Dockerfile`](./Dockerfile) is a multi-stage build:

1. Maven compiles the application and explodes the Spring Boot fat JAR into
   layers.
2. The runtime stage uses a minimal JRE, runs as an unprivileged user and copies
   the layers in cache-friendly order.

```bash
docker build -t achat-backend .
```

The image listens on `8089` and exposes `/actuator/health` for its healthcheck.
It has no baked-in Spring profile or environment-specific defaults. It needs a
reachable MySQL and receives its configuration through environment variables;
the root [`compose.yaml`](../compose.yaml) wires it to the `mysql`, `grafana-lgtm`
and `alloy` services automatically.

## Project structure

```
src/
  main/
    java/org/example/achat/
      AchatApplication.java
      config/              security config, development data seeder
      stock/               controllers, services, repositories, entities,
      produit/             dto and exception handlers for each domain
      fournisseurs/
      operateur/
      facture/
      reglements/
    resources/
      application.properties   single configuration; environment drives values
      db/migration/            Flyway migrations (V1__init.sql)
      seed/                    JSON data used by the development seeder
  test/
    java/org/example/achat/
      TestcontainersConfiguration.java   MySQL + LGTM test services
      ...                                service and controller tests
```

Each domain follows the same layers: `controllers` → `services` →
`repositories` → `entities`, with `dto` responses and `exceptions` mapped to
`ProblemDetail` responses.

## Troubleshooting

- **`LazyInitializationException` / `500` on suppliers, operators or
  invoices:** `spring.jpa.open-in-view` is disabled, so collections serialized
  after the service returns must be fetched up front. Repositories use
  `@EntityGraph` (`FournisseurRepository`, `OperateurRepository`,
  `FactureRepository`); keep this in mind when adding read methods.
- **Port 8089 already in use:** another instance is running; stop it or change
  `SERVER_PORT`.
- **`Access denied for user`:** the credentials in the root `.env` do not match
  the account in MySQL. Remember that credentials are only created on first
  database initialization.
- **Compose reports `MYSQL_USER="root"`:** set `DB_USERNAME=achat_user` in the
  root `.env`; the MySQL image cannot create root as a regular user.
- **Tests fail to start Testcontainers:** make sure the Docker daemon is
  running and accessible to the Jenkins agent/user.
- **Changes to seed files do not appear:** the seeder is idempotent and skips
  existing records. Reset the volume (`docker compose down -v`) or update the
  rows manually.

## Production

There is no separate production Spring profile. The same image runs locally and
in the cluster; only environment values differ:

```text
APP_SEED_ENABLED=false
SPRINGDOC_ENABLED=false          # true only if you want port-forward Swagger
OTEL_METRICS_ENABLED=true
OTEL_TRACING_ENABLED=true
OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=http://grafana-lgtm:4318/v1/metrics
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://grafana-lgtm:4318/v1/traces
```

Logs are collected by the Alloy DaemonSet from pod stdout, so the application
does not export logs over OTLP. Profiling is intentionally not configured.

Because the API itself is unauthenticated, it must only be reachable inside the
cluster: expose the frontend nginx service, keep the backend `ClusterIP`, and
reach Swagger through `kubectl port-forward` only if `SPRINGDOC_ENABLED=true`.
Provide database credentials through the deployment environment or a secret
store, use a dedicated database user with least privilege, and terminate TLS at
the ingress/load balancer. The root Compose setup remains a local development
environment, not a production deployment.
