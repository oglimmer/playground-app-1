#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# oglimmer.sh — build/push images, restart k8s deployments, cut releases, and
# run the app locally. See ~/dev/coding-guidelines/oglimmer-sh.md.
# ─────────────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Repo-specific defaults (only place names live) ───────────────────────────
# Images live on the GitHub Container Registry: ghcr.io/oglimmer/wiki-{backend,frontend}.
DEFAULT_REGISTRIES=("ghcr.io/oglimmer")
IMAGE_PREFIX="wiki"                                   # -> wiki-frontend, wiki-backend
DEFAULT_FRONTEND_DEPLOYMENT="wiki-frontend"
DEFAULT_BACKEND_DEPLOYMENT="wiki-backend"
RESTART_NAMESPACE="${RESTART_NAMESPACE:-wiki}"
RESTART_HOOK_URL="${RESTART_HOOK_URL:-https://restart.oglimmer.com/restart}"

FRONTEND_DIR="frontend"
BACKEND_DIR="backend"
HELM_CHART_DIR="helm/wiki"
HELM_OCI_REGISTRY="${HELM_OCI_REGISTRY:-oci://ghcr.io/oglimmer}"

VERSION_FILE="frontend/package.json"
CHART_FILE="helm/wiki/Chart.yaml"
POM_FILE="backend/pom.xml"

LOCAL_DB_URL="${DB_URL:-jdbc:postgresql://localhost:5433/wiki}"
BACKEND_PIDFILE="/tmp/${IMAGE_PREFIX}-backend.pid"
BACKEND_LOGFILE="/tmp/${IMAGE_PREFIX}-backend.log"

# ── Option defaults ──────────────────────────────────────────────────────────
COMMAND="build"
BUILD_FRONTEND=false
BUILD_BACKEND=false
VERBOSE="${VERBOSE:-false}"
DRY_RUN="${DRY_RUN:-false}"
PUSH="${PUSH:-true}"
RESTART="${RESTART:-true}"
PLATFORM="${PLATFORM:-${DOCKER_PLATFORM:-arm64}}"
BUMP=""
FRONTEND_DEPLOYMENT="${FRONTEND_DEPLOYMENT:-$DEFAULT_FRONTEND_DEPLOYMENT}"
BACKEND_DEPLOYMENT="${BACKEND_DEPLOYMENT:-$DEFAULT_BACKEND_DEPLOYMENT}"
REGISTRIES=("${DEFAULT_REGISTRIES[@]}")

# ── Logging ──────────────────────────────────────────────────────────────────
log_info()    { printf '\033[0;34m[info]\033[0m  %s\n' "$*"; }
log_ok()      { printf '\033[0;32m[ ok ]\033[0m  %s\n' "$*"; }
log_error()   { printf '\033[0;31m[fail]\033[0m  %s\n' "$*" >&2; }
log_verbose() { [ "$VERBOSE" = true ] && printf '\033[0;90m[dbg ]\033[0m  %s\n' "$*" || true; }

# Run a command honouring DRY_RUN/VERBOSE. Secrets must never be passed here in
# a way that would be printed — the restart hook redacts its own token.
execute_cmd() {
  if [ "$DRY_RUN" = true ]; then
    printf '\033[0;33m[dry ]\033[0m  %s\n' "$*"
    return 0
  fi
  log_verbose "+ $*"
  if [ "$VERBOSE" = true ]; then
    "$@"
  else
    "$@" >/dev/null 2>&1
  fi
}

# ── Help ─────────────────────────────────────────────────────────────────────
show_help() {
  cat <<EOF
oglimmer.sh — manage the wiki (Spring Boot + Vue + Helm)

Usage: ./oglimmer.sh [COMMAND] [OPTIONS]

Commands:
  build (default)   Build/push selected components and restart their deployments
  release           Bump semver, sync versions, commit, tag, push
  helm-push         Package and push the OCI Helm chart
  show              Print the current version
  test              Backend (mvnw test) + frontend (npm test)
  start|stop|status|logs   Local backend (spring-boot:run) process

Component flags (build): default is --all
  -f, --frontend         Build the frontend
  -b, --backend          Build the backend
  -a, --all              Build both

Options:
  -v, --verbose          Show docker/kubectl/curl output
      --dry-run          Print commands without running them
      --no-push          Build locally only (auto-disables restart)
  -n, --no-restart       Skip the k8s rollout
      --registries a,b   Comma-separated registries (default: ${DEFAULT_REGISTRIES[*]})
      --platform P       auto | arm64 | amd64 | multi   (default: ${PLATFORM})
      --frontend-deploy N / --backend-deploy N
      --bump major|minor|bugfix   Non-interactive release
  -h, --help

Examples:
  ./oglimmer.sh build -a --dry-run
  ./oglimmer.sh build -b --platform auto --verbose      # CI
  ./oglimmer.sh release --bump minor
EOF
}

