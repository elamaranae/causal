#!/bin/bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"

# Regenerate seed SQL files from the single source of truth
echo "generating seed SQL from seed-data.yml..."
python3 "$DIR/generate-seeds.py"
echo ""

SEEDS=(
  "causal-causal-identity-db-1:userdb:identity/seed.sql"
  "causal-causal-product-db-1:productdb:product/seed.sql"
  "causal-causal-inventory-db-1:inventorydb:inventory/seed.sql"
)

for entry in "${SEEDS[@]}"; do
  IFS=: read -r container db file <<< "$entry"
  if [ -f "$DIR/$file" ]; then
    echo "seeding $db..."
    docker exec -i "$container" psql -U postgres -d "$db" < "$DIR/$file"
    echo ""
  fi
done

echo "done"
