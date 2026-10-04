#!/usr/bin/env bash
set -euo pipefail
# The existing PostgreSQL harness copies sources to /tmp without .env and removes
# its disposable database on exit. This benchmark never targets a deployed app.
backend_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export UITMERCH_PERFORMANCE_AUDIT=true
exec bash "$backend_dir/scripts/test-postgres.sh" -Dtest=ApiPerformanceAuditTest "$@"
