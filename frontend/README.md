# Achat - Frontend

Angular single-page application for the Achat purchasing API. It covers invoices,
payments, products, product categories, stocks, suppliers, business sectors and
operators.

Generated with Angular CLI 21.2 and built with standalone components, signals,
Reactive Forms and Angular Material.

- [Features](#features)
- [Tech stack](#tech-stack)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Docker](#docker)
- [Available scripts](#available-scripts)
- [Project structure](#project-structure)
- [Screens and capabilities](#screens-and-capabilities)
- [Testing](#testing)
- [Formatting and scaffolding](#formatting-and-scaffolding)
- [Troubleshooting](#troubleshooting)
- [Related documentation](#related-documentation)

## Features

- Material shell with a responsive toolbar and side navigation.
- Lazy-loaded feature routes: invoices, payments, products, categories, stocks,
  suppliers, business sectors and operators.
- Typed services for every backend resource, shared CRUD base class and
  centralized API error handling.
- Forms with validators mirroring the backend, confirmation dialogs,
  loading/empty/error states and reusable table/statistic components.
- Invoice recovery rate, payment revenue over a period, "below minimum" stock
  flag, supplier invoices and sector assignment.
- No login page: the backend runs without authentication in its `dev` profile.

## Tech stack

| Area | Technology |
| ---- | ---------- |
| Framework | Angular 21.2 (standalone components, signals) |
| Language | TypeScript 5.9 |
| UI | Angular Material 21 + Angular CDK, SCSS |
| Reactive | RxJS 7.8 |
| Tests | Vitest 4 + jsdom (Angular unit-test builder) |
| Tooling | Angular CLI/build, Prettier, npm |
| Runtime image | Node 24 build stage + unprivileged nginx |

## Requirements

- Node.js 22.12+ (or 20.19+ / 24+) and npm.
- A running backend on <http://localhost:8089>, started from
  [`../backend`](../backend/README.md) or with the root
  [Docker Compose stack](../README.md#quick-start-with-docker-compose).

## Quick start

### Full stack with Docker Compose (from the repository root)

```bash
docker compose up -d --build
```

Open <http://localhost:4200>. This builds the UI, the API and MySQL together;
see the [root README](../README.md) for configuration and commands.

### Local development

Start the backend first, then:

```bash
npm install
npm start          # same as: ng serve
```

Open <http://localhost:4200>. The application reloads automatically when a
source file changes.

The dev server reads `proxy.conf.json` and forwards `/api/*` to
`http://localhost:8089/*`, stripping the `/api` prefix. No CORS setup is
needed.

Run **only one** `ng serve` for this folder at a time: two dev servers share the
`.angular/cache` folder and can corrupt each other's pre-bundled dependencies
(see [Troubleshooting](#troubleshooting)).

## Configuration

| File | Used by | `apiUrl` |
| ---- | ------- | -------- |
| `src/environments/environment.development.ts` | `ng serve`, development builds | `/api` |
| `src/environments/environment.ts` | `ng build` (production) | `/api` |

All HTTP calls go through `apiUrl`. During development `proxy.conf.json`
forwards `/api/*` to `http://localhost:8089/*`. To target another backend,
change `target` in `proxy.conf.json`.

In production the app is served behind nginx (see [Docker](#docker)), which
forwards `/api` to the backend the same way. Alternatively, set `apiUrl` to the
backend's full URL, in which case the backend must allow CORS for that origin.

## Docker

The [`Dockerfile`](./Dockerfile) builds the production bundle with Node and
serves it with unprivileged nginx:

```bash
docker build -t achat-frontend .

# Backend reachable as "backend:8089" on a shared Docker network:
docker run --rm -p 4200:8080 \
  -e BACKEND_URL=http://backend:8089 \
  achat-frontend
```

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `BACKEND_URL` | `http://backend:8089` | Origin `/api/*` is proxied to |
| `DNS_RESOLVER` | `127.0.0.11` (Docker) | DNS server used to resolve `BACKEND_URL` |

Behaviour:

- The container listens on `8080`; Compose publishes it on `4200`
  (`FRONTEND_PORT`) so it does not collide with Jenkins on `8080`.
- `/api/*` is proxied with the `/api` prefix stripped and a same-origin
  browser request, matching `proxy.conf.json`.
- Unknown paths fall back to `index.html`, so Angular deep links survive a
  refresh.
- Hashed JS/CSS assets are cached for a year; `index.html` is always
  revalidated.
- The backend hostname is resolved per request, so nginx starts even when the
  backend is down. On Kubernetes, set `DNS_RESOLVER` to the cluster DNS.
- `GET /healthz` is available for container health checks.

Use the root [`compose.yaml`](../compose.yaml) to run this image with the backend
and MySQL. The Angular tests can run in the builder stage without Node on the
host:

```bash
docker build --target builder -t achat-frontend:test .
docker run --rm achat-frontend:test npm test -- --watch=false --no-progress
```

## Available scripts

| Command | Description |
| ------- | ----------- |
| `npm start` / `npm run ng serve` | Development server on port 4200 |
| `npm run build` | Production build in `dist/frontend/browser` |
| `npm run watch` | Development build in watch mode |
| `npm test` | Unit tests with Vitest (watch mode) |
| `npm test -- --watch=false --no-progress` | Single test run (used by CI) |
| `npm run ng` | Angular CLI passthrough |

## Project structure

```
src/
  main.ts                 bootstrap entry point
  styles.scss             global Material/theme styles
  environments/           development and production `apiUrl`
  app/
    app.config.ts         router, HttpClient + interceptor, date adapter
    app.routes.ts         shell layout + one lazy-loaded route file per feature
    core/
      api/                ApiError, error interceptor, CrudService, LoadState
      layout/             shell: toolbar + responsive side navigation
      models/             TypeScript interfaces matching backend DTOs/entities
      services/           one typed service per resource + NotificationService
    shared/
      components/         data table, loader, error/empty state, page header,
                          date-range statistic card
      dialogs/            confirmation dialog + ConfirmService
      forms/              validators mirroring the backend and error messages
      utils/              date helpers (`yyyy-MM-dd` for the API)
    features/
      factures/           invoices: list, recovery rate, create, detail,
                          cancel, assign operator
      reglements/         payments: list, filter by invoice, revenue,
                          detail, delete
      produits/           products: CRUD + stock assignment
      categories-produits/ product categories: CRUD
      stocks/             stocks: CRUD
      fournisseurs/       suppliers: CRUD + sectors + invoices
      secteurs-activite/  business sectors: CRUD
      operateurs/         operators: CRUD
      not-found/          404 page
```

Each feature folder has a `*.routes.ts` file with `''` (list), `new` (create),
`:id` (detail) and `:id/edit` (edit) routes, depending on what the API allows.

## Screens and capabilities

| Screen | List | Detail | Create | Edit | Delete | Extras |
| ------ | :--: | :----: | :----: | :--: | :----: | ------ |
| Invoices | ✓ | ✓ | ✓ | – | cancel | recovery rate, lines, payments, assign operator |
| Payments | ✓ | ✓ | – | – | ✓ | filter by invoice, revenue over a period |
| Products | ✓ | ✓ | ✓ | ✓ | ✓ | assign a stock |
| Product categories | ✓ | ✓ | ✓ | ✓ | ✓ | |
| Stocks | ✓ | ✓ | ✓ | ✓ | ✓ | "below minimum" flag |
| Suppliers | ✓ | ✓ | ✓ | ✓ | ✓ | assign business sectors, invoices list |
| Business sectors | ✓ | ✓ | ✓ | ✓ | ✓ | |
| Operators | ✓ | ✓ | ✓ | ✓ | ✓ | |

Missing actions (–) are not exposed by the backend; see
[Known API limitations](../backend/README.md#known-api-limitations).

## Testing

```bash
# watch mode
npm test

# single run, as in Jenkins
npm test -- --watch=false --no-progress
```

Tests use Vitest with jsdom; specs live next to the code (`*.spec.ts`). Build
the production bundle with:

```bash
npm run build
```

Artifacts are written to `dist/frontend/browser`.

## Formatting and scaffolding

Formatting uses Prettier (`.prettierrc`):

```bash
npx prettier --write "src/**/*.{ts,html,scss}"
```

Generate Angular artifacts with the CLI:

```bash
npm run ng -- generate component component-name
npm run ng -- generate --help
```

## Troubleshooting

- **`mat-form-field must contain a MatFormFieldControl` (dev server only):**
  the Vite dependency cache is out of sync, usually after installing packages
  or running two `ng serve` processes. Stop all dev servers, delete the cache
  and restart, then hard-refresh the browser:

  ```bash
  rm -rf .angular/cache
  npm start
  ```

- **"Cannot reach the server. Is the backend running?"** - start the backend on
  port 8089 (or the Compose stack) and check `proxy.conf.json`.
- **Port 4200 already in use** - run `npm start -- --port 4300`, or change
  `FRONTEND_PORT` when using Compose.
- **404 after refreshing a deep link** - the reverse proxy must fall back to
  `index.html`. The provided nginx configuration already does this.
- **Container healthcheck fails** - check `docker compose logs frontend` and
  verify `wget -qO- http://127.0.0.1:8080/healthz` inside the container.

## Related documentation

- [Root README - full project and Compose](../README.md)
- [Backend README](../backend/README.md)
- [Angular CLI documentation](https://angular.dev/tools/cli)
