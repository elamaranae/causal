#!/bin/bash
set -euo pipefail

MARKER_START="# causal-start"
MARKER_END="# causal-end"

ENTRIES="
172.20.0.2    causal-gateway
172.20.0.10   causal-identity-app
172.20.0.11   causal-identity-db
172.20.0.20   causal-product-app
172.20.0.21   causal-product-db
172.20.0.30   causal-cart-app
172.20.0.31   causal-cart-db
172.20.0.40   causal-inventory-app
172.20.0.41   causal-inventory-db
172.20.0.50   causal-order-app
172.20.0.51   causal-order-db
172.20.0.60   causal-profile-app
172.20.0.61   causal-profile-db
172.20.0.100  o11y-otel-collector
172.20.0.101  o11y-prometheus
172.20.0.102  o11y-tempo
172.20.0.103  o11y-loki
172.20.0.104  o11y-grafana
"

add() {
    remove 2>/dev/null || true
    printf "\n%s\n%s\n%s\n" "$MARKER_START" "$ENTRIES" "$MARKER_END" | sudo tee -a /etc/hosts > /dev/null
    echo "added causal hosts"
}

remove() {
    sudo sed -i '' "/$MARKER_START/,/$MARKER_END/d" /etc/hosts
    echo "removed causal hosts"
}

case "${1:-}" in
    add)    add ;;
    remove) remove ;;
    *)      echo "usage: $0 {add|remove}" && exit 1 ;;
esac
