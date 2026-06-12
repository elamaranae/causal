[![test](https://github.com/elamaranae/causal/actions/workflows/test.yml/badge.svg)](https://github.com/elamaranae/causal/actions/workflows/test.yml)

E-commerce microservices project. Built to learn how distributed systems
actually work: checkout flows, inventory reservations, outbox/CDC patterns,
observability.

## Architecture

```
                         ┌──────────┐
                         │  Caddy   │
                         │ Gateway  │
                         └────┬─────┘
                              │
        ┌──────────┬──────────┼──────────┬──────────┐
        ▼          ▼          ▼          ▼          ▼
   ┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐
   │Identity ││ Product ││  Cart   ││  Order  ││ Profile │
   │         ││         ││         ││         ││         │
   └────┬────┘└────┬────┘└────┬────┘└────┬────┘└────┬────┘
        │          │          │          │          │
        ▼          ▼          ▼          ▼          ▼
      [PG]       [PG]       [PG]       [PG]       [PG]
                                         │
                              ┌──────────┼──────────┐
                              ▼          ▼          ▼
                          Debezium   RabbitMQ     Kafka
                           (CDC)     (jobs)    (events)
                                         │
                                         ▼
                                    ┌─────────┐
                                    │Inventory│
                                    └────┬────┘
                                         ▼
                                        [PG]
```

Each service owns its Postgres database. Synchronous calls over REST, Kafka for
cross-service events, RabbitMQ for background jobs. Outbox delivery via
Debezium CDC.

## Services

| Service     | What it does                                  |
|-------------|-----------------------------------------------|
| Identity    | Auth with stateless JWT, exposes JWKS          |
| Profile     | User profiles and addresses                    |
| Product     | Catalog, SKUs, pricing, reviews                |
| Cart        | Shopping cart, checks stock via inventory       |
| Inventory   | Stock tracking, reservations with expiry        |
| Order       | Checkout orchestration, payment coordination    |

## What's interesting

- Outbox + CDC: Debezium tails Postgres WAL and pushes to RabbitMQ. No polling.
- Inventory reservations with lazy reclamation on contention.
- Idempotency at every boundary: outbox/inbox tables, payment idempotency keys,
  order status guards on webhook retries.
- Kafka for events between services, RabbitMQ for jobs within a service.
- Each service validates JWTs on its own using identity's JWKS endpoint.

Full design doc: [docs/DESIGN.md](docs/DESIGN.md)

## Stack

Java 21, Spring Boot, PostgreSQL, Kafka, RabbitMQ, Debezium Server, Caddy,
OpenTelemetry, Prometheus, Tempo, Loki, Grafana, Gradle, Docker Compose.

## Running it

Needs Docker, Java 21, and Orbstack (or
[docker-mac-net-connect](https://github.com/chipmk/docker-mac-net-connect)) on
macOS.

```bash
sudo ./hosts.sh add

cd observability && docker compose up -d
cd ../
docker compose up -d
```

Seed the databases (identity, product, inventory):

```bash
./seed.sh
```

Run a service locally instead of in Docker:

```bash
cd <service> && ./gradlew bootRun
```

Tests:

```bash
cd <service> && ./gradlew test
```

Cleanup:

```bash
docker compose down
sudo ./hosts.sh remove
```
