# Event-Driven Order Processing with Microservices & Saga Pattern

[![Spring Boot 3.3.3](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-red.svg)](https://kafka.apache.org/)
[![OpenTelemetry](https://img.shields.io/badge/Observability-OpenTelemetry%20%2B%20Jaeger-blue.svg)](https://opentelemetry.io/)
[![PostgreSQL](https://img.shields.io/badge/Databases-PostgreSQL%20(Isolated)-336791.svg)](https://www.postgresql.org/)

An enterprise-grade, distributed event-driven order pipeline demonstrating the core patterns of scalable distributed systems: **Saga Choreography**, **Transactional Outbox**, **Idempotent Consumers**, **Dead Letter Queues (DLQ)** with **Administrative Replay**, and **OpenTelemetry Distributed Tracing**.

---

## 🏛️ Architecture Overview

```
                      +-----------------------------+
                      |   Client / API Gateway      |
                      +--------------+--------------+
                                     | POST /api/orders (202 Accepted)
                                     v
+-----------------------------------------------------------------------------------+
|  ORDER SERVICE (:8081)                                                            |
|  - Creates Order (PENDING)                                                        |
|  - Atomic Write: (orders + outbox_events) -----------------+                      |
|  - Outbox Relay Poller                                     |                      |
+------------------------------------------------------------+----------------------+
                                                             |
                                                             | (1) order.created
                                                             v
+-----------------------------------------------------------------------------------+
|  INVENTORY SERVICE (:8083)                                 |                      |
|  - Deduplication via processed_events                      |                      |
|  - Stock Check & Reservation                               |                      |
|  - Outbox Relay -------------------------------------------+                      |
+------------------------------------------------------------+----------------------+
                                                             |
                                                             | (2) inventory.reserved
                                                             v
+-----------------------------------------------------------------------------------+
|  PAYMENT SERVICE (:8082)                                   |                      |
|  - Deduplication via processed_events                      |                      |
|  - Card Processing (Mock Gateway / Failure Injection)      |                      |
|  - Outbox Relay -------------------------------------------+                      |
+------------------------------------------------------------+----------------------+
                                     |                                 |
           (3a) payment.completed    |       (3b) payment.failed       |
                                     v                                 v
+---------------------------------------------------+   +---------------------------+
|  NOTIFICATION SERVICE (:8084)                     |   | SAGA COMPENSATING ROLLBACK|
|  - Customer Confirmation Email                    |   | - Inventory Releases Stock|
|  - Order Confirmed Event                          |   | - Order Marked CANCELLED  |
+---------------------------------------------------+   +---------------------------+
```

---

## 🚀 Key Distributed Systems Features

| Pattern / Concept | Description & Implementation |
| :--- | :--- |
| **Database-Per-Service** | 4 isolated PostgreSQL databases (`order_db`, `payment_db`, `inventory_db`, `notification_db`). Zero shared schema. |
| **Transactional Outbox** | Eliminates dual-write inconsistencies by writing domain entities and outbox events in a single local ACID transaction. An asynchronous poller streams them to Kafka. |
| **Saga Choreography** | Linear event choreography. When payment declines, a `payment.failed` compensating event automatically refunds reserved warehouse inventory and transitions order status to `CANCELLED`. |
| **Idempotent Consumers** | Deduplication keyed on `eventId` via `processed_events` table. Safe against duplicate message storms, network retries, and Kafka rebalances. |
| **Dead Letter Queue (DLQ)** | Non-recoverable poison pill events are routed to `.DLT` topics after retry exhaustion. Includes a dedicated DLQ Admin UI (`:8085`) to inspect payloads and trigger message replay. |
| **Distributed Tracing** | OpenTelemetry W3C `traceparent` context propagated through HTTP headers and Kafka record headers, visualized end-to-end in Jaeger UI. |

---

## 📂 Project Structure

```
.
├── pom.xml                                   # Root Maven Multi-Module descriptor
├── common/                                   # Shared contracts, envelopes, Outbox entities, Idempotency handler
├── monolith-service/                         # Step 1: All-in-one ACID benchmark service (:8080)
├── order-service/                            # Step 2-8: Order Orchestrator & Outbox Relay (:8081)
├── payment-service/                          # Step 2-8: Payment Service & Latency Simulator (:8082)
├── inventory-service/                        # Step 3-8: Inventory Service & Saga Compensation (:8083)
├── notification-service/                     # Step 3-8: Email/SMS Notifications (:8084)
├── dlq-admin-service/                        # Step 7: DLQ Inspector & Replay Web UI (:8085)
├── docker/                                   # Docker Compose (Kafka KRaft, Postgres, Jaeger, Services)
│   └── docker-compose.yml
├── k8s/                                      # Complete Kubernetes Production Manifests
│   ├── 00-namespace.yaml
│   ├── 01-postgres.yaml
│   ├── 02-kafka.yaml
│   ├── 03-jaeger.yaml
│   └── 04-microservices.yaml
├── scripts/                                  # 8 Step-by-step demonstration & verification scripts
│   ├── 01_test_monolith.ps1
│   ├── 02_test_sync_degradation.ps1
│   ├── 03_test_eventual_consistency.ps1
│   ├── 04_test_outbox_durability.ps1
│   ├── 05_test_idempotency_storm.ps1
│   ├── 06_test_saga_compensation.ps1
│   ├── 07_test_dlq_and_replay.ps1
│   └── 08_verify_opentelemetry_traces.ps1
└── SKILL.md                                  # Comprehensive Distributed Systems & Interview Master Guide
```

---

## 🛠️ Step-by-Step Build & Verification Order

### Step 1: Monolith Baseline
Run the monolith service to benchmark the simplicity of single-database ACID transactions:
```powershell
./scripts/01_test_monolith.ps1
```

### Step 2: Synchronous Microservice Latency Degradation
Demonstrates how synchronous HTTP chains create thread starvation and cascading latency:
```powershell
./scripts/02_test_sync_degradation.ps1
```

### Step 3: Kafka Asynchronous Event-Driven Flow & Eventual Consistency
Demonstrates sub-20ms HTTP 202 responses with asynchronous status transitions:
```powershell
./scripts/03_test_eventual_consistency.ps1
```

### Step 4: Transactional Outbox Pattern & Durability
Verifies atomic event enqueuing and outbox relay processing:
```powershell
./scripts/04_test_outbox_durability.ps1
```

### Step 5: Idempotent Consumer Deduplication Storm
Floods consumers with duplicate event IDs and verifies zero double-charging or double stock deductions:
```powershell
./scripts/05_test_idempotency_storm.ps1
```

### Step 6: Saga Pattern & Compensating Transactions
Simulates payment decline and verifies automated stock refund in warehouse:
```powershell
./scripts/06_test_saga_compensation.ps1
```

### Step 7: Dead Letter Queue (DLQ) & Message Replay Center
Simulates an unrecoverable poison pill, verifies dead letter routing, and replays the message from the Web UI:
```powershell
./scripts/07_test_dlq_and_replay.ps1
```
*Access the DLQ Admin Dashboard at: [http://localhost:8085](http://localhost:8085)*

### Step 8: OpenTelemetry Distributed Tracing
Tracks end-to-end trace context across all 4 services:
```powershell
./scripts/08_verify_opentelemetry_traces.ps1
```
*Access the Jaeger UI at: [http://localhost:16686](http://localhost:16686)*

---

## 🐳 Quickstart with Docker Compose

```bash
cd docker
docker-compose up -d
```

Check running services:
- **Order Service API**: `http://localhost:8081/api/orders`
- **Payment Service API**: `http://localhost:8082/api/payment`
- **Inventory Service API**: `http://localhost:8083/api/inventory`
- **Notification Service API**: `http://localhost:8084/api/notifications`
- **DLQ Replay Dashboard**: `http://localhost:8085`
- **Jaeger Distributed Tracing**: `http://localhost:16686`

---

## ☸️ Kubernetes Deployment

Deploy the complete cluster to Kubernetes:
```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-postgres.yaml
kubectl apply -f k8s/02-kafka.yaml
kubectl apply -f k8s/03-jaeger.yaml
kubectl apply -f k8s/04-microservices.yaml
```

---

## 📝 Resume Bullet Points

- *Architected an event-driven order processing pipeline across 4 microservices using Spring Boot 3, Apache Kafka, and PostgreSQL, achieving sub-25ms API response latency.*
- *Eliminated dual-write discrepancies by implementing the Transactional Outbox pattern with an asynchronous relay scheduler, ensuring guaranteed at-least-once message delivery.*
- *Designed choreographed Saga with compensating transactions to guarantee eventual consistency and automated inventory rollback across isolated database schemas upon payment declines.*
- *Implemented idempotent consumers backed by persistent deduplication keys, successfully handling duplicate message storms with zero double-charging or stock mismatches.*
- *Constructed an automated Dead Letter Queue (DLQ) topology with exponential backoff retries and an administrative replay dashboard for zero-data-loss incident mitigation.*
- *Integrated OpenTelemetry and Jaeger distributed tracing across HTTP endpoints and Kafka record headers, enabling full end-to-end request visibility across service boundaries.*
