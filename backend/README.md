# Achat - Backend

Spring Boot REST API for the Achat purchasing application. It manages products,
stocks, product categories, suppliers, business sectors, operators, invoices and
payments, and exposes the API consumed by the [Angular frontend](../frontend).

- Spring Boot 4.1.1 on Java 21
- MySQL 8+ with Spring Data JPA/Hibernate
- OpenAPI/Swagger documentation, Actuator health/metrics
- Development seeder and open security in the `dev` profile
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
| Database | MySQL 8+ (`mysql-connector-j`) |
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

This starts MySQL, the backend on <http://localhost:8089> and the frontend on
<http://localhost:4200>. Configuration, service details and the two Compose
files are explained in the [root README](../README.md), especially
[How Compose fits](../README.md#how-compose-fits-in-this-project).

### Backend only, from Maven

```bash
cd backend
cp .env.example .env    # configure DB_URL, DB_USERNAME, DB_PASSWORD
./mvnw spring-boot:run
```

Then continue with [Local development](#local-development) for the database and
frontend setup.

## Local development

The backend runs from Maven against a MySQL instance reachable from the host.
Choose one of the two database options below.

### Option A - helper Compose file (MySQL in Docker)

```bash
cd backend
docker compose up -d mysql
cp .env.example .env
```

The helper creates the `mydatabase` database and uses the credentials from
`backend/.env` (the same file Spring Boot reads):

- `DB_USERNAME=root` and `DB_PASSWORD=change-me` by default;
- `DB_URL=jdbc:mysql://localhost:3306/mydatabase?serverTimezone=UTC`.

These defaults are for disposable local development only. The helper also
defines an optional Grafana LGTM container
(`docker compose --profile observability up -d`). It is intended for running the
applications from Maven/Angular, not as the full stack; use the root
`compose.yaml` for that.

### Option B - existing local MySQL server

Create the database and an application user:

```sql
CREATE DATABASE achatdb CHARACTER SET utf8mb4;
CREATE USER 'achat_user'@'localhost' IDENTIFIED BY 'a-strong-password';
GRANT ALL PRIVILEGES ON achatdb.* TO 'achat_user'@'localhost';
```

Then create the backend environment file and adjust it:

```bash
cp .env.example .env
```

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8089
DB_URL=jdbc:mysql://localhost:3306/achatdb?serverTimezone=UTC
DB_USERNAME=achat_user
DB_PASSWORD=a-strong-password
JPA_DDL_AUTO=update
APP_SEED_ENABLED=true
```

`root` is also acceptable on a **locally installed** MySQL for development, but
never in the root Compose file (the MySQL image reserves root for
`MYSQL_ROOT_PASSWORD`).

### Start the API and frontend

```bash
./mvnw spring-boot:run
```

Optionally start the frontend in another terminal:

```bash
cd ../frontend
npm install
npm start
```

Open <http://localhost:4200>; the dev server proxies `/api` to port 8089.

   Open <http://localhost:4200>; the dev server proxies `/api` to port 8089.

> **Setup note:** Spring Boot imports the file through
> `spring.config.import=optional:file:.env[.properties]`. Keep `.env` in the
> `backend/` folder (the directory you start Maven from) and never commit it.
> The root `.env` used by Compose is a separate file with different variables.

If you prefer environment variables over the `.env` import:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

## Configuration

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` opens the API and enables the seeder |
| `SERVER_PORT` | `8089` | HTTP port of the API |
| `DB_URL` | `jdbc:mysql://localhost:3306/achatdb?serverTimezone=UTC` | JDBC URL |
| `DB_USERNAME` | `achat_user` | Database account |
| `DB_PASSWORD` | `change-me` | Database password |
| `JPA_DDL_AUTO` | `update` | Hibernate schema strategy (`update` for dev) |
| `APP_SEED_ENABLED` | `false` | Insert development data on startup |

When using the root Compose stack, MySQL is bootstrapped from `DB_USERNAME` and
`DB_PASSWORD`, and the backend receives the same values plus a containerised
`DB_URL`. Root and application credentials must be different, and
`DB_USERNAME` must not be `root` (the MySQL image reserves root for
`MYSQL_ROOT_PASSWORD`).

API security is disabled by `DevelopmentSecurityConfig` when the `dev` profile
is active. There is no authentication or authorization in this mode.

## Database and seed data

- Hibernate creates/updates the schema according to `JPA_DDL_AUTO`.
- Seeding runs only with the `dev` profile **and** `APP_SEED_ENABLED=true`.
- Seed files live in `src/main/resources/seed/` (`stocks.json`,
  `products.json`, `categories.json`, `fournisseurs.json`, `secteurs.json`,
  `operateurs.json`, `factures.json`) and are loaded by
  `config/DevelopmentDataSeeder`.
- The seeder is idempotent: restarting the application does not duplicate rows.
- Disable it with `APP_SEED_ENABLED=false` and restart.
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

The image listens on `8089`, exposes `/actuator/health` for its healthcheck and
uses `SPRING_PROFILES_ACTIVE=dev` by default. It needs a reachable MySQL using
the configuration variables above; the root [`compose.yaml`](../compose.yaml)
wires it to the `mysql` service automatically.

## Project structure

```
src/
  main/
    java/org/example/achat/
      AchatApplication.java
      config/              development security, data seeder
      stock/               controllers, services, repositories, entities,
      produit/             dto and exception handlers for each domain
      fournisseurs/
      operateur/
      facture/
      reglements/
    resources/
      application.properties
      application-dev.properties
      seed/                JSON data used by the development seeder
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
- **`Access denied for user`:** the credentials in `.env` (or the Compose
  `.env`) do not match the account in MySQL. Remember that credentials are only
  created on first database initialization.
- **Compose reports `MYSQL_USER="root"`:** set `DB_USERNAME=achat_user` in the
  root `.env`; the MySQL image cannot create root as a regular user.
- **Tests fail to start Testcontainers:** make sure the Docker daemon is
  running and accessible to the Jenkins agent/user.
- **Changes to seed files do not appear:** the seeder is idempotent and skips
  existing records. Reset the volume (`docker compose down -v`) or update the
  rows manually.

## Production

Do not use the `dev` profile, development seeding, shared/root database
credentials, or open API security in production. Provide configuration through a
secret manager or deployment environment, use a dedicated database user with
least privilege, enable authentication/authorization, and terminate TLS at the
edge. The current Compose setup is a development environment, not a production
deployment.
