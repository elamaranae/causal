# Causal Project

## Architecture

Causal is a microservices e-commerce platform. All services run on a shared
Docker network (`causal_causal-net`, subnet `172.20.0.0/16`).

Service hostnames are accessible both inside containers and from the host
machine via `hosts.sh` (manages `/etc/hosts` entries). Some services may run
locally during development — in that case, the compose service uses socat to
forward traffic from the container IP to `host.docker.internal`.

### Services

| Hostname               | IP           | Type                  | App Port | Database     |
|------------------------|--------------|-----------------------|----------|--------------|
| causal-gateway         | 172.20.0.2   | Caddy (API Gateway)   | 80       | —            |
| causal-identity-app    | 172.20.0.10  | Spring Boot (identity)| 8080     | userdb       |
| causal-identity-db     | 172.20.0.11  | PostgreSQL            | 5432     | userdb       |
| causal-product-app     | 172.20.0.20  | Spring Boot (product) | 8080     | productdb    |
| causal-product-db      | 172.20.0.21  | PostgreSQL            | 5432     | productdb    |
| causal-cart-app        | 172.20.0.30  | Spring Boot (cart)    | 8080     | cartdb       |
| causal-cart-db         | 172.20.0.31  | PostgreSQL            | 5432     | cartdb       |
| causal-inventory-app   | 172.20.0.40  | Spring Boot (inventory)| 8080    | inventorydb  |
| causal-inventory-db    | 172.20.0.41  | PostgreSQL            | 5432     | inventorydb  |
| causal-orders-app      | 172.20.0.50  | Spring Boot (orders)  | 8080     | ordersdb     |
| causal-orders-db       | 172.20.0.51  | PostgreSQL            | 5432     | ordersdb     |
| causal-profile-app     | 172.20.0.60  | Spring Boot (profile) | 8080     | profiledb    |
| causal-profile-db      | 172.20.0.61  | PostgreSQL            | 5432     | profiledb    |

### Service Dependencies

- **product** → inventory (stock availability)
- **cart** → inventory (stock checks)
- All services → identity (JWT validation via `jwk-set-uri`)

### Networking

- `hosts.sh add` — adds all service hostnames to `/etc/hosts`
- `hosts.sh remove` — removes them
- Socat-proxied services: the compose container runs `alpine/socat` forwarding
  port 8080 to `host.docker.internal:8080`, so the app runs on the host but is
  reachable at its container IP by other services

## Observability

### Stack

| Hostname            | IP           | Port | Purpose                     |
|---------------------|--------------|------|-----------------------------|
| o11y-otel-collector | 172.20.0.100 | 4318 | OTLP HTTP ingestion         |
| o11y-prometheus     | 172.20.0.101 | 9090 | Metrics storage & querying  |
| o11y-tempo          | 172.20.0.102 | 3200 | Distributed trace storage   |
| o11y-loki           | 172.20.0.103 | 3100 | Log aggregation & querying  |
| o11y-grafana        | 172.20.0.104 | 3300 | Dashboards                  |

### Data Flow

```
App Services ──OTLP HTTP (4318)──▶ OTel Collector
                                      │
                        ┌──────────────┼──────────────┐
                        ▼              ▼              ▼
                     Tempo           Loki        Prometheus
                    (traces)        (logs)       (metrics)
                        └──────────────┼──────────────┘
                                       ▼
                                    Grafana
```

No ports are published to the host — query backends directly via container IPs.

### Querying

- **Traces (Tempo):** `curl http://o11y-tempo:3200/api/traces/<traceId>`
- **Logs (Loki):** `curl 'http://o11y-loki:3100/loki/api/v1/query_range?query={service_name="product"}'`
- **Metrics (Prometheus):** `curl 'http://o11y-prometheus:9090/api/v1/query?query=up'`
