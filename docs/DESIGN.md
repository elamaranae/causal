# Causal Design

E-commerce study project for understanding real-life problems in microservices.

## Services

### Identity Service

Registers and logs in users using stateless JWTs signed by a private key. Other
services validate tokens using the public key exposed via an internal JWKS
endpoint.

#### Models

User: name, email (unique), password_hash

Refresh Token: user_id, token (unique), expiry

### Profile Service

#### Models

User Profile: user_id (unique), first_name, last_name, currency,
default_address_id

Address: user_id, label, line_1, line_2, city, state, country, pincode,
phone_number

### Product Service

Stores the entire catalog of products available for selling.

#### Models

Product: name, description, attributes, vendor_id, category_id,
primary_thumbnail_url

SKU: product_id, variant_attributes, attributes, media_id,
primary_thumbnail_url, is_default

Price: sku_id, effective_from, price_currency, price_amount
`unique(sku_id, effective_from, price_currency)`

Product Media: media[]{ type, url, thumbnail, primary }

Vendor: name, description, logo

Product Category: name, description, parent_id

Review: user_id, product_id `unique(user_id, product_id)`, rating, subject,
description

### Cart Service

Stores products selected for ordering.

#### Models

Cart: user_id (unique), session_id (unique)

Cart Item: cart_id, sku_id `unique(cart_id, sku_id)`, quantity

### Inventory Service

Tracks stock availability for all products.

#### Models

Stock: sku_id (unique), product_id, quantity, available

Reservation: order_id, sku_id, quantity, expires_at
`index(sku_id, expires_at)`, `index(order_id)`

### Order Service

#### Models

Order: user_id, status, idempotency_key (unique), total_amount,
total_currency, address (embedded: name, line_1, line_2, city, state, country,
pincode, phone_number)

Order Item: sku_id, order_id `unique(order_id, sku_id)`, delivery_status,
quantity, purchase_amount, purchase_currency, sku_name, sku_description

### Payment Service

#### Models

Payment: order_id, idempotency_key (unique), payment_method, amount,
currency, status

---

## Authentication

### Cookie-Based (Web)

HTTP-only, SameSite Lax cookies with JWT access token. Gateway checks CSRF
token using double-submit technique.

- Hashed refresh tokens stored in the database.
- Login sets a refresh token cookie scoped to the refresh endpoint path.
- Login sets a short-lived access token cookie for all paths.
- Client retries via the refresh endpoint on auth failure.

### Authorization Header (Mobile)

The same login endpoints also return tokens in the JSON response body. Mobile
clients store them locally and send them in the Authorization header.

### Long-Lived API Tokens

Stored in the database. Distinguished from access tokens by a known prefix.
Gateway forwards them to `identity`, which mints a short-lived JWT for use
with other services.

### Endpoints

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout` (clears cookies)

---

## Checkout Flow

1. User clicks checkout.
2. Order service receives the request.
    1. Creates order record with status `pending_reservation`.
    2. Calls cart service to get the current user's items.
    3. Calls profile service to fetch the user's currency.
    4. Calls product service to fetch recent prices.
    5. Calls inventory service to reserve the items and quantity. **(critical section)**
        - Checkout fails if unable to reserve; order marked as `reservation_failed`.
    6. Creates order items, marks order as `reserved`, publishes `event.order_status`,
       responds 200 with order and amount details.
3. Frontend redirects to payment page.
4. User fills in address and payment details, clicks place order with the order ID.
5. Order service receives payment request.
    1. Calls inventory to extend the reservation by 5 min. **(critical section)**
       Max lifetime capped at 25 min from creation.
        - If the reservation has expired, marks order as `reservation_expired` and fails.
    2. Encrypts payment details (AES/GCM) into outbox payload, sets status to
       `payment_initiated`, returns 200.
6. Frontend redirects to waiting page and short-polls order show API.
7. RabbitMQ worker (`PaymentProcessor`) picks up `job.initiate_payment` and
   calls payment service. Safe to retry since payment service uses idempotent keys.
8. Payment provider sends a webhook to the order service.
    1. Marks order as `payment_success` or `payment_failed`, atomically adds
       an outbox record for `event.payment_completed`. Order status guards
       idempotency on webhook retries.
9. RabbitMQ worker (`EventForwarder`) picks up `event.*` messages and publishes
   them to Kafka topic `order.events`.
10. Inventory service consumes `event.payment_completed` from Kafka.
    1. If `payment_success` and reservation exists, atomically:
        - Deletes reservation (stock already decremented at reserve time).
        - Adds `job.complete_order` outbox record with `order_success`.
        - Adds entry in processed events table for idempotency.
    2. If `payment_failed` or reservation expired, atomically:
        - Restores available stock from reservation quantities.
        - Deletes reservation.
        - Adds `job.complete_order` outbox record with `order_failed`.
        - Adds entry in processed events table for idempotency.
11. RabbitMQ worker (`OrderCompleter`) picks up `job.complete_order` and calls
    the order service webhook. Order service sets the final status. If failed,
    adds a `job.refund` outbox record.
12. Frontend shows success or failure based on order status in show API poll.

### Reservation Logic

```
if current user already has reservation for all sku ids and quantities, respond 200 OK

# try to reserve
begin transaction
    for sku in skus
        update sku set available = available - requested where available >= requested
        track failed sku ids
    end
    if any failed, rollback
    else insert reservations for each sku (expires in 10 min)
commit

# reclaim expired stock only for failed skus
if failed_sku_ids.present?
    for sku_id in failed_sku_ids
        begin transaction
            select stock for update where sku_id = sku_id
            expired = delete reservations where sku_id = sku_id and expires_at <= now
            update stock set available = available + sum(expired.quantity)
        commit
    end
