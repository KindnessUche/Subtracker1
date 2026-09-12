# Subtracker — Subscription Tracker me sef

A full-stack subscription tracker: track recurring subscriptions manually, or (optionally)
auto-detect them by scanning Gmail with Gemini.

- **Frontend:** Next.js 16 / React 19 (`./frontend`)
- **Backend:** Spring Boot 3.5 / Java 21, JWT auth, Postgres (`./backend`)
- **Database:** PostgreSQL 16

The whole stack runs with a single `docker compose up` — no local Java/Node/Postgres
install required.

## Quick start

```bash
git clone <this-repo-url>
cd <this-repo>
cp .env.example .env   # optional — sane defaults work without this
docker compose up --build
```

Then open:

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080

Sign up for an account on the frontend and start adding subscriptions. That's it —
manual subscription tracking works fully out of the box with zero configuration.

## What's running

| Service  | Container port | Host port (default) |
|----------|----------------|----------------------|
| frontend | 3000           | 3000                 |
| backend  | 8080           | 8080                 |
| db       | 5432           | 5432                 |

All ports, credentials, and secrets can be overridden via `.env` (see `.env.example`
for every available variable). Data persists in a Docker volume (`db_data`), so your
subscriptions survive `docker compose down` / restarts. Use `docker compose down -v`
to wipe the database.

## Optional: Gmail auto-import

The backend can scan a connected Gmail account and use Gemini to detect subscription
emails automatically. This is entirely optional — the app works fine without it,
you just won't see the "Connect Gmail" flow do anything useful.

To enable it, set these in `.env` before building:

```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GEMINI_API_KEY=...
```

You'll need a Google Cloud OAuth client (Gmail API scope) with an authorized redirect
URI matching `GOOGLE_REDIRECT_URI` (default `http://localhost:8080/api/gmail/callback`),
and a Gemini API key from Google AI Studio.

## Security notes for real deployments

The defaults are meant for local/dev use. Before deploying anywhere public, set your
own values in `.env` for:

- `JWT_SECRET` — signs auth tokens
- `APP_ENCRYPTION_KEY` — base64, 32 raw bytes, used to encrypt stored Gmail tokens.
  Generate one with `openssl rand -base64 32`
- `POSTGRES_PASSWORD`
- `APP_FRONTEND_URL` / `APP_CORS_ALLOWED_ORIGINS` / `NEXT_PUBLIC_API_URL` — set these
  to your real domains (not localhost)

## Running services individually (development)

**Backend** (needs a local Postgres, or point `DB_URL` at the Dockerized one):

```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev
```

Create `frontend/.env.local` with `NEXT_PUBLIC_API_URL=http://localhost:8080` when
running the frontend outside Docker.

## Project structure

```
.
├── backend/          # Spring Boot API (Java 21, Maven)
├── frontend/          # Next.js app (React 19)
├── docker-compose.yml # Orchestrates db + backend + frontend
└── .env.example       # All configurable environment variables
```

## API overview

- `POST /api/auth/signup`, `POST /api/auth/login` — JWT-based auth
- `GET/POST/PUT/DELETE /api/subscriptions` — manage subscriptions
- `GET /api/review-queue` — review auto-detected subscriptions before they're added
- `GET /api/gmail/*` — optional Gmail connect/sync flow

All non-auth endpoints require an `Authorization: Bearer <token>` header, which the
frontend handles automatically once you're logged in.
