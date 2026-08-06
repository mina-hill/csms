<div align="center">

# CSMS
### Chicken Shed Management System

A full-stack web application for managing day-to-day operations of a poultry farm — from chick procurement through to sale, covering feed, medicine, payroll, expenses, and financial reporting.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](#tech-stack)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-brightgreen?logo=springboot&logoColor=white)](#tech-stack)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](#tech-stack)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-4169E1?logo=postgresql&logoColor=white)](#tech-stack)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](#license)

</div>

---

## Tech Stack

| Layer     | Technology                      |
|-----------|---------------------------------|
| Frontend  | React 19, Vite                  |
| Backend   | Spring Boot 3, Java 21          |
| Database  | PostgreSQL (hosted on Supabase) |
| Auth      | Spring Security (session-based) |
| Testing   | JUnit 5, Mockito                |

---

## Architecture

The frontend is a single-page React app that talks to a Spring Boot REST API, which in turn persists through a layered service/repository stack onto a PostgreSQL database hosted on Supabase.

```mermaid
flowchart LR
    subgraph Client["Browser"]
        SPA["React 19 SPA (Vite)"]
    end

    subgraph API["Spring Boot REST API"]
        direction TB
        SEC["Spring Security (session auth)"]
        CTRL["Controllers\nFlock / Feed / Medicine / Brada / Sales / Expense / Payroll / Supplier / Report / Audit"]
        SVC["Services / Business Logic"]
        REPO["Spring Data Repositories"]
        SEC --> CTRL --> SVC --> REPO
    end

    DB[("PostgreSQL\n(Supabase)")]

    SPA -- "HTTPS / JSON (fetch)" --> SEC
    REPO -- "JDBC" --> DB

    style Client fill:#61dafb22,stroke:#61dafb
    style API fill:#6db33f22,stroke:#6db33f
    style DB fill:#4169e122,stroke:#4169e1
```

> A live PostgreSQL instance is required. Connection details (URL, username, password) are configured in `backend/src/main/resources/application.properties` — no credentials are reproduced here or anywhere in this README.

---

## Data Model

Core entities and their relationships (simplified — the full schema also includes lookup/audit tables such as `FeedType`, `Medicine`, `Worker`, `User`, `DailyMortality`, `WeeklyWeight`, and reporting views).

```mermaid
erDiagram
    SUPPLIER ||--o{ FLOCK : "supplies chicks for"
    SUPPLIER ||--o{ FEED_PURCHASE : supplies
    SUPPLIER ||--o{ MEDICINE_PURCHASE : supplies
    SUPPLIER ||--o{ BRADA_PURCHASE : supplies

    FLOCK ||--o{ BRADA_PURCHASE : "procured via"
    FLOCK ||--o{ FLOCK_SALE : "sold via"
    FLOCK ||--o{ EXPENSE : "attributed to (optional)"

    WORKER ||--o{ SALARY_PAYMENT : "paid via"

    SUPPLIER {
        uuid supplierId PK
        string name
        enum supplierType
        boolean isActive
    }
    FLOCK {
        uuid flockId PK
        string flockCode
        string breed
        int initialQty
        int currentQty
        enum status
        uuid supplierId FK
    }
    BRADA_PURCHASE {
        uuid purchaseId PK
        uuid flockId FK
        uuid supplierId FK
        int quantity
        decimal unitCost
        decimal totalCost
    }
    FEED_PURCHASE {
        uuid purchaseId PK
        uuid feedTypeId FK
        uuid supplierId FK
        int sackCount
        decimal costPerSack
        decimal totalCost
    }
    MEDICINE_PURCHASE {
        uuid purchaseId PK
        uuid medicineId FK
        uuid supplierId FK
        int quantity
        decimal unitCost
        decimal totalCost
    }
    FLOCK_SALE {
        uuid saleId PK
        uuid flockId FK
        string buyerName
        int qtySold
        decimal pricePerKg
        decimal totalAmount
    }
    OTHER_SALE {
        uuid saleId PK
        enum category
        string description
        decimal amount
    }
    EXPENSE {
        uuid expenseId PK
        enum category
        decimal amount
        uuid flockId FK "nullable"
    }
    WORKER {
        uuid workerId PK
        string name
        string role
        decimal salaryRate
    }
    SALARY_PAYMENT {
        uuid paymentId PK
        uuid workerId FK
        int periodYear
        int periodMonth
        decimal amountPaid
    }
```

---

## Features

| Module | Description |
|--------|-------------|
| **Flock Management** | Register flocks, track status (active/closed), log daily mortality and weekly weights (`/api/flocks`) |
| **Feed Management** | Record feed purchases, daily usage per flock, and surplus sack sales; live stock counter maintained by a DB trigger (`/api/feed`, `/api/feed-types`) |
| **Medicine Management** | Track medicine inventory, purchases per supplier, and usage per flock |
| **Brada (Chick) Procurement** | Log chick purchases linked to a flock at placement (`/api/brada`) |
| **Sales** | Record flock sales (birds sold by weight/price-per-kg) and miscellaneous other sales (`/api/sales`, `/api/other-sales`) |
| **Expenses** | Categorised expense tracking, per-flock or farm-wide (`/api/expenses`) |
| **Payroll** | Manage workers and process monthly salary payments (`/api/payroll`, `/api/workers`) |
| **Supplier Directory** | Maintain supplier records for feed, medicine, and chick suppliers (`/api/suppliers`) |
| **Reports** | Profit/loss per flock, FCR (Feed Conversion Ratio), mortality report, resource consumption summary, and global P&L (`/api/reports`) |
| **Audit Log** | Immutable log of all write operations for traceability (`/api/audit`) |
| **Role-based Access** | ADMIN and WORKER roles with per-endpoint access control via Spring Security |

---

## Prerequisites

- Java 21+
- Maven (or use the included `mvnw` wrapper — no install needed)
- Node.js 18+ and npm
- A running PostgreSQL database (connection configured via `application.properties` — see note above)

---

## How to Run

### 1. Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API starts on **http://localhost:8080**.

> The database connection is configured in `src/main/resources/application.properties`. To use your own PostgreSQL instance, update `spring.datasource.url`, `username`, and `password` there.

### 2. Frontend

```powershell
cd frontend
npm install
npm run dev
```

The app opens on **http://localhost:5173** (Vite default).

> If the backend runs on a different port, update the proxy in `frontend/vite.config.js`.

---

## Running the Tests

Unit tests (Mockito-based, no database required):

```powershell
cd backend
.\mvnw.cmd test "-Dtest=FlockControllerTest,WhiteBoxTests" "-DfailIfNoTests=false"
```

All tests including integration tests (requires the database to be reachable):

```powershell
cd backend
.\mvnw.cmd test
```

---

## Project Structure

```
csms/
├── backend/          # Spring Boot REST API
│   └── src/
│       ├── main/java/com/csms/csms/
│       │   ├── controller/   # REST endpoints
│       │   ├── entity/       # JPA entities & DB views
│       │   └── repository/   # Spring Data repositories
│       └── test/             # JUnit 5 + Mockito test suites
└── frontend/         # React + Vite SPA
    └── src/
        ├── legacy/   # Core UI (HTML/JS extracted into React shell)
        └── App.jsx
```
