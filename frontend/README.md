# Achat – Frontend

Angular web UI for the Achat purchasing API (`../backend`). It covers invoices,
payments, products, product categories, stocks, suppliers, business sectors and
operators.

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.26
and uses standalone components, signals, Reactive Forms and Angular Material.

## Prerequisites

- Node.js 22.12+ (or 20.19+ / 24+) and npm
- The backend running on <http://localhost:8089> (see [`../backend/README.md`](../backend/README.md))

## Install and run

```bash
npm install
npm start          # same as: ng serve
```

Open <http://localhost:4200/>. The application reloads automatically when you
modify a source file.

Run **only one** `ng serve` for this folder at a time: two dev servers share the
`.angular/cache` folder and can corrupt each other's pre-bundled dependencies
(see [Troubleshooting](#troubleshooting)).

## Environment configuration

| File                                        | Used by                    | `apiUrl` |
| ------------------------------------------- | -------------------------- | -------- |
| `src/environments/environment.development.ts` | `ng serve`, dev builds   | `/api`   |
| `src/environments/environment.ts`            | `ng build` (production)  | `/api`   |

All HTTP calls go to `apiUrl`. During development `proxy.conf.json` forwards
`/api/*` to `http://localhost:8089/*` (stripping the `/api` prefix). To target
another backend, change `target` in `proxy.conf.json`.

In production, serve the built files behind a reverse proxy that forwards
`/api` to the backend the same way, or set `apiUrl` to the backend's URL (the
backend then needs CORS for that origin).

The API has no authentication in the `dev` profile, so the UI has no login page.

## Project structure

```
src/app/
  app.config.ts        providers: router, HttpClient + interceptor, date adapter
  app.routes.ts        shell layout + one lazy-loaded route file per feature
  core/
    api/               ApiError, error interceptor, CrudService base, LoadState helper
    layout/            Shell: toolbar + responsive side navigation
    models/            TypeScript interfaces matching the backend DTOs/entities
    services/          one typed service per backend resource + NotificationService
  shared/
    components/        data table, loader, error message, empty state, async state,
                       page header, date-range statistic card
    dialogs/           confirmation dialog + ConfirmService
    forms/             validators mirroring the backend (notBlank, integer), error messages
    utils/             date helpers (yyyy-MM-dd for the API)
  features/
    factures/          invoices: list, recovery rate, create, detail, cancel, assign operator
    reglements/        payments: list, filter by invoice, revenue, detail, delete
    produits/          products: CRUD + stock assignment
    categories-produits/  product categories: CRUD
    stocks/            stocks: CRUD
    fournisseurs/      suppliers: CRUD + assign business sectors + their invoices
    secteurs-activite/ business sectors: CRUD
    operateurs/        operators: CRUD
    not-found/         404 page
```

Each feature folder has a `*.routes.ts` file with `''` (list), `new` (create),
`:id` (detail) and `:id/edit` (edit) routes, depending on what the API allows.

## What each screen can do

| Screen             | List | Detail | Create | Edit | Delete | Extras                                   |
| ------------------ | :--: | :----: | :----: | :--: | :----: | ---------------------------------------- |
| Invoices           | ✓    | ✓      | ✓      | –    | cancel | recovery rate, lines, payments, assign operator |
| Payments           | ✓    | ✓      | –      | –    | ✓      | filter by invoice, revenue over a period |
| Products           | ✓    | ✓      | ✓      | ✓    | ✓      | assign a stock                           |
| Product categories | ✓    | ✓      | ✓      | ✓    | ✓      |                                          |
| Stocks             | ✓    | ✓      | ✓      | ✓    | ✓      | "below minimum" flag                     |
| Suppliers          | ✓    | ✓      | ✓      | ✓    | ✓      | assign business sectors, invoices list   |
| Business sectors   | ✓    | ✓      | ✓      | ✓    | ✓      |                                          |
| Operators          | ✓    | ✓      | ✓      | ✓    | ✓      |                                          |

Missing actions (–) are not exposed by the backend; see "Known API limitations"
in the backend README.

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

Formatting uses Prettier (`.prettierrc`): `npx prettier --write "src/**/*.{ts,html,scss}"`.

## Troubleshooting

- **`mat-form-field must contain a MatFormFieldControl` in the console (dev server only):**
  the Vite dependency cache is out of sync, usually after installing packages
  or running two `ng serve` at once. Stop every `ng serve`, delete the cache and
  restart, then hard-refresh the browser (Ctrl+Shift+R):

  ```bash
  rm -rf .angular/cache
  npm start
  ```

- **"Cannot reach the server. Is the backend running?":** start the backend on
  port 8089, or update `proxy.conf.json`.
- **Port 4200 already in use:** `ng serve --port 4300`.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
