#!/usr/bin/env bash
set -euo pipefail

SERVICES=(identity profile inventory order)
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS=()
RESULTS=()
FAILED=0

for svc in "${SERVICES[@]}"; do
  echo "Starting tests for $svc..."
  (cd "$BASE_DIR/$svc" && ./gradlew test 2>&1 | tail -5) &
  PIDS+=($!)
done

for i in "${!SERVICES[@]}"; do
  if wait "${PIDS[$i]}"; then
    RESULTS+=("✓ ${SERVICES[$i]}")
  else
    RESULTS+=("✗ ${SERVICES[$i]}")
    FAILED=1
  fi
done

echo ""
echo "=== Test Results ==="
for r in "${RESULTS[@]}"; do
  echo "  $r"
done

exit $FAILED
