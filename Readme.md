# Pismo Account & Transaction API

## 1. What this application is about

This application is a **RESTful backend service** that manages:

* **Accounts**, identified by a document number
* **Financial transactions** associated with accounts
* **Business rules** around transaction types (debit vs credit)
* **Data consistency** using database transactions and constraints

The system is designed with **clean architecture**, **strong test coverage**, and a proper **testing pyramid** (unit, integration, and end-to-end tests).

---

## 2. Application components

### APIs

#### Account APIs

| Method | Endpoint         | Description            |
| ------ | ---------------- | ---------------------- |
| POST   | `/accounts`      | Create a new account   |
| GET    | `/accounts/{id}` | Fetch an account by ID |

#### Transaction APIs

| Method | Endpoint        | Description                         |
| ------ | --------------- | ----------------------------------- |
| POST   | `/transactions` | Create a transaction for an account |

---

### Database

* **Relational database using JPA/Hibernate**
* Core tables:

  * `accounts`
  * `transactions`
* Constraints:

  * Unique `document_number` on accounts
  * Foreign key from transactions → accounts

---

### Core layers

* **Controller layer**: HTTP handling and validation
* **Service layer**: business logic and transactions
* **Repository layer**: persistence (Spring Data JPA)
* **DTOs**: immutable request/response objects (API contracts)
* **Entities**: JPA-managed persistence models

---

## 3. API Documentation (Swagger / OpenAPI)

This application exposes **interactive API documentation** using **OpenAPI 3 (Swagger UI)** via **springdoc-openapi**.

### Swagger UI

Once the application is running, you can access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

The Swagger UI provides:

* All available endpoints
* Request and response schemas
* Field-level documentation and examples
* Enum values and constraints

### OpenAPI specification

The raw OpenAPI specification is available at:

```
http://localhost:8080/v3/api-docs
```

This can be used to:

* Generate client SDKs
* Import into Postman / Insomnia
* Export OpenAPI JSON or YAML

### Documentation approach

* Swagger annotations are applied **only to API-facing DTOs**
* Database entities are **not** coupled to Swagger
* Request DTOs define validation rules and constraints
* Response DTOs define the public API contract

This ensures a clean separation between **persistence models** and **API documentation**.

---

## 4. Dependencies

### Runtime

* Java 17+ (tested up to Java 23)
* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* H2 (local)
* Jackson (JSON serialization)
* Springdoc OpenAPI (Swagger UI)

### Testing

* JUnit 5
* AssertJ
* Spring Boot Test
* MockMvc

---

## 5. Run and build the application

### Build the Docker image

```bash
docker build -t pismo-api .
```

### Run the Docker image

```bash
docker run -p 8080:8080 pismo-api
```

After startup, Swagger UI will be available at:

```
http://localhost:8080/swagger-ui.html
```

---

## 6. How to clone the repository using a Personal Access Token (PAT)

### Step 1: Generate a Personal Access Token

Create a token from your Git provider (GitHub / GitLab / Bitbucket) with **repository read access**.

### Step 2: Clone using curl

```bash
curl -u YOUR_USERNAME:YOUR_PERSONAL_ACCESS_TOKEN \
  https://github.com/moiezg/account-and-transaction-apis
```