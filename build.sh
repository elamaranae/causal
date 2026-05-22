#!/usr/bin/env bash
set -euo pipefail

services=(identity profile product orders cart inventory)
pids=()
failures=()

for service in "${services[@]}"; do
  echo "Building $service..."
  ./"$service"/gradlew -p "$service" jibDockerBuild &
  pids+=("$!:$service")
done

for entry in "${pids[@]}"; do
  pid="${entry%%:*}"
  service="${entry##*:}"
  if wait "$pid"; then
    echo "$service built successfully."
  else
    failures+=("$service")
    echo "$service FAILED."
  fi
done

if [ ${#failures[@]} -gt 0 ]; then
  echo "Failed services: ${failures[*]}"
  exit 1
fi

echo "All services built successfully."
