# syntax=docker/dockerfile:1

# =============================================================================
# Angular (achat) - optimized multi-stage image
#
# Stage 1 (builder): install dependencies and compile the production bundle.
# Stage 2 (runtime): unprivileged nginx serving the static bundle and
#                    reverse-proxying /api to the backend (same-origin, no CORS).
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 - build
# -----------------------------------------------------------------------------
FROM node:24-alpine AS builder

WORKDIR /app

# Manifests only (change less often than source) - caches the dependency layer.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

# Application source (node_modules, dist and caches are excluded by .dockerignore).
COPY . .

# Angular's default configuration is "production" (see angular.json).
RUN npm run build


# -----------------------------------------------------------------------------
# Stage 2 - runtime
# -----------------------------------------------------------------------------
FROM nginxinc/nginx-unprivileged:alpine AS runtime

LABEL org.opencontainers.image.title="achat-frontend" \
      org.opencontainers.image.description="Angular frontend for the achat application" \
      org.opencontainers.image.source="https://github.com/org/Projet-DevOps"

# The image's entrypoint renders /etc/nginx/templates/*.template with envsubst
# at startup, so BACKEND_URL can be changed without rebuilding the image.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Static bundle produced by the Angular application builder.
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

# Where /api requests are forwarded (must be reachable from this container).
# Keep the dev proxy semantics: /api/foo -> <url>/foo.
# DNS_RESOLVER is Docker's embedded DNS by default; change it on Kubernetes.
ENV BACKEND_URL=http://backend:8089 \
    DNS_RESOLVER=127.0.0.11

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/healthz >/dev/null 2>&1 || exit 1
