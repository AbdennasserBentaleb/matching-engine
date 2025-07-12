# In-Memory Matching Engine

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)

This repository contains a proof-of-concept order matching engine implemented as a Spring Boot microservice. It uses a price-time priority algorithm to match buy and sell orders entirely in-memory, keeping the critical execution path free of I/O blocking. Executed trades are asynchronously persisted to PostgreSQL and broadcast via Redis Pub/Sub.

A simple HTML/JS frontend is included to observe order book depth and trade execution.

---

## Architecture Overview

The engine relies on decoupled components to separate execution from persistence:

* **Order Book**: Each ticker uses its own `OrderBook` instance. We use a `TreeMap` to sort price levels and a `Deque` at each price level to maintain time priority during execution.
* **Matching Coordinator**: A `ConcurrentHashMap` manages order book lifecycles, instantiating them dynamically for new tickers.
* **Asynchronous Side-effects**: Writing to the database or publishing to Redis out-of-band prevents the core matching logic from blocking.
* **Pub/Sub**: Trades are serialized into JSON and pushed to a `trades_channel` in Redis.

Detailed design information is available in [ARCHITECTURE.md](docs/ARCHITECTURE.md) and [PRD.md](docs/PRD.md).

---

## Architecture Decisions & Trade-offs

* **JVM Ecosystem limitations**: While a true HFT (High-Frequency Trading) matching engine would be written in C++ or Rust to have granular control over memory allocation and avoid Garbage Collection pauses, this project deliberately uses Java. The goal is not to achieve sub-microsecond latencies, but to demonstrate the correct algorithmic implementation of price-time priority and asynchronous persistence boundaries within the JVM and Spring Boot ecosystems. Throughput over absolute latency is the target here.
* **Asynchronous Persistence**: We acknowledge that writing to the database *after* acknowledging the trade to the caller introduces a small window for data loss if the JVM crashes before the async thread completes the write. A write-ahead log (WAL) system would address this in a production system.

---

## Getting Started

### Prerequisites
* Java 17 or higher
* Docker and Docker Compose

### Option 1: Run with Docker Compose
To start the entire infrastructure (PostgreSQL, Redis, and the Application), run:

```bash
docker-compose up -d
```

The service and Web UI will be available on port `8888`. Access the application at [http://localhost:8888/](http://localhost:8888/).

### Option 2: Local Development Mode
To run the application locally on the host machine using Docker strictly for the database and cache layers:

**1. Start Infrastructure:**
```bash
docker-compose up -d postgres redis
```

**2. Build the application:**
```bash
./mvnw clean package -DskipTests
```

**3. Run the service:**
```bash
java -jar target/matching-engine-0.0.1-SNAPSHOT.jar
```

The service will be available on port `8080`. Access the Web UI at [http://localhost:8080/](http://localhost:8080/).

---

## API Documentation

### Place Limit Order
`POST /api/v1/orders`

**Request Body:**
```json
{
  "ticker": "BTC-USD",
  "side": "BUY",
  "price": 50000.00,
  "quantity": 1.5
}
```

**Response:** 
Returns a list of `Trade` objects for any immediate matches. An empty array response (`[]`) indicates the order was added to the resting order book as liquidity.

### Get Order Book Snapshot
`GET /api/v1/orderbook/{ticker}`

**Response:** 
Returns the current state of the order book for the specified ticker, aggregated by price level.

---

## Testing

Run the unit test suite to verify the matching logic:

```bash
./mvnw test
```

The tests cover exact matches, partial fills, and price-time priority execution constraints.

---

## Deployment

Kubernetes manifests (Deployments, Services, and Ingress) are located in the `k8s/` directory.

```bash
kubectl apply -f k8s/
```
