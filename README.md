# 🏦 Banking Application — Microservices Backend

A backend banking system built with **Java 21**, **Spring Boot**, and **Spring Cloud**, split into five independently deployable microservices. It demonstrates service discovery, centralized configuration, gateway-level JWT authentication, and inter-service communication via Feign clients — the same patterns used in real fintech backend systems.

---

## Architecture

```
                                   Client
                                     │
                                     ▼
                          ┌────────────────────┐
                          │    API Gateway      │  :8080
                          │  (JWT auth filter)  │
                          └──────────┬───────────┘
                    ┌────────────────┼────────────────┐
                    ▼                ▼                ▼
            ┌───────────────┐┌────────────────┐┌─────────────────────┐
            │  User Service  ││ Account Service ││ Transaction Service │
            │     :8081      ││     :8082       ││        :8083        │
            └───────┬────────┘└────────┬────────┘└──────────┬──────────┘
                    └── Feign ─────────▶└──── Feign ─────────▶
                                     │
                        (each service registers &
                          pulls config on startup)
                    ┌────────────────┴────────────────┐
                    ▼                                  ▼
            ┌─────────────────┐                ┌──────────────────┐
            │  Eureka Server   │                │  Config Server    │
            │  registry :8761  │                │  git config :8888 │
            └─────────────────┘                └──────────────────┘
```
 

Every business service (user, account, transaction) registers with **Eureka** for discovery, pulls its datasource and secrets from the **Config Server**, and is only reachable by clients through the **API Gateway**, which validates JWTs before forwarding requests.

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| **eureka-server** | 8761 | Service registry — all other services register here and discover each other by name instead of hardcoded URLs |
| **config-server** | 8888 | Serves each service's configuration (datasource, JWT secret) from a separate Git-backed config repo, so no service ships its own secrets |
| **api-gateway** | 8080 | Single entry point for clients. Routes `/users/**`, `/accounts/**`, `/transactions/**` to the right service via Eureka, and enforces JWT auth on all routes except `/users/register` and `/users/login` |
| **user-service** | 8081 | User registration, login (issues JWT), CRUD on users, and fetches a user's accounts via a Feign call to account-service |
| **account-service** | 8082 | Account creation, balance lookups, deposits, withdrawals, and transfers (a transfer is a withdraw + deposit wrapped in a `@Transactional` method) |
| **transaction-service** | 8083 | Persists a transaction record for every deposit/withdrawal (written to by account-service via Feign) and exposes transaction history per account |

---

## How authentication works

