#!/usr/bin/env bash
set -euo pipefail

verification_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$verification_dir"

if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
  printf '%s\n' 'Docker is unavailable to this process. Run docker info in your normal terminal, then retry this script there.' >&2
  exit 1
fi

verification_compiler=""
if [[ -n "${JAVAC_COMMAND:-}" ]]; then
  verification_compiler="$(command -v "$JAVAC_COMMAND" 2>/dev/null || true)"
elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
  verification_compiler="$JAVA_HOME/bin/javac"
elif command -v javac >/dev/null 2>&1; then
  verification_compiler="$(command -v javac)"
elif [[ -x /snap/intellij-idea/current/jbr/bin/javac ]]; then
  verification_compiler=/snap/intellij-idea/current/jbr/bin/javac
fi

if [[ -z "$verification_compiler" || ! -x "$verification_compiler" ]]; then
  printf '%s\n' 'A Java compiler is required. Install a JDK or set JAVAC_COMMAND to its javac executable.' >&2
  exit 1
fi

printf 'Running full backend verification using compiler: %s\n' "$verification_compiler"
exec ./mvnw -Dmaven.compiler.fork=true "-Dmaven.compiler.executable=$verification_compiler" verify "$@"
