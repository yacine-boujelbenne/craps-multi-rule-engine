#!/usr/bin/env bash
set -euo pipefail
repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
exec opencode run 'Continue the existing Angular frontend task using the attached architect handoff. Implement its scoped frontend integration and report commits and verification.' \
  --session ses_e343dadeaffe3jKWkxIpbe2WWx \
  --dir "$repo_dir" \
  --file="$repo_dir/docs/handoffs/FRONTEND-LOCAL-ACCOUNTS.md" \
  --format=json
