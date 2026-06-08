# SnipLink - AI URL Shortener

A full-stack URL shortener with **on-device AI risk scoring**. Shorten any link and instantly see whether it's safe to share — no paid APIs, no data leaving your machine.

---

## Features

- **AI-Powered Risk Scoring** — Every URL is scored 0.00–1.00 using a local BERT cross-encoder (ONNX) or a built-in heuristic scorer. No external API calls.
- **Base62 Short Codes** — 7-character, URL-safe, collision-resistant codes generated server-side.
- **Click Tracking** — Every redirect is counted. Look up any short code to see live stats.
- **Per-IP Rate Limiting** — Token-bucket rate limiter (Bucket4j) at the servlet filter layer.
- **Idempotent Shortening** — Same URL always returns the same short code.
- **Demo Mode** — Frontend falls back to an in-memory `localStorage` store when the backend is unreachable. Fully usable without Docker.
- **One-Command Docker** — `docker compose up` spins up PostgreSQL + the Spring Boot backend. Frontend runs via Vite.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | React 19, TypeScript, Tailwind CSS 4, Vite 7 |
| Backend | Spring Boot 3.3, Java 21, Spring Data JPA |
| Database | PostgreSQL 16 (Alpine) |
| AI / ML | LangChain4j 0.35 + ONNX Runtime (local BERT cross-encoder) |
| Rate Limiting | Bucket4j 8.10 |
| Testing | JUnit 5, Testcontainers 1.20 |
| Containerization | Docker, Docker Compose |

---

## Project Structure

```
snip-link/
├── backend/                    # Spring Boot API
│   ├── src/main/java/com/ankush/shortener/
│   │   ├── controller/
│   │   │   ├── UrlController.java          # POST /api/v1/shorten
│   │   │   ├── RedirectController.java     # GET  /{code} → 302 redirect
│   │   │   └── StatsController.java        # GET  /api/v1/stats/{code}
│   │   ├── service/
│   │   │   ├── UrlShortenerService.java    # Core shortening + redirect logic
│   │   │   ├── UrlSafetyService.java       # AI risk scoring (ONNX + heuristic)
│   │   │   └── Base62Encoder.java          # Atomic long → Base62 encoder
│   │   ├── entity/Url.java                 # JPA entity
│   │   ├── repository/UrlRepository.java   # Spring Data JPA repo
│   │   ├── dto/                            # Request/Response records
│   │   ├── config/                         # Rate limit, CORS, app properties
│   │   └── exception/                      # Global exception handler
│   ├── src/test/                           # Unit + integration tests
│   ├── Dockerfile
│   └── pom.xml
├── src/                        # React frontend
│   ├── pages/
│   │   ├── Home.tsx             # URL shortening form + result card
│   │   └── Stats.tsx            # Short code lookup + click stats
│   ├── components/
│   │   ├── Layout.tsx           # Header / Footer shell
│   │   ├── ResultCard.tsx       # Displays short link + copy button
│   │   ├── StatsCard.tsx        # Displays click count + metadata
│   │   └── RiskBadge.tsx        # Safe / Caution / Danger pill
│   ├── api/
│   │   ├── client.ts            # Typed HTTP client + demo fallback
│   │   └── types.ts             # TypeScript interfaces
│   └── utils/                   # Formatters, cn() helper
├── docker-compose.yml
├── package.json
├── vite.config.ts
└── index.html
```

---

## Getting Started

### Prerequisites

- **Java 21** (for the backend)
- **Node.js 18+** (for the frontend)
- **Docker + Docker Compose** (recommended for the database)

### 1. Start the Backend (Docker)

```bash
docker compose up --build
```

This starts PostgreSQL on port `5432` and the Spring Boot app on port `8080`.

### 2. Start the Frontend

```bash
npm install
npm run dev
```

The app opens at `http://localhost:5173`.

### 3. Use It

1. Paste any URL into the input field on the **Home** tab.
2. Click **Shorten URL** — you'll get a short link and a risk score.
3. Switch to the **Stats** tab and paste the short code to see click counts.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE` | `http://localhost:8080` | Backend URL for the frontend |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/shortener` | PostgreSQL connection string |
| `SPRING_DATASOURCE_USERNAME` | `shortener` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `shortener` | DB password |
| `APP_BASE_URL` | `http://localhost:8080` | Base URL for generating short links |
| `SAFETY_ENABLED` | `true` | Enable/disable AI risk scoring |
| `SAFETY_THRESHOLD` | `0.75` | Risk score threshold for rejecting URLs |
| `SAFETY_MODEL_PATH` | _(empty)_ | Path to ONNX model file (optional) |
| `SAFETY_VOCAB_PATH` | _(empty)_ | Path to ONNX tokenizer vocab (optional) |
| `CODE_LENGTH` | `7` | Length of generated short codes |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Allowed CORS origins |
| `RL_CAPACITY` | `60` | Rate limit: token bucket capacity |
| `RL_REFILL` | `60` | Rate limit: tokens refilled per minute |

---

## API Reference

### `POST /api/v1/shorten`

Shortens a URL and returns a risk score.

**Request:**
```json
{ "url": "https://example.com/very/long/path" }
```

**Response:**
```json
{
  "code": "aB3xK9m",
  "shortUrl": "http://localhost:8080/aB3xK9m",
  "riskScore": 0.0
}
```

**Errors:**
- `422` — URL flagged as unsafe (risk score ≥ threshold)
- `429` — Rate limit exceeded

---

### `GET /{code}`

Redirects to the original URL (302) and increments the click counter.

---

### `GET /api/v1/stats/{code}`

Returns metadata and click count for a short link.

**Response:**
```json
{
  "code": "aB3xK9m",
  "longUrl": "https://example.com/very/long/path",
  "clicks": 42,
  "createdAt": "2025-06-08T12:00:00Z"
}
```

---

### `GET /actuator/health`

Spring Boot Actuator health endpoint. Used by the frontend to detect whether the backend is available.

---

## How Risk Scoring Works

`UrlSafetyService` scores every URL on a **0.00 (safe) → 1.00 (dangerous)** scale.

**Heuristic checks** (always run):
| Signal | Score penalty |
|--------|--------------|
| URL length > 200 chars | +0.15 |
| Path length > 120 chars | +0.10 |
| IP-based hostname | +0.25 |
| Suspicious TLD (`.xyz`, `.top`, `.buzz`, `.tk`, …) | +0.25 |
| Brand typosquatting (`paypa1`, `g00gle`, …) | +0.35 |
| URL shortener chain | +0.10 |
| Excessive subdomains (4+ dots) | +0.15 |
| Credential query strings (`password=`, `login=`) | +0.20 |

**ONNX model** (optional):
- If `SAFETY_MODEL_PATH` and `SAFETY_VOCAB_PATH` are set, a BERT cross-encoder runs local inference via LangChain4j + ONNX Runtime.
- The final score is `max(heuristic, modelScore)`, clamped to `[0, 1]`.
- If the model fails to load or inference errors, it silently falls back to heuristic only.

URLs scoring **≥ 0.75** (configurable) are rejected with HTTP 422.

---

## Demo Mode

If the backend is unreachable, the frontend automatically switches to **Demo Mode**:

- Short URLs are generated client-side with random codes.
- Risk scoring uses a simplified heuristic.
- Click counts and URL data are stored in `localStorage`.
- Short links point to `/?code=…` so you can paste them into the Stats tab.

No backend, no Docker, no database required — just open the Vite dev server.

---

## Testing

```bash
# Backend unit + integration tests (requires Docker for Testcontainers)
cd backend
./mvnw test

# Frontend build check
npm run build
```

---

## License

MIT