# ── Arg parsing ──────────────────────────────────────────────────────────────
parse_args() {
  if [ $# -gt 0 ]; then
    case "$1" in
      build|release|helm-push|show|test|start|stop|status|logs)
        COMMAND="$1"; shift ;;
    esac
  fi

  while [ $# -gt 0 ]; do
    case "$1" in
      -f|--frontend) BUILD_FRONTEND=true ;;
      -b|--backend)  BUILD_BACKEND=true ;;
      -a|--all)      BUILD_FRONTEND=true; BUILD_BACKEND=true ;;
      -v|--verbose)  VERBOSE=true ;;
      --dry-run)     DRY_RUN=true ;;
      --no-push)     PUSH=false ;;
      -n|--no-restart) RESTART=false ;;
      --registries)  shift; IFS=',' read -r -a REGISTRIES <<< "$1" ;;
      --platform)    shift; PLATFORM="$1" ;;
      --frontend-deploy) shift; FRONTEND_DEPLOYMENT="$1" ;;
      --backend-deploy)  shift; BACKEND_DEPLOYMENT="$1" ;;
      --bump)        shift; BUMP="$1" ;;
      -h|--help)     show_help; exit 0 ;;
      *) log_error "Unknown option: $1"; show_help; exit 1 ;;
    esac
    shift
  done

  # Default to --all for build when no component chosen.
  if [ "$COMMAND" = build ] && [ "$BUILD_FRONTEND" = false ] && [ "$BUILD_BACKEND" = false ]; then
    BUILD_FRONTEND=true
    BUILD_BACKEND=true
  fi

  # Nothing to roll out if we are not pushing.
  if [ "$PUSH" = false ] && [ "$RESTART" = true ]; then
    log_verbose "Push disabled -> disabling restart"
    RESTART=false
  fi
}

# ── Version / metadata ───────────────────────────────────────────────────────
read_version() {
  grep '"version"' "$VERSION_FILE" | head -1 | sed 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/'
}

git_commit() {
  if git rev-parse --short HEAD >/dev/null 2>&1; then
    git rev-parse --short HEAD
  else
    echo "unknown"
  fi
}

# ── Prerequisites ────────────────────────────────────────────────────────────
check_prerequisites() {
  if ! command -v docker >/dev/null 2>&1; then
    log_error "docker is required"; exit 1
  fi
  if [ "$PLATFORM" != auto ] && [ "$PLATFORM" != arm64 ] && [ "$PLATFORM" != amd64 ] && [ "$PLATFORM" != multi ]; then
    log_error "Invalid --platform '$PLATFORM' (auto|arm64|amd64|multi)"; exit 1
  fi
  if { [ "$PLATFORM" = multi ] || [ "$PLATFORM" = arm64 ] || [ "$PLATFORM" = amd64 ]; } \
     && ! docker buildx version >/dev/null 2>&1; then
    log_error "docker buildx required for --platform $PLATFORM"; exit 1
  fi
  if [ "$RESTART" = true ] && ! command -v kubectl >/dev/null 2>&1 && [ -z "${RESTART_TOKEN:-}" ]; then
    log_error "Restart requested but no kubectl and no RESTART_TOKEN. Install kubectl, set RESTART_TOKEN, or pass --no-restart."
    exit 1
  fi
}

# ── Build one image across registries ────────────────────────────────────────
# args: <context-dir> <image-suffix> <build-arg...>
build_image() {
  local ctx="$1" suffix="$2"; shift 2
  local build_args=("$@")
  local ba=()
  local a
  for a in "${build_args[@]}"; do ba+=(--build-arg "$a"); done

  local tags=()
  local reg
  for reg in "${REGISTRIES[@]}"; do
    tags+=("${reg}/${IMAGE_PREFIX}-${suffix}:${APP_VERSION}")
    tags+=("${reg}/${IMAGE_PREFIX}-${suffix}:latest")
  done

  local tag_flags=()
  local t
  for t in "${tags[@]}"; do tag_flags+=(-t "$t"); done

  log_info "Building ${IMAGE_PREFIX}-${suffix} (platform=${PLATFORM}) -> ${tags[*]}"

  case "$PLATFORM" in
    auto)
      execute_cmd docker build "${tag_flags[@]}" "${ba[@]}" "$ctx"
      if [ "$PUSH" = true ]; then
        for t in "${tags[@]}"; do execute_cmd docker push "$t"; done
      fi
      ;;
    arm64|amd64)
      local plat="linux/${PLATFORM}"
      if [ "$PUSH" = true ]; then
        execute_cmd docker buildx build --platform "$plat" "${tag_flags[@]}" "${ba[@]}" --push "$ctx"
      else
        execute_cmd docker buildx build --platform "$plat" "${tag_flags[@]}" "${ba[@]}" --load "$ctx"
      fi
      ;;
    multi)
      execute_cmd docker buildx build --platform linux/amd64,linux/arm64 "${tag_flags[@]}" "${ba[@]}" --push "$ctx"
      ;;
  esac
  log_ok "Built ${IMAGE_PREFIX}-${suffix}"
}