end

# retry with reclaimed stock
begin transaction
    for sku in skus
        update sku set available = available - requested where available >= requested
    end
    if any failed, rollback and raise conflict with failed sku ids
    else insert reservations
commit
```

---

## Messaging

### Events vs Commands

Kafka for cross-service events (order placed, payment completed). RabbitMQ
within a service for background jobs, retries, scheduled tasks.

### Outbox Pattern with Debezium CDC

All outbox events are streamed to RabbitMQ by Debezium Server via CDC.

```
Postgres WAL → Debezium Server → RabbitMQ
```

At-least-once guarantee chain:

Postgres → Debezium: Debezium opens a logical replication slot and the database
streams WAL entries over a persistent connection. Debezium only advances the
slot's confirmed position after it has durably processed events. Postgres won't
recycle WAL segments Debezium hasn't acknowledged.

Debezium → RabbitMQ: Publisher confirms enabled. Debezium moves its own offset
only after RabbitMQ acks the message.

Consumer-side idempotency is handled via a processed events (inbox) table since
CDC only guarantees at-least-once delivery.

#### Outbox Table

id (UUID), aggregate_type, aggregate_id, event_type, payload, created_at

#### Inbox Table (Processed Events)

event_id, consumer_id, `primary key(event_id, consumer_id)`

Outbox cleanup: delete after processing or run a periodic job. Inbox cleanup:
delete records older than the RabbitMQ message TTL or Kafka log retention.

### SMT Event Router

Debezium's Single Message Transform extracts the `payload` column as the
message body and uses `aggregate_type` as the routing key.

```
topic   ←  aggregate_type  =  "event.contact"
key     ←  aggregate_id    =  "456"
body    ←  payload         =  { "name": "Asha" }
headers ←  id, type
```

### RabbitMQ Setup

- Single exchange: `outbox`
- `aggregate_type` used as routing key
- Each job type gets its own queue, all event types share one queue
- Each queue has its own dead-letter queue

Sample `aggregate_type` values:

```
events:  event.contact   event.payment   event.order
jobs:    job.email       job.initiate_payment
```

### Ordering

RabbitMQ writes to Kafka may arrive out of order. Techniques to handle this:

1. Event versioning: track last processed version in inbox, skip older.
2. Sequencer: re-sequence events within a time window before processing.
3. Park method: if a later version arrives first, requeue it until the earlier
   events show up.

### Record of Intents

A record of intent doesn't have to be an outbox table, and an idempotency record
doesn't have to be an inbox table. Regular domain records can serve both purposes.

Re-using outbox as inbox breaks down when:
- Dedup records need to live longer than intents (intents get deleted after
  processing, but idempotency records need to stick around for the retry window).
- One incoming event fans out to multiple intents.

Execution: push for the happy path, poll as a safety net.

---

## Observability

### Stack

Spring Actuator for health checks and runtime log-level changes via
`/actuator/loggers`.

Micrometer for business metrics + auto-configured JVM/HTTP/Hikari/Tomcat metrics.

OpenTelemetry Java Agent for distributed traces, pushes OTLP to Collector.

Logback for structured logs.

`spring-boot-starter-opentelemetry` is the all-in-one starter for Spring Boot
that bundles actuator, micrometer tracing with OTel bridge, and OTLP exporters.

### Data Flow

```
App Services ──OTLP HTTP──▶ OTel Collector
                                │
                  ┌─────────────┼─────────────┐
                  ▼             ▼             ▼
               Tempo          Loki       Prometheus
              (traces)       (logs)      (metrics)
                  └─────────────┼─────────────┘
                                ▼
                             Grafana
```

Spans are linked using the `traceparent` header. A service checks the header;
if present, it uses it as the trace ID, otherwise it creates one and passes it
along. The backend connects all spans with the same trace ID.

MDC is a thread-local storage mechanism in SLF4J/Logback used to attach
contextual data like `requestId` and `userId` to logs throughout a request's
lifecycle. Since it's thread-local, it doesn't carry over to async threads and
needs to be cleared in a filter's finally block to avoid leakage.

---

## Transparent Local Service Substitution

Allows faster development by running a microservice locally while connecting
with other microservices inside containers seamlessly, with minimal code changes.

### Problem

Docker's network is not reachable from macOS by default. Docker and the host
use different DNS resolvers.

### Solution

- Compose services should be reachable from both containers and the host using
  the same hostname.
  - Use static IPs in Docker Compose for each service.
  - Use Orbstack or `docker-mac-net-connect` to make container IPs routable
    from host.
  - Add the static IP to service hostname mapping in `/etc/hosts`.

- Any service in Docker Compose can be overridden with a `socat` image to
  forward traffic to the locally running service:

```yaml
product-app:
  image: alpine/socat
  command: tcp-listen:8080,fork,reuseaddr tcp-connect:host.orb.internal:8080
```

`socat` acts as a transport-layer gateway, forwarding traffic from the container
to the local process.

---

## What I'd do differently in production

- Saga orchestrator. Choreography works for the current linear checkout but gets
  messy with complex compensation logic.
- Gateway-level JWT validation instead of each service doing it. Saves a JWKS
  fetch on cold start per service.
- Multi-broker Kafka. Single broker means no replication.
- Redis or similar for caching product/inventory data. Right now every read is a
  synchronous service call.
- Scheduled job for reservation expiry instead of only lazy reclamation. Under
  load, expired reservations can temporarily eat into available stock.
