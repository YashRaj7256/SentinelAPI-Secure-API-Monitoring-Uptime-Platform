# SentinelAPI — Secure API Monitoring & Uptime Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-12-red.svg)](https://flywaydb.org/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

SentinelAPI is an enterprise-grade API monitoring and uptime platform built with Java 21, Spring Boot, and PostgreSQL. It allows users to register HTTP/HTTPS endpoints, monitor availability, track response latency, classify network failure types, and report uptime health statistics.

> ⚠️ **Development Note (V1)**: The current V1 release is intended for **local development and testing**. Advanced SSRF defense-in-depth, user authentication, and distributed scheduling are scheduled for subsequent phases.

---

## 📌 Project Overview & Current Scope

SentinelAPI is being developed incrementally with clean software architecture principles, production-style migrations, and strict separation of concerns.

### Current Implementation Status (Day 5 / M4 Complete)
- **Framework & Runtime**: Java 21 LTS with Spring Boot 4.1.0.
- **Database & Persistence**: PostgreSQL 17 managed via Flyway versioned migrations (`V1__initial_schema.sql`).
- **JPA & Schema Validation**: Hibernate operating with `ddl-auto: validate` and `open-in-view: false`.
- **Entity Model**: Type-safe `Monitor` entity with `MonitorStatus` (`UP`, `DOWN`, `UNKNOWN`, `PAUSED`) and restricted `HttpMethod` (`GET`, `HEAD`).
- **Data Transfer Objects (DTOs)**: Complete request/response validation decoupling persistence from API representation (`@NotBlank`, `@URL`, `@Min`, `@Max`).
- **Clean Service Architecture**: `MonitorService` implementing full CRUD and pagination (`PageResponse<T>`), explicit constructor injection, and `@Transactional(readOnly = true)`.
- **Global Error Handling**: `@RestControllerAdvice` emitting standardized `ApiErrorResponse` with field-level validation errors and security-conscious exception masks.
- **REST API**: Fully functional endpoints under `/api/v1/monitors` supporting POST, GET, PATCH, and DELETE.

---

## 🏗️ Architecture & Layering

```
HTTP Request
     │
     ▼
[ MonitorController ]         ── Handles HTTP status codes, routing & @Valid
     │
     ▼
[ MonitorService ]            ── Enforces business rules & transactional logic
  ├── [ MonitorMapper ]       ── Entity <-> DTO conversions
  └── [ MonitorRepository ]   ── Spring Data JPA database operations
            │
            ▼
     [ PostgreSQL ]           ── Relational persistence (Flyway managed)
```

---

## 🚀 Getting Started

### Prerequisites
- **Java**: OpenJDK 21 or Eclipse Temurin 21+
- **Maven**: 3.9+
- **PostgreSQL**: 17+ installed and running locally
- **Terminal**: PowerShell / Bash

---

### Database Setup

1. Start your local PostgreSQL service:
   ```powershell
   net start postgresql-x64-17
   ```
2. Create the target database:
   ```sql
   CREATE DATABASE sentinelapi;
   ```
3. Configure your local credentials in `src/main/resources/application-dev.yml` if different from the default:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/sentinelapi
       username: postgres
       password: YourPasswordHere
   ```

---

### Building and Running

1. **Build the project**:
   ```powershell
   mvn clean compile
   ```
2. **Run the application**:
   ```powershell
   mvn spring-boot:run
   ```
3. **Run tests**:
   ```powershell
   mvn test
   ```

The application will start on `http://localhost:8080`. Flyway will automatically run the baseline migration `V1__initial_schema.sql` on first launch.

---

## 📡 REST API Reference

