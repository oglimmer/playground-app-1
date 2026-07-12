# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An approval-gated Markdown wiki: Vue 3 SPA + Spring Boot API + PostgreSQL, SSO via
Keycloak (OIDC). Code follows the conventions in `~/dev/coding-guidelines`
(`vue-frontend.md`, `java-spring-backend.md`, `github-actions.md`).

## Commands

### Backend (`backend/`, Maven wrapper)
```bash
DB_URL=jdbc:postgresql://localhost:5433/wiki ./mvnw spring-boot:run   # run (needs `docker compose up -d` first)
./mvnw test                                    # full test suite (runs against in-memory H2)
./mvnw test -Dtest=PageTagsIntegrationTest     # single test class
./mvnw spotless:apply                          # format (REQUIRED before commit; CI runs spotless:check)
./mvnw verify                                  # what CI's backend gate runs
```
Spotless uses palantir-java-format, which relies on internal javac APIs and **only runs
on a Java 21 toolchain** — it fails on JDK 25. Format via Java 21 or let CI do it.

### Frontend (`frontend/`, npm)
```bash
npm install && npm run dev     # Vite dev server on :5173, proxies /api → :8080
npm run check                  # typecheck + lint + vitest — the frontend CI gate
npm run typecheck              # vue-tsc only
npx vitest run src/lib/markdown.test.ts   # single test file
npm run lint:fix               # eslint --fix
```

### Local stack
`docker compose up -d` starts Postgres (host port **5433**, not 5432) + Keycloak (:8081)
with a seeded realm. Sign in as `alice`/`alice` **first** — the first user to log in
becomes the approved admin; `bob`/`bob` lands in the pending-approval screen.

### Build / release (`./oglimmer.sh`, repo root)
`frontend/package.json` `version` is the single semver source of truth; the script
propagates it to images, the Helm chart, and the git tag. `./oglimmer.sh --help` for all flags.
```bash
./oglimmer.sh show                  # current version
./oglimmer.sh test                  # backend mvnw test + frontend npm test
./oglimmer.sh build -a --dry-run    # preview build+push+restart of both components
./oglimmer.sh release --bump minor  # bump versions, commit, tag v*, push
```

## Architecture — the non-obvious parts

**Single origin, path-based routing.** The browser only ever talks to one host: `/`
serves the SPA, `/api/*` is the backend (Spring `context-path: /api`). Dev = Vite proxy;
prod = Traefik routing `PathPrefix(/api)` → backend, everything else → frontend nginx.
Nothing hard-codes a host/port. The backend trusts `X-Forwarded-*`
(`forward-headers-strategy: framework`) to build OIDC redirect URIs against the public host.

**Auth is session-cookie + OIDC, no JWT in the browser.** Key pieces that must stay consistent:
- `frontend/src/api.ts` sends `X-Requested-With: XMLHttpRequest` on every request. This is
  the signal `SecurityConfig` uses to return a clean **401** to XHR instead of a 302 HTML
  login redirect. A plain top-level navigation, lacking that header, *is* redirected into
  the Keycloak flow. Both cases are owned by one `authenticationEntryPoint` — see the
  warning comment in `SecurityConfig` about not mixing in `defaultAuthenticationEntryPointFor`.
- After OIDC login the backend does `sendRedirect("/")` (SPA root, NOT `/api`). It uses a
  raw redirect to bypass Spring's `DefaultRedirectStrategy`, which would prepend the `/api`
  context-path. Don't "fix" this to use the redirect strategy.
- CSRF: `CookieCsrfTokenRepository` (non-HttpOnly) sets an `XSRF-TOKEN` cookie; mutating
  requests must echo it as the `X-XSRF-TOKEN` header (`api.ts` does this for non-GET).

**Approval is checked live from the DB, never baked into the session.** Roles (`ROLE_USER`,
`ROLE_ADMIN`) become Spring authorities at login in `CustomOidcUserService`, but
approval **status** is deliberately not an authority. `CurrentUserService.requireApproved()`
re-reads the `AppUser` on every wiki request, so an admin approving a pending user takes
effect immediately without the user re-authenticating. The frontend mirrors this: the
router guard (`router.ts`) routes unapproved users to `/pending`, and `PendingView`'s
"Check again" calls `auth.refresh()`.

**Layering (backend).** Standard Spring flow: `controller → service → repository → entity`,
with `dto/` records for the wire shape and `exception/` types mapped to the
`{"error": "..."}` contract by `GlobalExceptionHandler`. Pages get a unique slug via
`Slugs` + `PageService.uniqueSlug` (appends `-2`, `-3`, …). Tags are normalized by `Tags`
and stored in a separate `page_tag` table (see `V2__add_tags.sql`).

**Schema is Flyway-managed.** JPA runs with `ddl-auto: validate` — it will NOT create or
alter tables. Any schema change needs a new `V*__*.sql` under
`backend/src/main/resources/db/migration/`.

**Frontend data flow.** Pinia `auth` store holds the session (`ensureUser()` de-dupes
concurrent loads). Components fetch via the hand-rolled `api` client + `useAsyncData`
composable — there is no axios/react-query. Markdown is rendered with `marked` and
**sanitized with DOMPurify** in `src/lib/markdown.ts` before display; keep sanitization
on that path.

## Conventions & gotchas

- **Spring Boot 4.1 / Spring Framework 7 / Spring Security 7, Java 21.** APIs differ from
  Boot 3 — verify against the installed version, not memory.
- Run `spotless:apply` before committing Java, or the `ci.yml` backend gate fails.
- Postgres is on host **5433** locally (compose remaps it to avoid clashing with a local 5432).
- CI (`.github/workflows/ci.yml`) is path-filtered: backend changes run
  `spotless:check` + `verify`; frontend changes run `npm run check` + build.