# ── Restart ──────────────────────────────────────────────────────────────────
restart_deployment() {
  local name="$1"
  [ "$RESTART" = true ] || { log_verbose "Restart disabled; skipping $name"; return 0; }
  if command -v kubectl >/dev/null 2>&1; then
    restart_via_kubectl "$name"
  elif [ -n "${RESTART_TOKEN:-}" ]; then
    restart_via_hook "$name"
  else
    log_error "No restart mechanism for $name"; return 1
  fi
}

restart_via_kubectl() {
  local name="$1"
  log_info "kubectl rollout restart deployment/$name -n $RESTART_NAMESPACE"
  execute_cmd kubectl rollout restart "deployment/$name" -n "$RESTART_NAMESPACE"
}

restart_via_hook() {
  local name="$1"
  local url="${RESTART_HOOK_URL}/${RESTART_NAMESPACE}/${name}"
  log_info "POST $url (Authorization: Bearer ***)"
  if [ "$DRY_RUN" = true ]; then
    printf '\033[0;33m[dry ]\033[0m  curl -X POST %s -H "Authorization: Bearer ***"\n' "$url"
    return 0
  fi
  if [ "$VERBOSE" = true ]; then
    curl -fsS -X POST "$url" -H "Authorization: Bearer ${RESTART_TOKEN}" -o /dev/null -w '  hook status: %{http_code}\n'
  else
    curl -fsS -X POST "$url" -H "Authorization: Bearer ${RESTART_TOKEN}" -o /dev/null
  fi
  log_ok "Restarted $name via hook"
}

# ── Build orchestration ──────────────────────────────────────────────────────
execute_build() {
  APP_VERSION="$(read_version)"
  local commit build_time
  commit="$(git_commit)"
  build_time="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  log_info "Version ${APP_VERSION} · commit ${commit} · ${build_time}"

  if [ "$BUILD_BACKEND" = true ]; then
    build_image "$BACKEND_DIR" backend \
      "VERSION=${APP_VERSION}" "GIT_COMMIT=${commit}" "BUILD_TIME=${build_time}"
    [ "$PUSH" = true ] && restart_deployment "$BACKEND_DEPLOYMENT"
  fi
  if [ "$BUILD_FRONTEND" = true ]; then
    build_image "$FRONTEND_DIR" frontend \
      "VITE_APP_VERSION=${APP_VERSION}" "VITE_GIT_COMMIT=${commit}" "VITE_BUILD_TIME=${build_time}"
    [ "$PUSH" = true ] && restart_deployment "$FRONTEND_DEPLOYMENT"
  fi
  log_ok "Build complete"
}

# ── Release ──────────────────────────────────────────────────────────────────
next_version() {
  local cur="$1" bump="$2"
  local major minor patch
  IFS='.' read -r major minor patch <<< "$cur"
  case "$bump" in
    major) echo "$((major+1)).0.0" ;;
    minor) echo "${major}.$((minor+1)).0" ;;
    bugfix|patch) echo "${major}.${minor}.$((patch+1))" ;;
    *) log_error "Invalid bump '$bump'"; exit 1 ;;
  esac
}

execute_release() {
  command -v git >/dev/null 2>&1 || { log_error "git required for release"; exit 1; }
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { log_error "not a git repo"; exit 1; }
  [ -z "$(git status --porcelain)" ] || { log_error "working tree not clean"; exit 1; }

  local cur; cur="$(read_version)"
  local bump="$BUMP"
  if [ -z "$bump" ]; then
    log_info "Current version: $cur"
    local opts=("major" "minor" "bugfix")
    select opts_choice in "${opts[@]}"; do
      [ -n "$opts_choice" ] && { bump="$opts_choice"; break; }
    done
  fi
  local new; new="$(next_version "$cur" "$bump")"
  log_info "Releasing $cur -> $new"

  execute_cmd bash -c "cd '$FRONTEND_DIR' && npm version '$new' --no-git-tag-version"
  # Chart version + appVersion
  execute_cmd sed -i.bak -E "s/^version: .*/version: $new/; s/^appVersion: .*/appVersion: \"$new\"/" "$CHART_FILE"
  execute_cmd rm -f "${CHART_FILE}.bak"
  # Maven project version (jar is copied as wiki-*.jar so this stays cosmetic/consistent)
  execute_cmd bash -c "cd '$BACKEND_DIR' && ./mvnw -q versions:set -DnewVersion='$new' -DgenerateBackupPoms=false"

  execute_cmd git add "$VERSION_FILE" "$FRONTEND_DIR/package-lock.json" "$CHART_FILE" "$POM_FILE"
  execute_cmd git commit -m "Release v$new"
  execute_cmd git tag -a "v$new" -m "Release v$new"
  execute_cmd git push origin HEAD
  execute_cmd git push origin "v$new"
  log_ok "Released v$new (tag pushed)"
}

