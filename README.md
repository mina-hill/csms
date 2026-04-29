# CSMS — Chicken Shed Management System

A full-stack web application for managing day-to-day operations of a poultry farm. It tracks flocks from chick procurement through to sale, covering feed, medicines, workers, expenses, and financial reporting.

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

## Key Features

- **Flock Management** — register flocks, track status (active/closed), log daily mortality and weekly weights
- **Feed Management** — record feed purchases, daily usage per flock, and surplus sack sales; live stock counter maintained by DB trigger
- **Medicine Management** — track medicine inventory, purchases per supplier, and usage per flock
- **Brada (Chick) Procurement** — log chick purchases linked to a flock at placement
- **Sales** — record flock sales (birds) and other miscellaneous sales
- **Expenses** — categorised expense tracking per flock or farm-wide
- **Payroll** — manage workers and monthly salary payments
- **Supplier Directory** — maintain supplier records (feed, medicine, chick suppliers)
- **Reports** — profit/loss per flock, FCR (Feed Conversion Ratio) report, mortality report, resource consumption summary, and global P&L
- **Audit Log** — immutable log of all write operations for traceability
- **Role-based Access** — ADMIN and WORKER roles with per-endpoint access control

---

## Prerequisites

- Java 21+
- Maven (or use the included `mvnw` wrapper — no install needed)
- Node.js 18+ and npm
- A running PostgreSQL database (or use the Supabase connection already in `application.properties`)

---

## How to Run

### 1. Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API starts on **http://localhost:8080**.

> The database connection is pre-configured in `src/main/resources/application.properties`. If you want to use your own PostgreSQL instance, update `spring.datasource.url`, `username`, and `password` there.

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
