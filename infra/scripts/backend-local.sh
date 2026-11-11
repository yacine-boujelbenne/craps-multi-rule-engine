#!/usr/bin/env bash
set -euo pipefail
repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
if [[ -f "$repo_dir/.env" ]]; then
  set -a
  source "$repo_dir/.env"
  set +a
fi
cd "$repo_dir/backend"
exec ./mvnw spring-boot:run "$@"
