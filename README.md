# Achat

Spring Boot 4.1.1 application for purchasing, products, suppliers, invoices, and payments.

## Requirements

- Java 21
- MySQL 8+
- Docker and Docker Compose are optional

## Local setup

The application is currently intended to run locally from Maven while
connecting to a MySQL instance running on your machine or in an existing
development environment. The `compose.yaml` file is only a starter for local
experimentation with MySQL and the optional observability stack; it is not
required to run the application and is not a production deployment.

1. Create a local environment file:

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

3. Start the application from the project root:

   ```bash
   ./mvnw spring-boot:run
   ```

> **Setup note:** Spring Boot imports `.env` through
> `spring.config.import=optional:file:.env[.properties]`. Keep `.env` at the
> project root and do not commit it; it is ignored by Git. The `.env.example`
> file is only a safe template. The `.env` file activates the `dev` profile
> and enables the development data seeder.

The seed data is inserted during application startup. It is limited to the
`dev` profile and runs only when `APP_SEED_ENABLED=true`. The seeder is
idempotent: restarting the application does not duplicate existing records.
The database credentials in `.env` are separate from API authentication;
development API authentication is currently disabled.

To disable seeding, set `APP_SEED_ENABLED=false` in `.env` and restart the
application.

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

## Production

Do not use the `dev` profile, development seeding, root database credentials, or open API security in production. Provide production configuration through a secret manager or deployment environment and use a dedicated database user.