### Base URL: `/api/v1/monitors`

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/monitors` | Create a new monitor | `201 Created` |
| `GET` | `/api/v1/monitors` | List monitors (paginated) | `200 OK` |
| `GET` | `/api/v1/monitors/{id}` | Get monitor details by ID | `200 OK` / `404 Not Found` |
| `PATCH` | `/api/v1/monitors/{id}` | Partial update monitor settings | `200 OK` / `404 Not Found` |
| `DELETE` | `/api/v1/monitors/{id}` | Delete a monitor | `204 No Content` / `404 Not Found` |

---

### Example API Requests

#### 1. Create a Monitor (`POST /api/v1/monitors`)

```bash
curl -X POST http://localhost:8080/api/v1/monitors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Payment Service Health",
    "url": "https://api.example.com/health",
    "httpMethod": "GET",
    "expectedStatus": 200,
    "checkInterval": 300,
    "timeout": 5000,
    "failureThreshold": 3
  }'
```

**Response (`201 Created`):**
```json
{
  "id": 1,
  "name": "Payment Service Health",
  "url": "https://api.example.com/health",
  "httpMethod": "GET",
  "expectedStatus": 200,
  "checkInterval": 300,
  "timeout": 5000,
  "active": true,
  "status": "UNKNOWN",
  "failureThreshold": 3,
  "consecutiveFailures": 0,
  "lastCheckedAt": null,
  "createdAt": "2026-10-12T17:50:27.859869Z",
  "updatedAt": "2026-10-12T17:50:27.859869Z"
}
```

---

#### 2. List Monitors (`GET /api/v1/monitors?page=0&size=20`)

```bash
curl -X GET "http://localhost:8080/api/v1/monitors?page=0&size=20"
```

**Response (`200 OK`):**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Payment Service Health",
      "url": "https://api.example.com/health",
      "httpMethod": "GET",
      "status": "UNKNOWN",
      "active": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

#### 3. Partial Update (`PATCH /api/v1/monitors/1`)

```bash
curl -X PATCH http://localhost:8080/api/v1/monitors/1 \
  -H "Content-Type: application/json" \
  -d '{
    "checkInterval": 600,
    "timeout": 8000
  }'
```

**Response (`200 OK`):**
```json
{
  "id": 1,
  "name": "Payment Service Health",
  "url": "https://api.example.com/health",
  "checkInterval": 600,
  "timeout": 8000,
  "updatedAt": "2026-10-12T17:51:58.246450Z"
}
```

---

#### 4. Delete Monitor (`DELETE /api/v1/monitors/1`)

```bash
curl -X DELETE http://localhost:8080/api/v1/monitors/1
```

**Response:** `204 No Content`

---

## 🛡️ Validation & Error Response Format

All API errors return a uniform schema with HTTP-appropriate statuses:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": [
    {
      "field": "url",
      "message": "URL must be a valid HTTP or HTTPS URL"
    },
    {
      "field": "name",
      "message": "Monitor name is required"
    }
  ],
  "timestamp": "2026-10-12T17:52:29.350831Z"
}
```

### Safety by Design
- **Method Restrictions**: In V1, monitors are restricted to safe idempotent methods (`GET`, `HEAD`). State-changing methods (`POST`, `PUT`, `DELETE`) are intentionally rejected to prevent accidental side effects against monitored targets.
- **SSRF Architecture**: Full network isolation and DNS pinning are part of Phase 3 hardening before public deployments.

---

## 🗺️ Roadmap & Upcoming Milestones

- [x] **Day 1**: Project Bootstrap & Flyway Baseline Migration
- [x] **Day 2**: JPA Entity (`Monitor`), Enums (`MonitorStatus`, `HttpMethod`), Repository
- [x] **Day 3**: DTOs & Bean Validation (`CreateMonitorRequest`, `PatchMonitorRequest`)
- [x] **Day 4**: Mapper, `PageResponse`, `MonitorService` CRUD Logic
- [x] **Day 5**: `GlobalExceptionHandler` & `MonitorController` (Live CRUD verification)
- [ ] **Day 6**: Unit Testing `MonitorService` with JUnit 5 & Mockito
- [ ] **Day 7–9**: Monitoring Execution Engine with Spring `WebClient`
- [ ] **Day 10**: Polling Scheduler & Status Transitions
- [ ] **Day 11–12**: Check Result History & Uptime Percentage Aggregations
- [ ] **Day 13**: Integration Tests with Testcontainers
- [ ] **Day 14**: Swagger / OpenAPI 3 Documentation
