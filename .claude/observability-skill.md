# Observability Skill

## Stack Overview

The observability stack runs via Podman Compose. All services are on a shared
network and mapped to port on localhost.


| Component          | Image                                      | Port  | Purpose                          |
|--------------------|--------------------------------------------|-------|----------------------------------|
| OTel Collector     | otel/opentelemetry-collector-contrib       | 4318  | OTLP HTTP ingestion pipeline     |
| Prometheus         | prom/prometheus                            | 9090  | Metrics storage & querying       |
| Loki               | grafana/loki                               | 3100  | Log aggregation & querying       |
| Tempo              | grafana/tempo                              | 3200  | Distributed trace storage        |
| Grafana            | grafana/grafana                            | 3300  | Dashboards (queries all backends)|

## Data Flow

```
App Services (product, identity, cart)
        |
        | OTLP HTTP (port 4318)
        v
  OTel Collector
   /    |     \
  v     v      v
Tempo  Loki  Prometheus
(traces) (logs) (metrics)
        \   |   /
         Grafana (port 3300)
```

The OTel Collector is a pipeline only — it does not store or serve data. Always query the backends directly.

Trace IDs correlate data across all three backends.

## App Services

| Service   | Internal Port | External Port | Database Port |
|-----------|---------------|---------------|---------------|
| product   | 8080          | via gateway   | 5433          |
| identity  | 8080          | 10000         | 5432          |
| cart      | 8080          | via gateway   | 5434          |
| gateway   | 80            | 8090          | —             |

Gateway is Caddy, routing to backend services.
