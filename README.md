# Order Service

## Overview

Order Service is a Spring Boot microservice responsible for managing orders and items in the Marketplace application.

It provides REST API endpoints for creating, retrieving, updating, and deleting orders and items. The service stores order and item data in PostgreSQL and communicates with User Service to retrieve user information.

## Technologies

- Java 21
- Spring Boot 3.3.5
- Spring Data JPA
- Spring Web
- PostgreSQL
- Liquibase
- MapStruct
- Lombok
- Spring Boot Actuator
- Spring Cloud Circuit Breaker with Resilience4j
- Maven
- JUnit 5
- Mockito
- Testcontainers

## Features

- Create, retrieve, update, and delete orders.
- Create, retrieve, update, and delete items.
- Retrieve orders by ID or user ID.
- Retrieve orders using pagination and filtering.
- Filter orders by creation date and status.
- Calculate order totals based on item prices and quantities.
- Merge duplicate items in order requests.
- Validate request data.
- Integrate with User Service to retrieve user information.
- Handle missing resources and unavailable services.
- Manage database schema changes with Liquibase.
- Support local and Docker environments.

## Project Structure

```text
order-module/
├── src/
│   ├── main/
│   │   ├── java/com/innowise/orderservice/
│   │   │   ├── client/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── specification/
│   │   └── resources/
│   │       ├── db/changelog/
│   │       ├── application.yaml
│   │       ├── application-local.yaml
│   │       └── application-docker.yaml
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

## Database

The service uses PostgreSQL as its primary database.

Liquibase manages database schema migrations.

Hibernate uses the `validate` strategy to verify that the database schema matches the JPA entity mappings.

Default local database connection:

```text
jdbc:postgresql://localhost:5434/orderdb
```

Docker database connection:

```text
jdbc:postgresql://order-postgres:5432/orderdb
```

## Configuration

The application supports two Spring profiles:

| Profile | Description |
|---|---|
| `local` | Runs the application locally |
| `docker` | Runs the application inside Docker |

The default profile is `local`.

### Application Ports

| Environment | Port |
|---|---:|
| Local | 8083 |
| Docker | Configured by the application/Docker environment |

### Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `USER_SERVICE_URL` | User Service base URL |
| `SERVICE_ACCOUNT_USERNAME` | Service account username |
| `SERVICE_ACCOUNT_PASSWORD` | Service account password |

The shared `.env` file is maintained in the `marketplace-infra` repository.

For Order Service, the database credentials in the shared `.env` use:

- `ORDER_DB_USERNAME`
- `ORDER_DB_PASSWORD`

When running locally, these values must be mapped to `DB_USERNAME` and `DB_PASSWORD`.

Do not commit real credentials or `.env` files to the repository.

## REST API

Base paths:

- `/api/orders`
- `/api/items`

### Orders

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/orders` | Create an order |
| GET | `/api/orders/{id}` | Retrieve an order by ID |
| GET | `/api/orders/user/{userId}` | Retrieve orders by user ID |
| GET | `/api/orders` | Retrieve paginated orders with optional filters |
| PUT | `/api/orders/{id}` | Update an order |
| DELETE | `/api/orders/{id}` | Delete an order |

The orders listing endpoint supports optional filters:

- `createdFrom` — start of the creation date range.
- `createdTo` — end of the creation date range.
- `statuses` — one or more order statuses.
- `page` — page number.
- `size` — page size.
- `sort` — sorting criteria.

Example:

```http
GET /api/orders?statuses=CREATED&page=0&size=10
```

### Items

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/items` | Create an item |
| GET | `/api/items` | Retrieve paginated items |
| PUT | `/api/items/{id}` | Update an item |
| DELETE | `/api/items/{id}` | Delete an item |

Pagination parameters:

```http
GET /api/items?page=0&size=10&sort=name,asc
```

## User Service Integration

Order Service communicates with User Service to retrieve user information when creating orders and preparing order responses.

The service uses a dedicated client and service account credentials for inter-service communication.

Resilience4j Circuit Breaker is used to improve resilience when communicating with external services.

## Running Locally

Requirements:

- Java 21
- Maven 3.9+
- PostgreSQL

1. Start PostgreSQL using the shared infrastructure configuration in `marketplace-infra`.
2. Load the environment variables from the shared `.env` file.
3. Map the Order Service database credentials to the expected variable names:

```bash
export DB_USERNAME="$ORDER_DB_USERNAME"
export DB_PASSWORD="$ORDER_DB_PASSWORD"
export DB_URL="jdbc:postgresql://localhost:5434/orderdb"
```

4. Run the application from the `order-module` directory:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The application uses the `local` profile by default and listens on port `8083`.

After startup, the health endpoint is available at:

```text
http://localhost:8083/actuator/health
```

## Running with Docker

Build the Docker image:

```bash
docker build -t order-service .
```

The shared Docker Compose configuration is maintained in the `marketplace-infra` repository.

Run the Marketplace environment using the Docker Compose configuration from that repository. The Docker profile should be activated for the Order Service container.

## Testing

Run all tests:

```bash
./mvnw clean test
```

On Windows:

```bash
mvnw.cmd clean test
```

The project uses:

- JUnit 5 for testing.
- Mockito for mocking dependencies.
- Spring Boot Test for application-level tests.
- Testcontainers for PostgreSQL integration tests.

## Monitoring

Spring Boot Actuator is included for application monitoring and health checks.

## License

This project was developed as part of the Innowise training program.