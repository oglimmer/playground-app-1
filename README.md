# Wiki

An approval-gated Markdown wiki. Vue 3 SPA + Spring Boot API + PostgreSQL, with
single-sign-on via Keycloak (OIDC). Built to the conventions in
`~/dev/coding-guidelines` (`vue-frontend.md` + `java-spring-backend.md`).

## Features

- **SSO via OIDC / Keycloak** — session-cookie auth (no JWT in localStorage), CSRF-protected.
- **Admin-gated membership** — the **first user to sign in becomes an approved admin**;
  everyone else lands in a **"waiting for approval"** screen until an admin approves them.
- **Wiki** — an index of all pages, each page a Markdown document.
- **Editor** — a Markdown editor with live **Write / Split / Preview** modes, plus a
  **Raw** toggle on the page view. Markdown is sanitized (DOMPurify) before rendering.
- **Admin console** — approve / revoke members.

## Stack

| Layer    | Choice                                                             |
|----------|-------------------------------------------------------------------|
| Frontend | Vue 3 + TypeScript + Vite, Pinia, vue-router, hand-rolled fetch client |
| Backend  | Spring Boot, Spring Security OAuth2 login, Spring Data JPA, Flyway |
| DB       | PostgreSQL                                                        |
| Auth     | Keycloak (OIDC) — any OIDC provider works via env vars            |

## Architecture

```
Browser ──/api──► Vite dev proxy (5173) ──► Spring Boot (:8080, context-path /api)
   │                                              │
   └──────── OIDC redirect ───► Keycloak (:8081) ◄┘ (token exchange)
                                                   └► PostgreSQL
```

- **Single origin, path-based routing.** The browser only ever talks to one host: `/`
  serves the SPA, `/api/*` is the backend. Nothing hard-codes a host or port.
  - **Dev:** the Vite dev server (`:5173`) proxies `/api` → `:8080` (see `vite.config.ts`).
  - **Prod:** Traefik on one domain (no port) routes `PathPrefix(/api)` → backend and
    everything else → the frontend nginx. The backend trusts `X-Forwarded-*`
    (`forward-headers-strategy: framework`) so it builds OIDC redirect URIs and the
    post-login redirect against the public host.
- The whole API is mounted under `/api` (Spring `context-path`), including the OIDC
  login endpoints (`/api/oauth2/authorization/keycloak`, `/api/login/oauth2/code/keycloak`).
- After login the backend redirects to the SPA root `/` on the same origin (not `/api`).
- Login begins with a full-page redirect; unauthenticated XHR gets a clean **401**
  (via `X-Requested-With: XMLHttpRequest`) instead of an HTML login redirect.
- Approval status is checked **live from the DB on every wiki request**, so an admin
  approval takes effect immediately — the approved user does not need to log in again.

## Prerequisites

- JDK 21+ (Java 21 target), Node 20+, Docker + Docker Compose.

## Run it locally (recommended)

Keep the app on the host so the browser and backend both reach Keycloak at the same
`http://localhost:8081` origin (avoids OIDC issuer-mismatch problems).

```bash
# 1. Start infrastructure: Postgres (host :5433) + Keycloak (:8081) with a seeded realm
docker compose up -d

# 2. Backend (Postgres is on host port 5433 — see docker-compose.yml)
cd backend
DB_URL=jdbc:postgresql://localhost:5433/wiki ./mvnw spring-boot:run

# 3. Frontend
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**.

### Seeded Keycloak users

The bundled realm (`keycloak/wiki-realm.json`) has two test accounts (self-registration
is also enabled at the Keycloak login screen):

| Username | Password | Notes                                             |
|----------|----------|---------------------------------------------------|
| `alice`  | `alice`  | Sign in **first** → becomes the approved **admin** |
| `bob`    | `bob`    | Signs in second → **pending**, needs approval      |

Keycloak admin console: http://localhost:8081 (`admin` / `admin`).

### Try the flow

1. Sign in as **alice** first → she is auto-provisioned as an approved admin and sees the wiki.
2. Create a page, then open **Admin** in the header.
3. In another browser / private window, sign in as **bob** → he sees "waiting for approval".
4. As alice, approve bob in **Admin**. Bob clicks **Check again** → he's in.

## Pointing at your own Keycloak

Set these env vars for the backend (see `.env.example`) instead of using the bundled realm:

```
OIDC_ISSUER_URI=https://your-keycloak/realms/<realm>
OIDC_CLIENT_ID=<client-id>
OIDC_CLIENT_SECRET=<client-secret>
```

Your Keycloak client must be **confidential**, with the standard (authorization-code) flow
enabled and these redirect URIs allowed:

- `http://localhost:5173/*` (dev) and your production origin
- valid redirect: `<origin>/api/login/oauth2/code/keycloak`

## Project layout