1. `POST /users/register` and `POST /users/login` are the only public routes (defined in the gateway's `RouteValidator`).
2. On login, `user-service` verifies the password with **BCrypt** and issues a **JWT** (HS256, 30-minute expiry) signed with a shared secret pulled from the config server.
3. Every other request must carry `Authorization: Bearer <token>`. The gateway's `AuthenticationFilter` intercepts the request, checks the header is present, and validates the token's signature before routing it downstream.
4. Because validation happens at the gateway, the individual services don't need to re-implement auth logic.

---

## How a money transfer flows across services

```
Client → API Gateway → Account Service
                             │
                             ├─ withdraw(fromAccount)  → writes to accounts table
                             │        └─ Feign call → Transaction Service → logs WITHDRAWAL
                             │
                             └─ deposit(toAccount)      → writes to accounts table
                                      └─ Feign call → Transaction Service → logs DEPOSIT
```

`AccountService.transfer()` is annotated `@Transactional`, so a failure partway through (e.g. insufficient funds) rolls back the whole operation rather than leaving one account debited without the other being credited.

---

## Tech Stack

- **Language / Framework:** Java 21, Spring Boot 3
- **Microservices:** Spring Cloud Gateway, Netflix Eureka, Spring Cloud Config
- **Service-to-service calls:** OpenFeign
- **Security:** Spring Security, JWT (`jjwt`), BCrypt password hashing
- **Persistence:** Spring Data JPA + PostgreSQL (one database per service — `user_db`, `account_db`, `transaction_db`)
- **Validation:** Jakarta Bean Validation
- **Build:** Maven

---

## Project Structure

```
Banking_Application/
└── Backend/
    ├── banking-config-repo/          # externalized config, served by config-server
    │   ├── api-gateway.yaml
    │   ├── user-service.yaml
    │   ├── account-service.yaml
    │   └── transaction-service.yaml
    └── Microservices/
        ├── eureka-server/
        ├── config-server/
        ├── api-gateway/
        │   └── filter/                # AuthenticationFilter, RouteValidator, JwtUtil
        ├── user-service/
        │   ├── controller/ service/ repository/ entity/ dto/ client/
        │   └── config/                # SecurityConfig, JwtService
        ├── account-service/
        │   ├── controller/ service/ repository/ entity/ dto/ client/
        └── transaction-service/
            └── controller/ service/ repository/ entity/ dto/
```

Each service follows a clean layered structure: `controller → service → repository`, with `dto/` for request/response shaping, `exception/` for centralized error handling (`@RestControllerAdvice`), and `client/` for Feign interfaces where a service needs to call another.

---

## API Reference

### User Service (via gateway: `/users/**`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/users/register` | Public | Register a new user |
| POST | `/users/login` | Public | Authenticate, returns JWT + user info |
| GET | `/users` | JWT | List all users |
| GET | `/users/{userId}` | JWT | Get a user by ID |
| PUT | `/users/{userId}` | JWT | Update a user |
| DELETE | `/users/{userId}` | JWT | Delete a user |
| GET | `/users/{userId}/accounts` | JWT | Get all accounts for a user |

### Account Service (via gateway: `/accounts/**`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/accounts/create/{userId}` | Open a new account for a user |
| GET | `/accounts/{userId}` | List accounts for a user |
| PUT | `/accounts/deposit/{accountId}?amount=` | Deposit funds |
| PUT | `/accounts/withdraw/{accountId}?amount=` | Withdraw funds |
| PUT | `/accounts/transfer/from/{fromAccountId}/to/{toAccountId}?amount=` | Transfer between accounts |

### Transaction Service (via gateway: `/transactions/**`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/transactions` | Record a transaction (called internally by account-service) |
| GET | `/transactions/account/{accountId}` | Get transaction history for an account |

All routes except `/users/register` and `/users/login` require a valid `Authorization: Bearer <token>` header.

---

## Running Locally

### Prerequisites
- Java 21
- Maven
- PostgreSQL running locally, with `user_db`, `account_db`, and `transaction_db` created for user `bankuser`
- Update `Backend/Microservices/config-server/src/main/resources/application.yaml` to point `spring.cloud.config.server.git.uri` at your local path to `banking-config-repo`

### Start order
Services depend on Eureka and the Config Server being up first:

1. `eureka-server` — `mvn spring-boot:run` (port 8761)
2. `config-server` — `mvn spring-boot:run` (port 8888)
3. `user-service`, `account-service`, `transaction-service` — any order (8081 / 8082 / 8083)
4. `api-gateway` — `mvn spring-boot:run` (port 8080)

Once everything is registered, check `http://localhost:8761` to confirm all five services show as `UP`, then hit the API through the gateway at `http://localhost:8080`.

---

## Design Decisions Worth Discussing in an Interview

- **Database-per-service:** each service owns its own PostgreSQL schema, so no service reaches into another's tables — communication only happens through Feign-called REST APIs.
- **Gateway-level auth:** JWT validation is centralized in the gateway rather than duplicated in every service, keeping the business services focused on domain logic.
- **Externalized config:** datasource URLs and secrets live in a separate Git repo served by Spring Cloud Config, not hardcoded in each service — makes environment promotion (dev → staging → prod) straightforward.
- **Transactional transfer:** `transfer()` composes `withdraw()` and `deposit()` inside a single `@Transactional` boundary so a mid-flight failure doesn't leave the books unbalanced.

---

## 🚀 Future Improvements

The project is fully functional for demonstrating a microservices-based banking backend. The following enhancements are planned to make it production-ready:

* **Refresh Token Authentication** – Implement refresh tokens so users can obtain new JWT access tokens without logging in again after expiration.

* **Role-Based Authorization (RBAC)** – Enforce role-based access control using Spring Security to restrict endpoints based on user roles (e.g., CUSTOMER, ADMIN).

* **Circuit Breaker & Fault Tolerance** – Integrate Resilience4j with Feign Clients to provide graceful degradation and improve resilience during service failures.

* **Containerization** – Dockerize all microservices and provide a Docker Compose configuration for one-command local deployment, including PostgreSQL and supporting services.

* **Comprehensive Testing** – Increase test coverage with unit tests, integration tests, and API tests using JUnit 5, Mockito, and Testcontainers.

* **Financial Precision** – Replace `Double` with `BigDecimal` for monetary calculations to eliminate floating-point precision issues and comply with financial application best practices.

* **Monitoring & Observability** – Add Prometheus, Grafana, centralized logging, and distributed tracing for production-grade monitoring.

* **CI/CD Pipeline** – Automate build, testing, and deployment using GitHub Actions or Jenkins.

---

## Author

Om Deshmukh
