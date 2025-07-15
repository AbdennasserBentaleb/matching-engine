# In-Memory Matching Engine

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://docs.docker.com/compose/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Manifests-326CE5.svg)](https://kubernetes.io/)

A cloud-native, in-memory order matching engine built on Spring Boot. It implements a **price-time priority algorithm** to match buy and sell orders entirely in RAM, keeping the critical execution path free of I/O. Executed trades are asynchronously persisted to PostgreSQL and broadcast via Redis Pub/Sub. A built-in vanilla JS frontend lets you observe order book depth and live trade execution.

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST Client / Web UI                     │
└──────────────────────────────┬──────────────────────────────────┘
                               │ POST /api/v1/orders
                               ▼
                      ┌────────────────┐
                      │ OrderController │
                      └───────┬────────┘
                              │ processOrder(order)
                              ▼
                  ┌───────────────────────┐
                  │  MatchingEngineService │
                  │  (ConcurrentHashMap)   │
                  └────────────┬──────────┘
                               │ per-ticker dispatch
                               ▼
                  ┌────────────────────────┐
                  │       OrderBook        │   ← ReentrantLock per ticker
                  │  TreeMap<Price,Deque>  │   ← Price-time priority
                  └────────────┬───────────┘
                               │ List<Trade>
                               ▼
                  ┌────────────────────────┐
                  │  TradeEventPublisher   │
                  │  (ApplicationEvent)    │
                  └──────┬─────────────────┘
              ┌──────────┘           └──────────┐
              │ @Async                           │ @Async
              ▼                                 ▼
  ┌───────────────────┐             ┌─────────────────────┐
  │ AsyncTradePersister│            │  MarketDataPublisher │
  │  (PostgreSQL JPA)  │            │  (Redis Pub/Sub)     │
  └───────────────────┘             └─────────────────────┘
```

The matching coordinator uses `ConcurrentHashMap.computeIfAbsent` for lock-free book creation, while each `OrderBook` uses a `ReentrantLock` for atomic matching. Side effects (DB writes, Redis broadcasts) run on a dedicated `ThreadPoolTaskExecutor`, keeping matching latency unaffected by I/O.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.3.6 |
| **Persistence** | Spring Data JPA + PostgreSQL 15 |
| **Caching / Pub-Sub** | Spring Data Redis 7 |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Observability** | Spring Boot Actuator (health, liveness, readiness) |
| **Frontend** | Vanilla HTML5 / CSS3 / ES6 JS (zero build step) |
| **Containerization** | Docker (multi-stage, non-root) + Docker Compose |
| **Orchestration** | Kubernetes (Deployment, Service, Ingress manifests) |
| **Testing** | JUnit 5, Testcontainers (PostgreSQL + Redis) |

---

## Architecture Decisions & Trade-offs

- **JVM Ecosystem**: A production HFT engine would be C++ or Rust for sub-microsecond latency. This project intentionally uses Java to demonstrate correct price-time priority logic within the JVM and Spring ecosystem. Throughput over absolute latency is the target.
- **Asynchronous Persistence**: Trade acknowledgement happens before the DB write completes, introducing a small window for data loss on JVM crash. A production system would use a Write-Ahead Log (WAL) to address this.
- **In-Memory State**: Resting orders are not recovered from the database on restart. PostgreSQL is an immutable record of executed trades; matching state is transient by design.

Detailed design: [ARCHITECTURE.md](docs/ARCHITECTURE.md) | [PRD.md](docs/PRD.md)

---

## Getting Started

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) and Docker Compose v2
- Java 17+  *(only needed for local development mode)*

### Option 1: Run with Docker Compose (Recommended)

Starts PostgreSQL, Redis, and the application — all wired together with health checks:

```bash
docker-compose up -d
```

All three services will be healthy before the application starts. The Web UI and API are available at **[http://localhost:8888](http://localhost:8888)**.

To tail logs:
```bash
docker-compose logs -f matching-engine
```

### Option 2: Local Development Mode

Run infrastructure in Docker, and the application on the host JVM:

**1. Start infrastructure:**
```bash
docker-compose up -d postgres redis
```

**2. Build the JAR:**
```bash
./mvnw clean package -DskipTests
```

**3. Run the application:**
```bash
java -jar target/matching-engine-0.0.1-SNAPSHOT.jar
```

The service will be available on **[http://localhost:8080](http://localhost:8080)**.

---

## API Documentation

Interactive Swagger UI is available at:
- **Docker:** [http://localhost:8888/swagger-ui.html](http://localhost:8888/swagger-ui.html)
- **Local:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Raw OpenAPI spec: `/v3/api-docs`

### Place Limit Order
`POST /api/v1/orders`

```json
{
  "ticker": "BTC-USD",
  "side": "BUY",
  "price": 50000.00,
  "quantity": 1.5
}
```

**Response:** A JSON array of `Trade` objects for any immediate matches. An empty array (`[]`) means the order rested on the book as liquidity.

### Get Order Book Snapshot
`GET /api/v1/orderbook/{ticker}`

Returns the aggregated bid/ask depth for the given ticker.

---

## Testing

Run the full test suite (unit + Testcontainers integration tests):

```bash
./mvnw test
```

Tests cover:
- **Exact match**: Full fill at a single price level
- **Partial fill**: Remaining quantity rests on the book
- **Price-time priority**: Best price matched first, FIFO within a price level
- **Concurrency**: 100 concurrent threads (50 buys + 50 sells) produce exactly 50 trades with no race conditions
- **Integration (E2E)**: Full HTTP request lifecycle against live Postgres and Redis containers

---

## Observability

Spring Boot Actuator endpoints:

| Endpoint | Description |
|---|---|
| `/actuator/health` | Full health status |
| `/actuator/health/liveness` | K8s liveness probe |
| `/actuator/health/readiness` | K8s readiness probe |
| `/actuator/metrics` | JVM and application metrics |

---

## Deployment

Kubernetes manifests (Deployment, Service, Ingress) are in the `k8s/` directory. The Deployment uses Secrets for credentials and configures liveness/readiness probes against the Actuator endpoints.

```bash
kubectl apply -f k8s/
```

> **Note:** Create the `matching-engine-secrets` Secret with `database-url`, `database-username`, and `database-password` keys before deploying.
