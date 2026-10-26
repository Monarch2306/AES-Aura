# AURELIA — AI Answer Evaluation (Docker Deployment)

A Docker-only deployment of the AURELIA AES-QA platform. No local runtimes (Node.js, Java, Python, PostgreSQL) are required — everything runs inside containers.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) (with Docker Compose v2)

## Quick Start

1. **Configure environment variables:**
   ```bash
   cp .env.example .env
   # Edit .env with your preferred values (DB credentials, JWT secret, etc.)
   ```

2. **Build and start all services:**
   ```bash
   docker compose up --build
   ```

3. **Access the application:**
   - **Frontend:** [http://localhost](http://localhost)
   - **Backend API:** [http://localhost:8080](http://localhost:8080)
   - **AI Service:** [http://localhost:8001](http://localhost:8001)

## Services

| Service       | Description                        | Internal Port | External Port |
|---------------|------------------------------------|---------------|---------------|
| `postgres`    | PostgreSQL 16 database             | 5432          | 5433          |
| `db-migrate`  | One-shot DB migration runner       | —             | —             |
| `ai-service`  | Python/FastAPI AI evaluation engine | 8001          | 8001          |
| `backend`     | Spring Boot REST API               | 8080          | 8080          |
| `frontend`    | React app served via Nginx         | 80            | 80            |

## Useful Commands

```bash
# Start in background
docker compose up --build -d

# View logs
docker compose logs -f

# Stop all services
docker compose down

# Stop and remove all data (volumes)
docker compose down -v
```
