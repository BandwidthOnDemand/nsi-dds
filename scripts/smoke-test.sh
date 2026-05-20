#!/usr/bin/env bash
#
# Build the nsi-dds image, start it with the in-image default config, and assert
# that the JVM bootstrap phase comes up cleanly: no FileHandler failures from
# logging.properties, no NoSuchFileException for var/log/jersey.log.
#
# The container may exit during later bean wiring because the in-image default
# dds.xml expects writable cache/repository directories that the image does not
# provide; that is a separate pre-existing issue and not what this test gates.
#
set -euo pipefail

IMAGE_TAG="${IMAGE_TAG:-nsi-dds:smoke}"
CONTAINER_NAME="${CONTAINER_NAME:-nsi-dds-smoke}"
WAIT_SECONDS="${WAIT_SECONDS:-8}"

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo ">> Building image $IMAGE_TAG"
docker build -t "$IMAGE_TAG" .

echo ">> Starting container $CONTAINER_NAME"
docker run -d --name "$CONTAINER_NAME" "$IMAGE_TAG" >/dev/null

echo ">> Waiting ${WAIT_SECONDS}s for startup logs"
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

echo "OK: logging bootstrap clean"