```
backend/    Spring Boot API (controller / service / repository / entity / dto / security / config)
frontend/   Vue 3 SPA (views / components / stores / composables / lib / api.ts / router.ts)
keycloak/   wiki-realm.json — seeded realm (client + test users)
helm/       wiki/ Helm chart (+ argocd/ Application) for Kubernetes deploy
oglimmer.sh          build/push images, restart deployments, cut releases, local dev
docker-compose.yml   Postgres + Keycloak (+ optional "app" profile for the built images)
```

## Build & deploy with `oglimmer.sh`

The repo-root script builds/pushes images to `ghcr.io/oglimmer`, restarts the
k8s deployments, and cuts releases. `frontend/package.json` `version` is the semver
source of truth; the script propagates it to images, the Helm chart, and the git tag.

```bash
./oglimmer.sh show                       # current version
./oglimmer.sh build -a --dry-run         # preview build+push+restart of both components
./oglimmer.sh build -b --platform auto   # CI shape (plain docker, ARC runner)
./oglimmer.sh release --bump minor       # bump versions, commit, tag v*, push
./oglimmer.sh helm-push                  # package + push the OCI chart
./oglimmer.sh test                       # backend mvnw test + frontend npm test
```

Restart works via `kubectl` (developer) **or** a `RESTART_TOKEN` hook (CI runner);
the token is never logged. Run `./oglimmer.sh --help` for all flags.

## Deploy to Kubernetes

A Helm chart lives in `helm/wiki` (backend + frontend + optional bundled Postgres,
single Ingress host, `/api`→backend and `/`→frontend). Quick start:

```bash
kubectl create namespace wiki
kubectl -n wiki create secret generic wiki-secret \
  --from-literal=POSTGRES_PASSWORD='change-me' \
  --from-literal=OIDC_CLIENT_SECRET='<keycloak-client-secret>'

helm install wiki ./helm/wiki -n wiki \
  --set ingress.host=wiki.example.com \
  --set oidc.issuerUri=https://id.oglimmer.de/realms/wiki \
  --set backend.image.tag=0.1.0 --set frontend.image.tag=0.1.0
```

Full options, external-DB mode, and GitOps (ArgoCD) notes: [helm/wiki/README.md](helm/wiki/README.md).

## API

All routes are under `/api`. Errors use the shape `{"error": "..."}`.

| Method | Path                          | Access            |
|--------|-------------------------------|-------------------|
| GET    | `/me`                         | any authenticated |
| GET    | `/pages`                      | approved          |
| GET    | `/pages/{slug}`               | approved          |
| POST   | `/pages`                      | approved          |
| PUT    | `/pages/{slug}`               | approved          |
| DELETE | `/pages/{slug}`               | approved          |
| GET    | `/admin/users`                | admin             |
| POST   | `/admin/users/{id}/approve`   | admin             |
| POST   | `/admin/users/{id}/revoke`    | admin             |

Mutating requests require the CSRF header `X-XSRF-TOKEN` (read from the `XSRF-TOKEN` cookie).

## Tests / checks

```bash
cd backend  && ./mvnw test        # context load + unit tests
cd frontend && npm run check      # typecheck + lint + vitest
```

## CI / CD

`.github/workflows/` (see `~/dev/coding-guidelines/github-actions.md`):

| Workflow | Runner | Trigger | Purpose |
|----------|--------|---------|---------|
| `ci.yml` | ubuntu-latest | PR + push to main | Path-filtered gates: backend `spotless:check`+`verify`, frontend `ci`/typecheck/lint/test/build |
| `build.yml` | `arc-wiki` | push to main, manual | `./oglimmer.sh build … --platform auto` → push to ghcr.io + restart |
| `release.yml` | ubuntu-latest | `v*` tags | Multi-arch images (`:vX.Y.Z` + `:latest`) + GitHub Release |
| `cleanup-images.yml` | ubuntu-latest | daily | Prune untagged ghcr.io versions |

Images: `ghcr.io/oglimmer/wiki-backend` + `-frontend` (login via `GITHUB_TOKEN`).
`build.yml` runs on the in-cluster ARC runner and restarts via `RESTART_TOKEN`, so
onboard `arc-wiki` (`/setup-arc-build`) and set the `RESTART_TOKEN` repo secret first.

## Notes / deviations from the guidelines

- **Spring Boot 4.1.0** (Spring Framework 7, Spring Security 7), Java 21 — per the guideline.
  Flyway is wired via the Boot 4 `spring-boot-starter-flyway` + `flyway-database-postgresql`.
- **Spotless** (palantir-java-format) is the enforced format gate; the source is formatted
  and `spotless:check` passes on Java 21. Note it can't run on this machine's JDK 25 (the
  formatter uses internal javac APIs), so format via a Java 21 toolchain / CI.
- Docker Compose maps Postgres to host **5433** to avoid clashing with a local 5432.
