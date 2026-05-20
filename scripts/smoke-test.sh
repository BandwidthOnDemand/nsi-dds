#!/usr/bin/env bash
#
# Build the nsi-dds image, start it with the in-image default config, and assert
# that the container boots cleanly: no FileHandler failures from logging.properties,
# no NoSuchFileException for var/log/jersey.log, the container is still running
# after a short wait, and the HTTP server has reported "Started".
#
set -euo pipefail

IMAGE_TAG="${IMAGE_TAG:-nsi-dds:smoke}"
CONTAINER_NAME="${CONTAINER_NAME:-nsi-dds-smoke}"
WAIT_SECONDS="${WAIT_SECONDS:-10}"

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT
cleanup

echo ">> Building image $IMAGE_TAG"
docker build -t "$IMAGE_TAG" .

echo ">> Starting container $CONTAINER_NAME"
docker run -d --name "$CONTAINER_NAME" "$IMAGE_TAG" >/dev/null

echo ">> Waiting ${WAIT_SECONDS}s for startup"
sleep "$WAIT_SECONDS"

logs=$(docker logs "$CONTAINER_NAME" 2>&1)

fail() {
  echo "FAIL: $1"
  echo "----- container logs -----"
  echo "$logs"
  exit 1
}

echo "$logs" | grep -q "NoSuchFileException.*var/log/jersey.log" \
  && fail "regression: var/log/jersey.log NoSuchFileException is back"

echo "$logs" | grep -q "Can't load log handler" \
  && fail "regression: java.util.logging handler failed to load"

docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME" | grep -q true \
  || fail "container exited during startup"

echo "$logs" | grep -q "\[HttpServer\] Started" \
  || fail "HTTP server did not report Started within ${WAIT_SECONDS}s"

echo "OK: container booted cleanly"
