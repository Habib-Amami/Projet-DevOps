# Achat

Spring Boot 4.1.1 application for purchasing, products, suppliers, invoices, and payments.

The repository contains two applications:

| Folder      | What it is                                    | Default URL             |
| ----------- | --------------------------------------------- | ----------------------- |
| `backend/`  | Spring Boot REST API (this README)            | <http://localhost:8089> |
| `frontend/` | Angular 21 + Angular Material web UI          | <http://localhost:4200> |

The frontend has its own guide in [`../frontend/README.md`](../frontend/README.md).

## Quick start (backend + frontend)

1. Start MySQL (locally installed, or `docker compose up -d mysql` from `backend/`).
2. Start the API from the `backend/` folder (see [Local setup](#local-setup)):

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

3. In a second terminal, start the web UI:

   ```bash
   cd frontend
   npm install
   npm start
   ```

4. Open <http://localhost:4200>. The Angular dev server forwards every `/api/...`
   request to the API on port 8089, so no CORS setup is needed.

## Requirements

- Java 21
- MySQL 8+
- Docker and Docker Compose are optional
- For the frontend: Node.js 22.12+ (or 20.19+ / 24+) and npm

## Local setup

The application is currently intended to run locally from Maven while
connecting to a MySQL instance running on your machine or in an existing
development environment. The `compose.yaml` file is only a starter for local
experimentation with MySQL and the optional observability stack; it is not
required to run the application and is not a production deployment.

1. Create a local environment file (inside `backend/`):

   ```bash
   cp .env.example .env
   ```

2. Configure the database values in `.env` to match your local MySQL server:

   ```dotenv
   DB_URL=jdbc:mysql://localhost:3306/mydatabase?serverTimezone=UTC
   DB_USERNAME=root
   DB_PASSWORD=your-local-password
   ```

   Create `mydatabase` first if it does not exist, or change `DB_URL` to an
   existing local database. `JPA_DDL_AUTO=update` allows Hibernate to create
   and update the development tables.

3. Start the application from the `backend/` folder:

   ```bash
   ./mvnw spring-boot:run
   ```

> **Setup note:** Spring Boot imports `.env` through
> `spring.config.import=optional:file:.env[.properties]`. Keep `.env` in the
> `backend/` folder (the folder you start Maven from) and do not commit it; it
> is ignored by Git. The `.env.example` file is only a safe template. The `.env`
> file activates the `dev` profile and enables the development data seeder.

The seed data is inserted during application startup. It is limited to the
`dev` profile and runs only when `APP_SEED_ENABLED=true`. The seeder is
idempotent: restarting the application does not duplicate existing records.
The database credentials in `.env` are separate from API authentication;
development API authentication is currently disabled.

To disable seeding, set `APP_SEED_ENABLED=false` in `.env` and restart the
application.

### Environment variables

| Variable                 | Default                 | Purpose                                   |
| ------------------------ | ----------------------- | ----------------------------------------- |
| `SPRING_PROFILES_ACTIVE` | `dev`                   | `dev` opens the API and enables seeding   |
| `SERVER_PORT`            | `8089`                  | HTTP port of the API                      |
| `DB_URL`                 | `jdbc:mysql://localhost:3306/achatdb?...` | JDBC URL                |
| `DB_USERNAME`            | `achat_user`            | Database user                             |
| `DB_PASSWORD`            | `change-me`             | Database password                         |
| `JPA_DDL_AUTO`           | `update`                | Hibernate schema strategy                 |
| `APP_SEED_ENABLED`       | `false`                 | Insert the development data on startup    |

### Optional Compose starter

The current Compose file is a development convenience and will evolve as the
infrastructure is configured. It currently defines a MySQL container and an
optional Grafana LGTM container, but it does not replace application
configuration or provide production hardening.

To use its MySQL starter instead of a locally installed MySQL server:

```bash
docker compose up -d mysql
```

Use matching values in `.env`:

```dotenv
DB_URL=jdbc:mysql://localhost:3306/mydatabase?serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=the-compose-password
```

If the container was already initialized with a persistent data volume,
changing `DB_PASSWORD` later does not change the existing MySQL root password.
For disposable development data, stop the container and remove its volume
before starting it again.

If you prefer to start the application with explicit environment variables
instead of importing `.env`, use:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

## API documentation and testing

- Swagger UI: <http://localhost:8089/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8089/v3/api-docs>
- Health: <http://localhost:8089/actuator/health>

In `dev`, API authentication is disabled for local testing. Import `postman/achat-api.postman_collection.json` into Postman. Copy `postman/achat-local.postman_environment.example.json` to a local environment file and update its variables if necessary.

### Endpoints

All paths are relative to `http://localhost:8089` (no context path). Errors for
unknown ids are returned as `ProblemDetail` JSON with status `404`.

| Resource           | Endpoints                                                                                     |
| ------------------ | --------------------------------------------------------------------------------------------- |
| Stocks             | `GET/POST /stocks`, `GET/PUT/DELETE /stocks/{id}`                                              |
| Products           | `GET/POST /produits`, `GET/PUT/DELETE /produits/{id}`, `PUT /produits/{id}/stock/{stockId}`    |
| Product categories | `GET/POST /categories-produits`, `GET/PUT/DELETE /categories-produits/{id}`                    |
| Suppliers          | `GET/POST /fournisseurs`, `GET/PUT/DELETE /fournisseurs/{id}`, `PUT /fournisseurs/{id}/secteurs-activite/{secteurId}` |
| Business sectors   | `GET/POST /secteurs-activite`, `GET/PUT/DELETE /secteurs-activite/{id}`                        |
| Operators          | `GET/POST /operateurs`, `GET/PUT/DELETE /operateurs/{id}`                                      |
| Payments           | `GET/POST /reglements`, `GET/PUT/DELETE /reglements/{id}`, `GET /reglements/facture/{factureId}`, `GET /reglements/revenue?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd` |
| Invoices           | `GET /facture/retrieve-all-factures`, `GET /facture/retrieve-facture/{id}`, `POST /facture/add-facture`, `PUT /facture/cancel-facture/{id}`, `GET /facture/getFactureByFournisseur/{fournisseurId}`, `PUT /facture/assignOperateurToFacture/{operateurId}/{factureId}`, `GET /facture/pourcentageRecouvrement/{startDate}/{endDate}` |

Request bodies use the entity field names (for example a stock is
`{"libelleStock": "Main", "qte": 10, "qteMin": 2}`). Dates are sent as
`yyyy-MM-dd`.

### Known API limitations

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

## Troubleshooting

- **`LazyInitializationException` / `500` on suppliers, operators or invoices:**
  `spring.jpa.open-in-view` is disabled, so collections that are serialized
  after the service returns must be fetched up front. The repositories use
  `@EntityGraph` for this (`FournisseurRepository`, `OperateurRepository`,
  `FactureRepository`). Keep this in mind when adding new read methods.
- **Port 8089 already in use:** another instance is running; stop it or change
  `SERVER_PORT` in `.env`.
- **Access denied for user:** the credentials in `.env` do not match your MySQL
  server.

## Production

Do not use the `dev` profile, development seeding, root database credentials, or open API security in production. Provide production configuration through a secret manager or deployment environment and use a dedicated database user.
