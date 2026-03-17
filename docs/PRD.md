# Product Requirements Document

## Overview

The goal of this project is to develop a low-latency, in-memory matching engine for financial instruments with a fully integrated real-time web interface. By processing orders entirely in RAM, it eliminates the database I/O overhead that typically limits the throughput of traditional order matching systems. The built-in frontend provides instant visual feedback for order book states and resting liquidity.

## Key Objectives

1. **Low Latency Matching**: Ensure sub-millisecond execution by matching orders strictly within memory-resident data structures.
2. **Price-Time Priority**: Implement a deterministic execution model where the best price and oldest orders are matched first.
3. **Asynchronous Trade Settlement**: Offload trade persistence and market data broadcasting to background threads to protect the matching engine's performance.
4. **Scalability**: Support multiple trading pairs concurrently by isolating state within ticker-specific data structures.
5. **Real-Time Visualization**: Provide a zero-build static frontend served directly from the Spring Boot application for immediate demonstration and testing.

## Functional Requirements

### Order Management

* Accept limit orders via a REST API and the web interface.
* Validate order parameters (ticker, side, price, quantity) before processing.
* Assign unique identifiers and server-side timestamps to every incoming order.

### Matching Engine logic

* Execute matches using a strict price-time priority algorithm.
* Support partial fills, leaving the remaining quantity in the order book.
* Provide real-time snapshots of aggregated liquidity at each price level.

### Settlement and Distribution

* Persist every executed trade to a relational database for auditing and history.
* Broadcast trade data through a message bus (Redis) to support downstream consumers and real-time UI updates.

### Frontend Interface

* Serve a static HTML/JS/CSS application from `/`.
* Provide a form for order entry (Ticker, Side, Price, Quantity).
* Display a live-updating Order Book with depth visualization.
* Display a chronological feed of recent trades.

## Technical Requirements

* **Language**: Java 17
* **Framework**: Spring Boot 3
* **Frontend**: Vanilla HTML5, CSS3, ES6 JavaScript (No build step required)
* **In-Memory State**: Custom thread-safe data structures for order books.
* **Persistence**: PostgreSQL handled asynchronously.
* **Streaming**: Redis Pub/Sub for trade broadcasting.

## Scope Definitions

* **In Scope**: Limit orders, GTC (Good-Til-Cancelled) behavior, asynchronous persistence, real-time market data broadcasting, and a functional web frontend.
* **Out of Scope**: Order cancellations, market orders, historical order recovery into memory, and API authentication.
