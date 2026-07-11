# wiki Helm chart

Deploys the approval-gated Markdown wiki: Spring Boot backend + Vue/nginx frontend
+ (optional) bundled Postgres, behind a single Ingress host with Keycloak OIDC.

## Prerequisites

- Kubernetes + an Ingress controller (nginx), and (for TLS) cert-manager.
- Container images on GitHub Container Registry:
  - `ghcr.io/oglimmer/wiki-backend:<tag>`
  - `ghcr.io/oglimmer/wiki-frontend:<tag>`
  - If the packages are private, create a pull secret and set `imagePullSecrets`
    (see `values.yaml`); or make the ghcr.io packages public.
- A Keycloak realm + confidential client whose redirect URI allows
  `https://<host>/api/login/oauth2/code/keycloak`.

## Secret (not created by the chart)

Create a Secret with these keys (name defaults to `<release>-wiki-secret`, or set
`existingSecret`):

| Key | Required |
|-----|----------|
| `POSTGRES_PASSWORD` | always — DB password (bundled or external DB) |
| `OIDC_CLIENT_SECRET` | always — Keycloak client secret |

```bash
kubectl create namespace wiki
kubectl -n wiki create secret generic wiki-secret \
  --from-literal=POSTGRES_PASSWORD='change-me' \
  --from-literal=OIDC_CLIENT_SECRET='<keycloak-client-secret>'
```

## Install

```bash
helm install wiki ./helm/wiki -n wiki \
  --set ingress.host=wiki.example.com \
  --set oidc.issuerUri=https://id.oglimmer.de/realms/wiki \
  --set oidc.clientId=wiki-app \
  --set backend.image.tag=0.1.0 \
  --set frontend.image.tag=0.1.0
```

Or with a values file:

```bash
helm install wiki ./helm/wiki -n wiki -f my-values.yaml
```

Upgrade / uninstall:

```bash
helm upgrade wiki ./helm/wiki -n wiki -f my-values.yaml
helm uninstall wiki -n wiki    # PVC for bundled Postgres is retained
```

## Key values

| Key | Default | Notes |
|-----|---------|-------|
| `ingress.host` | `wiki.example.com` | Public host; `/api`→backend, `/`→frontend |
| `oidc.issuerUri` | `https://id.oglimmer.de/realms/wiki` | Realm discovery URL |
| `oidc.clientId` | `wiki-app` | Keycloak client id |
| `postgres.enabled` | `true` | `false` → set `externalDatabase.*` + secret |
| `postgres.image.tag` | `17-alpine` | **Major pinned** — do not bump via Renovate |
| `backend.image.tag` / `frontend.image.tag` | chart `appVersion` | Pin by digest in prod |
| `existingSecret` | `""` | Defaults to `<release>-wiki-secret` |

See `values.yaml` for the full, commented list.

## Validate before applying

```bash
helm lint ./helm/wiki
helm template wiki ./helm/wiki -n wiki --set ingress.host=wiki.example.com --debug
```

## Notes

- The backend health probes use Spring Boot Actuator:
  `/api/actuator/health/liveness` and `/api/actuator/health/readiness`.
- Bundled Postgres is a single-replica StatefulSet with a `ReadWriteOnce` PVC.
  Do not scale it; for HA use an external database (`postgres.enabled=false`).
- The first user to log in is provisioned as the approved **admin**.
