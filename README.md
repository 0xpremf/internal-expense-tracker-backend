# Internal Expense Tracker Backend

## Overview

This project is a Spring Boot backend for managing internal employee expense submissions, approvals, budgeting, and fraud-risk analysis. It supports role-based access for employees, managers, and finance users, and exposes a REST API for creating, reviewing, and analyzing expenses.

The service is designed to support a typical internal expense workflow:

1. An employee creates an expense.
2. The expense can be submitted for review.
3. A manager or finance user approves or rejects it.
4. The system evaluates risk signals and warns on suspicious patterns.
5. Department budgets can be monitored and updated.

---

## Key Features

- User registration and login with JWT authentication
- Role-based authorization for employees, managers, and finance users
- Expense lifecycle management: create, update, delete, submit, approve, reject, reopen
- Department-based budget tracking and monthly budget checks
- Risk analysis for suspicious or duplicate expense patterns
- Pagination and filtering for expense listing
- OpenAPI/Swagger documentation
- Integration tests covering workflow and concurrency behavior

---

## Technology Stack

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- JWT (jjwt)
- Lombok
- Springdoc OpenAPI / Swagger UI
- Maven

---

## Project Structure

```text
src/main/java/com/project/tracker/internal_expsense_tracker_backend/
├── config/              # Security, initialization, app configuration
├── controller/          # REST controllers
├── domain/              # JPA entities and enums
├── dto/                 # Request/response DTOs
├── exceptions/          # Custom exceptions
├── Repo/                # Repositories
├── security/            # JWT filter and utilities
├── service/             # Business logic services
└── InternalExpsenseTrackerBackendApplication.java
```

---

## Main Modules

### Authentication and users
The authentication flow is handled through the auth controller and auth service. Users can register, log in, and retrieve their own profile. Authenticated requests require a JWT token.

### Expense management
Expenses can be created, listed, fetched, updated, deleted, submitted, approved, rejected, or reopened. The workflow is enforced by the expense workflow service and protected by role-based rules.

### Department budgets
Departments can have monthly budgets. Approvals are checked against the department’s budget to generate warnings when spending exceeds the cap.

### Risk analysis
When an expense is submitted or analyzed, the system runs heuristic checks for:

- duplicate submissions
- structuring behavior
- statistical outliers
- suspicious timing
- round-number bias

Each result updates the expense’s risk score, risk level, and reasons.

---

## Roles

The application uses three roles:

- EMPLOYEE: creates and manages personal expenses
- MANAGER: reviews expenses in their department
- FINANCE: reviews and approves expenses with broader oversight

---

## Getting Started

### Prerequisites

- Java 25
- Maven
- PostgreSQL server

### 1. Clone the repository

```bash
git clone <repository-url>
cd internal-expsense-tracker-backend
```

### 2. Create the database

Create a PostgreSQL database named `expensedb`.

You can use the default connection settings from the configuration, or override them with environment variables.

### 3. Configure environment variables

The application uses the following defaults:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/expensedb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=user
```

You can override them as needed in your shell or IDE run configuration.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

On startup, the application will initialize sample departments, users, and expenses automatically (unless the test profile is active).

### 5. Access the API

- Base URL: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI docs: http://localhost:8080/v3/api-docs

---

## Authentication

The API uses JWT-based authentication.

### Register a user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Employee",
    "email": "newemployee@company.com",
    "password": "password123",
    "role": "EMPLOYEE",
    "departmentId": 1
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@company.com",
    "password": "password123"
  }'
```

Use the returned token in the `Authorization` header for protected endpoints:

```bash
Authorization: Bearer <token>
```

---

## API Overview

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/auth/register | Create a new user |
| POST | /api/auth/login | Authenticate a user |
| GET | /api/auth/me | Retrieve current logged-in user |

### Expenses

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/expenses | Create a new expense |
| GET | /api/expenses | List expenses with filters and pagination |
| GET | /api/expenses/{id} | Get one expense |
| PUT | /api/expenses/{id} | Update an expense |
| DELETE | /api/expenses/{id} | Delete an expense |
| POST | /api/expenses/{id}/submit | Submit an expense |
| POST | /api/expenses/{id}/approve | Approve an expense |
| POST | /api/expenses/{id}/reject | Reject an expense |
| POST | /api/expenses/{id}/reopen | Reopen a rejected expense |

### Departments

| Method | Endpoint | Purpose |
|---|---|---|
| GET | /api/departments | List departments |
| GET | /api/departments/{id}/budget | Retrieve department budget status |
| PUT | /api/departments/{id}/budget | Update monthly budget |

### Risk Analysis

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/expenses/{id}/analyze | Run risk analysis for an expense |
| GET | /api/expenses/flagged | Retrieve flagged expenses |
| GET | /api/expenses/{id}/risk | View risk breakdown |

---

## Example Expense Workflow

A typical flow looks like this:

1. Register or log in as an employee.
2. Create a draft expense.
3. Submit it for review.
4. A manager or finance user approves or rejects it.
5. If rejected, the original author can reopen it and revise it.

This workflow is implemented in the services responsible for expense lifecycle transitions and validation.

---

## Configuration Notes

The application configuration is stored in [src/main/resources/application.yml](src/main/resources/application.yml).

Important settings include:

- server port: 8080
- PostgreSQL datasource connection
- JWT secret and expiration
- Swagger/OpenAPI paths

---

## Testing

The repository includes integration and workflow tests under the test package.

Run all tests with:

```bash
./mvnw test
```

Tests cover:

- authentication flows
- expense workflow transitions
- approval and rejection rules
- concurrency behavior
- filtering and pagination logic

---

## Default Seed Data

When the application starts outside the test profile, it seeds sample data including:

- departments such as Engineering, Sales, and Finance
- users with the default password `password123`
- sample expenses in different states

This makes it easier to explore the API manually without creating data from scratch.

---

## Notes for Developers

- The application uses a stateless security model with JWTs.
- The expense workflow uses explicit state transitions and throws domain-specific exceptions for invalid actions.
- Risk scoring is heuristic-based and can be expanded with more advanced fraud-detection logic.
- The codebase is organized around controllers, services, repositories, and domain entities for clarity and maintainability.

---

## License

This project is intended for internal use and demonstration purposes unless otherwise stated by the repository owner.
