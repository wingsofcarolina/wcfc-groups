#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

JAR_FILE="$PROJECT_ROOT/docker/wcfc-groups.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "JAR file not found at $JAR_FILE"
    echo -e "Please build the application first (e.g., using the Makefile)"
    exit 1
fi

if [[ -z "$@" ]]; then
  CMD=/app/test-scripts/run-tests.sh
else
  CMD="$@"
fi

echo -e "Running integration test in container..."
export WCFC_TOKEN=$(openssl rand -base64 36)
export GROUPS_IO_API_KEY=$(openssl rand -base64 36)
podman run -it --rm -p 9301:9301 \
    -v "$JAR_FILE:/app/wcfc-groups.jar" \
    -v "$SCRIPT_DIR/test-data:/app/test-data" \
    -v "$SCRIPT_DIR/test-scripts:/app/test-scripts" \
    -e MONGODB=mongodb://localhost:27017 \
    -e WCFC_TOKEN \
    -e GROUPS_IO_API_KEY \
    ghcr.io/wingsofcarolina/wcfc-integration-testing:latest\
    bash -c "$CMD"

