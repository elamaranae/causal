#!/bin/bash
set -euo pipefail

SEEDS=(
  "causal-causal-identity-db-1:userdb:identity/seed.sql"
  "causal-causal-product-db-1:productdb:product/seed.sql"
  "causal-causal-inventory-db-1:inventorydb:inventory/seed.sql"
)

DIR="$(cd "$(dirname "$0")" && pwd)"

for entry in "${SEEDS[@]}"; do
  IFS=: read -r container db file <<< "$entry"
  if [ -f "$DIR/$file" ]; then
    echo "seeding $db..."
    docker exec -i "$container" psql -U postgres -d "$db" < "$DIR/$file"
    echo ""
  fi
done

echo "done"
