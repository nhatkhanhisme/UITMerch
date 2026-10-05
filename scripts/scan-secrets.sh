#!/usr/bin/env bash
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_dir="$(mktemp -d /tmp/uitmerch-secret-source.XXXXXX)"
report_dir="$(mktemp -d /tmp/uitmerch-secret-report.XXXXXX)"
trap 'rm -rf "$source_dir" "$report_dir"' EXIT
cd "$repo_dir"
# Include current tracked and new source; exclude ignored .env, credentials, caches and build output.
git ls-files --cached --others --exclude-standard -z | python3 -c 'import os,sys; sys.stdout.buffer.write(b"\0".join(path for path in sys.stdin.buffer.read().split(b"\0") if path and os.path.lexists(path))+b"\0")' | tar --null --verbatim-files-from --files-from=- -cf - | tar -xf - -C "$source_dir"
chmod -R a+rX "$source_dir"
status=0
docker run --rm --network none --volume "$source_dir:/scan:ro" --volume "$report_dir:/reports" \
  ghcr.io/gitleaks/gitleaks@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f \
  dir /scan --config /scan/.gitleaks.toml --redact --report-format json --report-path /reports/secret-scan.json || status=$?
if [[ -f "$report_dir/secret-scan.json" ]]; then
  cp "$report_dir/secret-scan.json" "${1:-/tmp/uitmerch-source-secret-scan.json}"
fi
rm -rf "$report_dir"
exit "$status"
