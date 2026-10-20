# Legacy Order Service

Spring Boot 3.2 / Java 17 order API. This README is a **refactoring report**: what was wrong in the original codebase, what changed, and how to run the service now.

## Stack

- Java 17
- Spring Boot 3.2.0
- Spring Web, Data JPA, Bean Validation
- H2 (in-memory)
- JUnit 5, Mockito, MockMvc

## Original problems

The first version was a single “God Controller” with public entity fields, in-memory filtering, and no tests.

| Area | Issue |
| --- | --- |
| Architecture | Controller talked to the repository, calculated totals, and searched lists in Java |
| Naming | Short, unclear names (`x`, `d`, `tmp`, `a`, `b`) |
| Money | `double` for `price` and `total` (rounding errors) |
| Duplication | `price * quantity` copied in constructor, create, update, and `/total` |
| Persistence | `GET /search` and `GET /expensive` loaded **all** rows with `findAll()` |
| Pagination | Full order list returned in one response |
| Validation | No Jakarta Bean Validation; invalid payloads were accepted |
| Errors | `Optional.get()` caused HTTP 500; deleting a missing id succeeded silently |
| Config | Datasource and port hardcoded in `application.properties` only |
| Tests | None |

## What changed

### Layered architecture

Responsibility is split so HTTP, business rules, and SQL stay apart:

```
controller  →  HTTP, routing, @Valid
service     →  use cases, 404 rules, mapping to DTOs
repository  →  JPA / JPQL queries
entity      →  persistence model + single total calculation
dto         →  request/response contracts
exception   →  OrderNotFoundException + @RestControllerAdvice
config      →  .env loader
```

The API no longer returns JPA entities. Clients use `OrderRequest` / `OrderResponse`.

### Java naming conventions

Public fields on `Order` were replaced with private fields and getters/setters. Controller and service parameters use names such as `orderService`, `request`, `pageable`, `minPrice`.

### Money as `BigDecimal`

`price` and `total` are `numeric(19,2)`. Totals are calculated once on the entity (`@PrePersist` / `@PreUpdate`) with scale 2 and `RoundingMode.HALF_UP`. Create, update, and `GET /{id}/total` share that logic.

### Filtering and search in SQL

- Search: JPQL `LIKE` on customer and product names (case-insensitive), parameterized to avoid injection.
- Expensive orders: `findByPriceGreaterThan` in the database.

Java `for` loops over `findAll()` were removed.

### Pagination

`GET /api/orders`, `/search`, and `/expensive` accept Spring Data `page`, `size`, and `sort`. Defaults: page 0, size 20, sort `id,desc`. Max page size is 100.

The list response is a Spring `Page` JSON object (`content`, `totalElements`, `number`, `size`) instead of a raw array. Routes are unchanged.

### Validation

`POST` and `PUT` bodies use Jakarta Bean Validation:

- `customerName` / `productName` — `@NotBlank`
- `price` — `@NotNull`, `@DecimalMin("0.00")`, `@Digits` (max 2 fraction digits)
- `quantity` — `@NotNull`, `@Min(1)`

Query params `q` and `price` are validated on the controller.

### Error handling

| Situation | HTTP |
| --- | --- |
| Order id not found (get / put / delete / total) | **404** |
| Validation or malformed JSON | **400** |

`GlobalExceptionHandler` returns a consistent body: `timestamp`, `status`, `error`, `message`, `path`, `details`.

### Configuration (`.env`)

`.env` is loaded by `DotenvEnvironmentPostProcessor` **before** the Spring context starts. `application.properties` reads placeholders such as `SERVER_PORT` and `SPRING_DATASOURCE_URL`. If `.env` is missing, H2 defaults still work.

`.env.example` documents the keys. `.env` is gitignored.

### Tests

| Class | Type | Covers |
| --- | --- | --- |
| `OrderServiceTest` | Unit | Totals, 404, repository delegation (no in-memory scan) |
| `OrderTest` | Unit | `BigDecimal` rounding |
| `OrderApiIntegrationTest` | API | CRUD, validation 400, missing order 404, pagination, search, expensive filter |

## Preserved API routes

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/orders` | Create; total calculated on the server |
| `GET` | `/api/orders` | Paginated list |
| `GET` | `/api/orders/search?q=` | SQL search, paginated |
| `GET` | `/api/orders/expensive?price=` | Price greater than threshold (default `100`) |
| `GET` | `/api/orders/{id}/total` | `{ "orderId", "total" }` |
| `PUT` | `/api/orders/{id}` | Full update |
| `DELETE` | `/api/orders/{id}` | `{ "message": "deleted" }` |

Added (does not break existing clients):

| Method | Path |
| --- | --- |
| `GET` | `/api/orders/{id}` |

## Run

Java 17 is required. From the project root (Maven on `PATH`, or the Maven bundled with IntelliJ):

```bash
mvn test
mvn spring-boot:run
```

Copy `.env.example` to `.env` and adjust values if needed.

- API: `http://localhost:8080/api/orders`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:legacydb`, user `sa`)

### Example

```http
POST /api/orders
Content-Type: application/json

{
  "customerName": "Alice",
  "productName": "Keyboard",
  "price": 25.50,
  "quantity": 2
}
```

Response includes `"total": 51.00`.

```http
GET /api/orders?page=0&size=20
GET /api/orders/search?q=key
GET /api/orders/expensive?price=100
```

## Project layout

```
src/main/java/kg/example/legacy/
  LegacyOrderServiceApplication.java
  config/DotenvEnvironmentPostProcessor.java
  controller/OrderController.java
  dto/
  entity/Order.java
  exception/
  repository/OrderRepository.java
  service/OrderService.java
src/test/java/kg/example/legacy/
  controller/OrderApiIntegrationTest.java
  entity/OrderTest.java
  service/OrderServiceTest.java
```
