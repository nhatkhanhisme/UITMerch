#!/usr/bin/env bash
set -euo pipefail
backend_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_build="$(mktemp -d /tmp/uitmerch-backend-test.XXXXXX)"
container_name="uitmerch-test-$(cat /proc/sys/kernel/random/uuid)"
postgres_image="${UITMERCH_TEST_POSTGRES_IMAGE:-pgvector/pgvector:pg17}"
created_container=false
cleanup() {
  if [[ "$created_container" == true ]]; then
    docker rm --force "$container_name" >/dev/null 2>&1 || true
  fi
  printf 'Test build and reports: %s\n' "$test_build"
}
trap cleanup EXIT
cp -a "$backend_dir/src" "$backend_dir/pom.xml" "$backend_dir/mvnw" "$backend_dir/.mvn" "$test_build/"
docker run --detach --name "$container_name" --user postgres --entrypoint sh \
  --publish 127.0.0.1::5432 --tmpfs /test-data:rw,mode=1777 --shm-size=128m \
  --env POSTGRES_PASSWORD=uitmerch_test_only "$postgres_image" -c '
    set -eu
    umask 077
    printf "%s\n" "$POSTGRES_PASSWORD" > /test-data/password
    initdb -D /test-data/pg --auth-local=trust --auth-host=scram-sha-256 --pwfile=/test-data/password > /test-data/initdb.log
    printf "host all all 0.0.0.0/0 scram-sha-256\n" >> /test-data/pg/pg_hba.conf
    exec postgres -D /test-data/pg -c listen_addresses=*
  ' >/dev/null
created_container=true
ready=false
for attempt in {1..30}; do
  if docker exec "$container_name" pg_isready -U postgres >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
if [[ "$ready" != true ]]; then docker logs "$container_name"; exit 1; fi
docker exec "$container_name" createdb --username postgres uitmerch_test
host_port="$(docker port "$container_name" 5432/tcp | sed 's/.*://')"
export UITMERCH_TEST_DATABASE_URL="jdbc:postgresql://127.0.0.1:${host_port}/uitmerch_test"
cd "$test_build"
./mvnw test "$@"
