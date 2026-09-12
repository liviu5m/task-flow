# TaskFlow

TaskFlow is a production-grade, distributed durable workflow orchestration engine and platform combining a robust Java 25 / Spring Boot backend and a modern Next.js 16 / React 19 frontend. It provides reliable execution of long-running workflows, directed acyclic graphs (DAGs), event sourcing, distributed coordination, and a polished user interface.

---

## Architecture & System Overview

TaskFlow is split into two main components:
- **Backend (`/backend`)**: Java 25, Spring Boot 4.1.0, Spring Data JPA, PostgreSQL, Redis, JGraphT, Spring Security.
- **Frontend (`/frontend`)**: Next.js 16.3.1 (App Router), React 19, TypeScript, Tailwind CSS v4, TanStack React Query, Better Auth, Lucide React.

---

## Backend Features (Spring Boot Engine)

### 1. Durable Workflow Engine & DAG Orchestration
- **Graph-Based Execution**: Utilizes **JGraphT** to model workflows as Directed Acyclic Graphs (DAGs) with topological sorting.
- **Event-Sourced Architecture**: Complete state machine driven by append-only workflow events (`WorkflowEvent`, `WorkflowInstance`) ensuring deterministic replay, auditability, and durability.
- **Workflow Registry**: Dynamic registration and validation of workflow definitions and steps.

### 2. Advanced Workflow Constructs
- **Sequential & Parallel Steps**: Execute steps sequentially or in parallel branches (`BranchBuilder`).
- **Conditional & Switch-Case Routing**: Dynamic branching using predicate conditions (`CaseBuilder`, `SwitchCaseBuilder`).
- **Event-Driven Signals (`AwaitingSignal`)**: Pause workflow execution until an external signal or event is received (`PendingSignal`).
- **Sub-Workflows & Parent-Child Relationships**: Spawn and manage child workflows linked to parent execution context (`WorkflowParentChild`).
- **Timers & Delays**: Schedule delayed steps and timer-based workflow continuations (`WorkflowTimer`).

### 3. Distributed Coordination & Fault Tolerance
- **Leader Election**: Automated leader election (`LeaderElector`, `LeaderElectionScheduler`, `LeaderEpoch`) for high availability and distributed task processing.
- **Redis Workers & Task Queue**: Asynchronous task distribution and consumption via Redis (`RedisWorkerConsumer`, `RedisTaskProducer`).
- **Automatic Recovery**: Scheduled recovery mechanism (`WorkflowRecoveryScheduler`) for stale or interrupted workflow instances.
- **Retry Logic & Backoff**: Configurable step execution retries with exception handling (`RetryableStepException`, `StepExecutionFailedException`).

### 4. Security & API Management
- **Spring Security & OAuth2**: Robust security configuration supporting role-based access and authentication.
- **API Key Management**: Secure generation, validation, and revocation of API keys (`ApiKey`, `ApiKeyRepository`) for programmatic REST API access.

---

## Frontend Features (Next.js Dashboard)

### 1. Authentication & User Management
- **Better Auth Integration**: Secure session handling, email/password sign up/login, and social login (Google) integration.
- **Protected Routes & Middleware**: Route protection and session validation via Next.js middleware.

### 2. Dashboard & Workflow Management (`/dashboard`)
- **Overview & Stats**: Real-time operational metrics and summary of active/completed workflows.
- **Workflow Catalog & Inspector**: View registered workflows, trigger new workflow instances with custom JSON input payloads, and inspect step-by-step execution progress.
- **Real-Time Event Stream (`/dashboard/workflows/events`)**: Live event monitoring showing workflow state transitions, step completions, failures, and signals.

### 3. API Key Management (`/dashboard/api-keys`)
- **Key Generation & Revocation**: UI to generate scoped API keys for external integrations and securely manage credentials.

### 4. User Profile (`/dashboard/profile`)
- **Account Settings**: View and update user profile information and security preferences.

---

## Project Directory Structure

```text
task-flow/
├── backend/                  # Spring Boot backend application
│   ├── src/main/java/com/task_flow/backend/
│   │   ├── config/           # Security & application configuration
│   │   ├── engine/           # Durable workflow engine, DAG builders, leader election, workers
│   │   ├── enums/            # Workflow states & event types
│   │   ├── exception/        # Custom step & execution exceptions
│   │   ├── model/            # JPA entities (WorkflowInstance, WorkflowEvent, Timer, ApiKey, etc.)
│   │   ├── repository/       # Spring Data JPA repositories
│   │   └── service/          # Business logic & Redis producers
│   └── pom.xml               # Maven configuration (Java 25, Spring Boot 4.1.0)
└── frontend/                 # Next.js frontend application
    ├── app/                  # Next.js App Router (Dashboard, Auth, Workflows, API Keys)
    ├── components/           # UI components (Shadcn UI primitives, Header, Loader)
    ├── lib/                  # API clients, Better Auth client, utilities, TypeScript types
    └── package.json          # Node dependencies (Next.js 16, React 19, TanStack Query, Better Auth)
```

---

## Getting Started

### Prerequisites
- **Java 25** & Maven
- **Node.js 20+** & npm
- **PostgreSQL** Database
- **Redis** Server

### Running the Backend
```cd backend
./mvnw spring-boot:run
```

### Running the Frontend
```cd frontend
npm install
npm run dev
```
