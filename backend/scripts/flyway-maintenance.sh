#!/usr/bin/env bash
set -euo pipefail
backend_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
maintenance_build="$(mktemp -d /tmp/uitmerch-flyway.XXXXXX)"
trap 'rm -rf "$maintenance_build"' EXIT
cd "$backend_dir"
./mvnw -q dependency:build-classpath -Dmdep.outputFile="$maintenance_build/classpath"
maintenance_classpath="$(cat "$maintenance_build/classpath")"
javac -proc:none -cp "$maintenance_classpath" -d "$maintenance_build" scripts/FlywayMaintenance.java
# Keep connection diagnostics private: JDBC/Flyway can log the configured database URL.
maintenance_log="$(mktemp /tmp/uitmerch-flyway-log.XXXXXX)"
if java -cp "$maintenance_build:$maintenance_classpath" FlywayMaintenance "${1:-validate}" >"$maintenance_log" 2>&1; then
  sed -n '/^Validation:/p; /^Pending V/p; /^Schema history backup:/p; /^Known V16/p; /^Applied migrations:/p; /^Schema history already/p' "$maintenance_log"
  rm -f "$maintenance_log"
else
  sed -n '/^Validation:/p; /^Invalid V/p; /Repair refused:/p' "$maintenance_log"
  printf 'Maintenance failed; private diagnostics: %s\n' "$maintenance_log" >&2
  exit 1
fi
