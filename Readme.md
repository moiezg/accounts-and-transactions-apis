# 🧾 Account & Transaction API

A RESTful backend service built using **Spring Boot** that manages accounts and financial transactions with strong guarantees around **consistency**, **validation**, and **concurrency**.

This project is designed to reflect real-world financial system constraints and demonstrates clean architecture, transactional safety, disciplined testing, and **containerized execution using Docker**.

---

## 🚀 Features

- Create and retrieve accounts
- Create credit and debit transactions
- Enforce non-negative account balances
- Strong transactional guarantees (all-or-nothing updates)
- Concurrency-safe balance updates
- Clean separation between API, domain, and persistence layers
- Comprehensive test coverage (unit, integration, E2E)
- Dockerized build and runtime

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|------|---------|-------------|
| POST | `/accounts` | Create a new account |
| GET | `/accounts/{id}` | Retrieve account details |
| POST | `/transactions` | Create a transaction for an account |

---

## 📚 Business Rules

- Transactions are either **credit** or **debit**, determined by `OperationType`
- Debit transactions must **not** result in a negative balance
- All balance updates and transaction inserts occur inside a **single database transaction**
- If any part of the operation fails, **all changes are rolled back**
- Invalid input or rule violations return **HTTP 400**

---

## 🛠 Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Data JPA (Hibernate)
- Jackson (JSON serialization/deserialization)
- H2 (default in-memory database)
- Springdoc OpenAPI (Swagger)
- JUnit 5, Mockito, AssertJ
- Docker

---

## 📦 Getting Started (Local)

### Prerequisites

- Java 17+
- Maven

---

### Clone & Build

```bash
git clone https://github.com/moiezg/account-and-transaction-apis.git
cd account-and-transaction-apis
mvn clean install
```

---

### Run Locally (Without Docker)

```bash
mvn spring-boot:run
```

Application runs at:

```
http://localhost:8080
```

---

## 🐳 Running with Docker (Recommended)

### Prerequisites

- Docker (20+)
- Docker Compose (optional)

---

### Build Docker Image

From the project root:

```bash
docker build -t account-transaction-api .
```

---

### Run Docker Container

```bash
docker run -p 8080:8080 account-transaction-api
```

Application will be available at:

```
http://localhost:8080
```

---

### Stop Container

```bash
docker ps
docker stop <container_id>
```

---

## 📘 API Documentation (Swagger)

Once the application is running (locally or in Docker):

- Swagger UI  
  http://localhost:8080/swagger-ui.html

- OpenAPI Spec  
  http://localhost:8080/v3/api-docs

---

## 🧠 JSON Contract

### Create Transaction Request

```json
{
  "accountId": 1,
  "operationType": 3,
  "amount": 100.00
}
```

Notes:
- JSON uses **camelCase**
- `operationType` is a numeric value mapped to an enum via `@JsonCreator`
- Validation failures return **HTTP 400**

---

## 🏛 Architecture Overview

### Controller Layer
- Handles HTTP requests
- Performs request validation
- Maps domain responses to API DTOs

### Service Layer
- Contains business logic
- Defines transactional boundaries
- Coordinates persistence and validation

### Repository Layer
- Uses Spring Data JPA
- Encapsulates database access

### DTOs
- Implemented using Java **records**
- Immutable
- Used strictly at API boundaries

### Entities
- Mutable JPA-managed objects
- Never exposed directly to API consumers

---

## 🔄 Transaction Safety & Concurrency

- Account balance updates use **pessimistic locking** (`PESSIMISTIC_WRITE`)
- Concurrent updates to the same account are serialized at the database level
- Prevents lost updates and double-spending scenarios
- Lock timeout is configured to avoid indefinite blocking

---

## 🧪 Testing Strategy

### Unit Tests
- Test service orchestration and business logic
- Use Mockito
- No Spring context or database

### Integration Tests
- Validate JPA behavior
- Verify transactions, rollbacks, and locking
- Use real database interactions

### End-to-End (E2E) Tests
- Exercise the full stack using MockMvc
- Validate HTTP contracts, validation, and persistence

Run all tests:

```bash
mvn clean verify
```

---

## 📌 Assumptions Made

1. Single currency  
   All monetary values are assumed to be in the same currency.

2. Eager balance updates  
   Account balance is updated immediately per transaction.

3. Single balance per account  
   Accounts maintain one balance field.

4. No authentication or authorization  
   Security is intentionally out of scope.

5. No idempotency handling  
   Duplicate transaction submissions are not deduplicated.

6. Static operation types  
   `OperationType` values are predefined and not configurable.

---

## ⚖️ Trade-offs & Design Decisions

### Pessimistic Locking
- Guarantees correctness under concurrency
- Trades throughput for strong consistency

### Balance Field vs Ledger-Only Model
- Faster reads, simpler design
- Less flexible than full event sourcing

---

## 📄 License

This project is currently unlicensed.

---

## 📌 Final Notes

This API prioritizes **correctness, clarity, and transactional integrity**.

It is intentionally designed to demonstrate:
- Safe money handling
- Concurrency correctness
- Clean layering
- Production-grade testing