# ── helm-push ────────────────────────────────────────────────────────────────
execute_helm_push() {
  command -v helm >/dev/null 2>&1 || { log_error "helm required"; exit 1; }
  # Authenticate to ghcr.io for OCI push (uses the gh CLI token; CI logs in via the workflow).
  if [ "${HELM_OCI_REGISTRY#oci://ghcr.io}" != "$HELM_OCI_REGISTRY" ] && command -v gh >/dev/null 2>&1; then
    if [ "$DRY_RUN" = true ]; then
      printf '\033[0;33m[dry ]\033[0m  gh auth token | helm registry login ghcr.io -u <user> --password-stdin\n'
    else
      gh auth token | helm registry login ghcr.io -u "$(gh api user -q .login)" --password-stdin
    fi
  fi
  local chart_version; chart_version="$(grep '^version:' "$CHART_FILE" | awk '{print $2}')"
  local tmp; tmp="$(mktemp -d)"
  log_info "Packaging $HELM_CHART_DIR ($chart_version) -> $HELM_OCI_REGISTRY"
  execute_cmd helm package "$HELM_CHART_DIR" -d "$tmp"
  execute_cmd helm push "$tmp/${IMAGE_PREFIX}-${chart_version}.tgz" "$HELM_OCI_REGISTRY"
  rm -rf "$tmp"
  log_ok "Pushed chart $chart_version"
}

# ── Local dev (backend) ──────────────────────────────────────────────────────
backend_start() {
  if [ -f "$BACKEND_PIDFILE" ] && kill -0 "$(cat "$BACKEND_PIDFILE")" 2>/dev/null; then
    log_info "Backend already running (pid $(cat "$BACKEND_PIDFILE"))"; return 0
  fi
  log_info "Starting backend (DB_URL=$LOCAL_DB_URL). Logs: $BACKEND_LOGFILE"
  ( cd "$BACKEND_DIR" && DB_URL="$LOCAL_DB_URL" nohup ./mvnw -q spring-boot:run >"$BACKEND_LOGFILE" 2>&1 & echo $! >"$BACKEND_PIDFILE" )
  log_ok "Backend started (pid $(cat "$BACKEND_PIDFILE"))"
}
backend_stop() {
  [ -f "$BACKEND_PIDFILE" ] || { log_info "Backend not running"; return 0; }
  local pid; pid="$(cat "$BACKEND_PIDFILE")"
  # spring-boot:run forks; kill the process group tree best-effort
  pkill -P "$pid" 2>/dev/null || true
  kill "$pid" 2>/dev/null || true
  rm -f "$BACKEND_PIDFILE"
  log_ok "Backend stopped"
}
backend_status() {
  if [ -f "$BACKEND_PIDFILE" ] && kill -0 "$(cat "$BACKEND_PIDFILE")" 2>/dev/null; then
    log_ok "Backend running (pid $(cat "$BACKEND_PIDFILE"))"
  else
    log_info "Backend not running"
  fi
}
backend_logs() { tail -f "$BACKEND_LOGFILE"; }

execute_test() {
  log_info "Backend tests"
  ( cd "$BACKEND_DIR" && ./mvnw -q test )
  log_info "Frontend tests"
  ( cd "$FRONTEND_DIR" && npm run test )
  log_ok "Tests passed"
}

# ── main ─────────────────────────────────────────────────────────────────────
main() {
  parse_args "$@"
  case "$COMMAND" in
    show)      read_version ;;
    start)     backend_start ;;
    stop)      backend_stop ;;
    status)    backend_status ;;
    logs)      backend_logs ;;
    test)      execute_test ;;
    helm-push) execute_helm_push ;;
    release)   execute_release ;;
    build)     check_prerequisites; execute_build ;;
    *)         show_help; exit 1 ;;
  esac
}

main "$@"
