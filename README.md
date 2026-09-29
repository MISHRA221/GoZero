# Go Zero Brand Intelligence Platform

A microservices prototype that turns public marketplace data for **Go Zero** (Indian D2C low-calorie ice cream brand, sold on
Blinkit, Zepto, Instamart, BigBasket and [letsgozero.in](https://letsgozero.in)) into three views a brand team would care about:
catalogue health, what customers complain about, and whether pricing and stock match across quick-commerce channels.

> **This is a portfolio project, not a production system.** It is independent and not affiliated with or endorsed by Go Zero.
> It aims for production-style architecture (service boundaries, database-per-service, containers, Kubernetes) with a
> deliberately small scope. **Every response and every screen says which data is live, seed or simulated.**

---

## Architecture

```
                                   +---------------------------------------+
   Browser  ---- HTTP :8080 ---->  |  dashboard-service  (gateway + UI)    |
                                   |  Thymeleaf + Chart.js, RestTemplate   |
                                   +----+----------------+-------------+---+
                                        | REST           | REST        | REST
                                        v                v             v
     +---------------------------+  +---------------------------+  +------------------------------+
     | ingestion-service  :8081  |  | sentiment-service  :8082  |  | reconciliation-service :8083 |
     | Jsoup scraper (robots.txt |  | lexicon scorer (~200      |  | platform simulator +         |
     | aware, 1.5s delay, cap 25)|  | terms, negation) +        |  | alert engine (price / stock  |
     | + flagged seed fallback   |  | complaint categoriser     |  | / assortment rules)          |
     +-------------+-------------+  +------+-------------+------+  +------+----------------+------+
                   |          ^            |             |                |                |
                   |          +---- GET /api/products, /api/products/{id}/reviews ---------+
                   v                       v                              v
             [ ingestion_db ]       [ sentiment_db ]              [ reconciliation_db ]
             products, reviews      review_sentiment              platform_listings, alerts
                   \_____________________ PostgreSQL 16 (one server, one DB + one login per service) ____/
                                          (embedded H2 in PostgreSQL mode when run without Docker)
```

| Service | Port | Owns | Key endpoints |
|---|---|---|---|
| ingestion-service | 8081 | `products`, `reviews` | `GET /api/products`, `GET /api/products/{id}/reviews`, `POST /api/ingest/refresh`, `GET /api/ingest/status` |
| sentiment-service | 8082 | `review_sentiment` | `GET /api/sentiment/by-flavor`, `GET /api/sentiment/complaints?category=X&flavor=Y`, `POST /api/sentiment/recompute` |
| reconciliation-service | 8083 | `platform_listings`, `alerts` | `GET /api/reconciliation/alerts?severity=&type=`, `GET /api/reconciliation/by-sku/{id}`, `GET /api/reconciliation/listings`, `POST /api/reconciliation/run` |
| dashboard-service | 8080 | (no DB) | `GET /` (UI), `GET /api/dashboard/summary`, `POST /api/dashboard/refresh` |

Swagger UI is at `http://localhost:<port>/swagger-ui.html` on every service. Health/readiness probes are at
`/actuator/health/{liveness,readiness}`.

**Design notes**

- **Database-per-service.** Each service has its own database *and* its own Postgres login; no service can read another's
  tables. Data crosses boundaries only through REST.
- **Startup order doesn't matter.** ingestion-service loads data in an `ApplicationRunner`, so its readiness probe only turns
  green once the catalogue exists. sentiment- and reconciliation-service compute lazily on first read and expose explicit
  recompute endpoints, so they tolerate the upstream being briefly unavailable (they return `503` with a Problem Details body).
- **Graceful degradation.** If a backend is down, the dashboard still renders and shows which service is unreachable.
- **Deterministic simulation.** The reconciliation simulator is seeded, so the same catalogue always yields the same alerts
  (reproducible demos and unit tests).

---

## What is real, what is seed, what is simulated (and why)

| Data | Source | Label in API / UI |
|---|---|---|
| Product catalogue, BigBasket price/MRP, rating, rating count | **Live Jsoup scrape** of `bigbasket.com/pb/go-zero/` when allowed and reachable | `isLiveData: true`, `dataSource: BIGBASKET_LIVE_SCRAPE`, green **LIVE SCRAPE** badge |
| Same, when scraping is blocked | Bundled **seed dataset** of 30 illustrative SKUs (`ingestion-service/src/main/resources/seed/`) | `isLiveData: false`, `dataSource: SEED_DATASET`, amber **SEED DATA** badge |
| Review text | Scraped from product-page JSON-LD if present, otherwise seed reviews. Each review has its own flag | per-review `isLiveData` |
| Sentiment and complaint categories | **Computed in-house** by sentiment-service from the reviews above | inherits review provenance; blue **COMPUTED** badge |
| Blinkit / Zepto / Instamart price and stock | **Simulated** by reconciliation-service | `dataSource: "SIMULATED"` on every response, red **SIMULATED** badges and ribbon |

**Why?**

- **BigBasket** has public product pages, so the scraper tries them politely. It checks `robots.txt` first (a minimal
  longest-match parser, unit-tested) and stops if the path is disallowed or robots.txt can't be read. It sends a standard
  browser User-Agent, waits 1.5 s between requests and fetches at most 25 product pages. BigBasket renders a lot of its
  content client-side and uses bot protection, so the scrape can fail or return nothing. When that happens the service
  **falls back to the seed dataset and says so**: it records the reason in `fallbackReason` (see `GET /api/ingest/status`)
  and shows it in the dashboard banner.
- **Blinkit, Zepto and Instamart** have no public catalogue pages that can reasonably be scraped in a weekend, and real
  per-SKU price or stock data needs seller-portal or API access. So reconciliation-service **simulates** those platforms
  around the BigBasket reference price:
  - normal price variation within ±5 %
  - **~15 % of SKUs**: one platform priced 11–15 % away from BigBasket, which breaches the 10 % threshold (never above MRP,
    since selling above MRP is illegal in India)
  - **~10 % of SKUs**: out of stock on 1–2 platforms
  - **~7 % of SKUs**: not listed on one platform (assortment gap)

  These anomalies are **deliberately injected** to exercise the alert rules. They are not findings about Go Zero.
- **Seed values** (prices, ratings, reviews) are hand-written and illustrative. They are **not** Go Zero's real figures
  or real customer reviews.

---

## Run it locally with no Docker and no PostgreSQL (Windows `cmd`)

The default JDBC URL points to **embedded H2 in PostgreSQL mode**. H2 ships inside the jar as a Maven dependency, so there
is nothing to install beyond a JDK 17 and Maven.

```bat
cd %USERPROFILE%\GoZero
java -version
mvn -v
mvn clean install
scripts\run-local.cmd
```

Then open <http://localhost:8080>. You can also start each service by hand in its own `cmd` window, in this order:

```bat
java -jar ingestion-service\target\ingestion-service.jar
java -jar sentiment-service\target\sentiment-service.jar
java -jar reconciliation-service\target\reconciliation-service.jar
java -jar dashboard-service\target\dashboard-service.jar
```

Useful switches, set in `cmd` before `java -jar`:

```bat
REM skip the live scrape and use the seed dataset immediately
set SCRAPER_ENABLED=false
REM route the scraper through a corporate proxy
set JAVA_TOOL_OPTIONS=-Dhttps.proxyHost=proxy.corp -Dhttps.proxyPort=8080
```

The H2 console runs on each data service at `/h2-console` (JDBC URL `jdbc:h2:mem:ingestion_db`, user `sa`, no password).

Quick checks with `curl`, which is built into Windows 10+:

```bat
curl http://localhost:8081/api/ingest/status
curl http://localhost:8082/api/sentiment/by-flavor
curl "http://localhost:8082/api/sentiment/complaints?category=MELTING_TEXTURE"
curl http://localhost:8083/api/reconciliation/alerts
curl -X POST http://localhost:8080/api/dashboard/refresh
```

---

## If Docker cannot be installed

Docker is optional for this project. The application is already runnable without any container runtime:

```bat
cd /d %USERPROFILE%\GoZero
mvn clean install
set SCRAPER_ENABLED=false
scripts\run-local.cmd
```

That path runs all four Spring Boot services as Java 17 processes and uses embedded H2 instead of PostgreSQL. It is the
appropriate path on a locked corporate laptop.

The Dockerfiles and `docker-compose.yml` are intentionally kept because they are standard OCI/container deployment
specifications. They can be used unchanged on an approved machine or cloud workspace.

### Compatible alternatives

If one of these is already approved and installed, the existing Docker specifications can usually be reused:

- **Podman Desktop / Podman Engine:** use `podman compose up --build -d` and `podman compose down`.
- **Rancher Desktop:** select the `containerd` or Docker-compatible runtime, then use the existing Compose commands.
- **Minikube or kind:** use the existing `k8s/` manifests, but the container images must first be built inside that runtime.
- **GitHub Codespaces, Gitpod or another approved Linux VM:** run the existing Docker Compose workflow there and expose port `8080`.

Installing Podman, Rancher Desktop, Minikube or kind still requires administrator approval, so none of these should be
assumed to work on the current laptop. Do not replace the Dockerfiles with a proprietary format: retaining OCI-compatible
specifications keeps the project portable and demonstrates the intended deployment architecture.

For a cloud-only Docker demonstration:

```text
1. Open the repository in an approved Codespace or Linux VM.
2. Run: docker compose up --build -d
3. Run: scripts/docker-smoke-test.cmd on Windows, or the equivalent curl checks on Linux.
4. Open the forwarded port 8080.
```

---

## Run with Docker Compose (PostgreSQL + all 4 services)

From `cmd`, optionally create a local environment file from the committed template:

```bat
copy .env.example .env
```

The checked-in values are demo credentials only. The `.env` file is ignored by Git.

```bash
docker compose up --build -d
```

This brings up:

- one `postgres:16-alpine` container. `docker/postgres/init-databases.sh` creates `ingestion_db`, `sentiment_db` and
  `reconciliation_db`, each with its own owner login.
- the 4 services on a shared bridge network. Services call each other by service name, e.g. `http://ingestion-service:8081`.
  Health-checked `depends_on` enforces the order postgres → ingestion → sentiment/reconciliation → dashboard.

Each service image is a **multi-stage build**: a Maven build stage (with a dependency-cache layer), then an
`eclipse-temurin:17-jre-alpine` runtime stage that runs as non-root UID 10001 with a readiness `HEALTHCHECK`.

Run the repeatable Windows smoke test:

```bat
scripts\docker-smoke-test.cmd
```

Dashboard: <http://localhost:8080>. Stop with `docker compose down`. Add `-v` to also delete the Postgres volume.
For troubleshooting, use `docker compose logs --tail=150` or `docker compose logs -f <service-name>`.

---

## Run on Kubernetes (minikube)

`k8s/` contains:

| File | Resources |
|---|---|
| `00-namespace.yaml` | Namespace `go-zero` |
| `10-postgres.yaml` | Secret (DB passwords), ConfigMap (init script), headless Service, **StatefulSet** with a 1Gi **PVC** (`volumeClaimTemplates`) |
| `20-ingestion-service.yaml` | ConfigMap, Secret, Deployment, Service |
| `30-sentiment-service.yaml` | ConfigMap, Secret, Deployment, Service |
| `40-reconciliation-service.yaml` | ConfigMap, Secret, Deployment, Service |
| `50-dashboard-service.yaml` | ConfigMap, Deployment, Service (NodePort 30080). No Secret, because the dashboard has no database |

Every Deployment has:

- an init container that waits for Postgres
- startup, readiness and liveness probes wired to Spring Boot's probe endpoints
- small resource requests and limits: 100m CPU / 256Mi memory requested, 500m / 512Mi limit
- `runAsNonRoot`

```bash
minikube start --cpus=4 --memory=6g

# Build the images, then load them into minikube (the manifests use imagePullPolicy: IfNotPresent)
docker compose build
minikube image load gozero/ingestion-service:1.0.0
minikube image load gozero/sentiment-service:1.0.0
minikube image load gozero/reconciliation-service:1.0.0
minikube image load gozero/dashboard-service:1.0.0

kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/
kubectl get pods -n go-zero -w          # wait until all pods are Running and READY 1/1

kubectl port-forward -n go-zero svc/dashboard-service 8080:8080
# -> http://localhost:8080
```

Optional:

```bash
kubectl port-forward -n go-zero svc/ingestion-service 8081:8081    # Swagger for a backend
minikube service dashboard-service -n go-zero                      # open via NodePort instead
kubectl delete namespace go-zero                                   # tear everything down (incl. PVC)
```

On **kind**, replace `minikube image load` with `kind load docker-image <image>`. Nothing else changes.

---

## Tests

`mvn clean install` runs the unit tests:

- robots.txt policy parsing: wildcard group only, longest match wins, `*` wildcards
- product-title parsing into flavor and pack size
- sentiment scoring: phrases, negation, clause boundaries. The lexicon must have at least 200 entries.
- complaint categorisation and category parameter parsing
- the simulator's anomaly rates, its MRP ceiling and its determinism, plus the alert engine

---

## What I'd add with real API access

- **Real channel data.** With Blinkit / Zepto / Instamart seller-API (or reporting-portal) access, I'd replace the
  `PlatformSimulator` with one adapter per platform behind the same interface. The alert engine, schema and dashboard
  would stay unchanged, and `dataSource` would switch from `SIMULATED` to the real platform per row.
- **Event-driven updates instead of synchronous REST.** Kafka topics such as `catalogue.updated`, `reviews.ingested` and
  `listing.snapshot`. sentiment- and reconciliation-service would consume them and recompute incrementally, instead of the
  dashboard triggering a synchronous refresh chain. The outbox pattern in ingestion-service would give reliable
  publishing.
- **Scheduled ingestion.** A CronJob or Spring `@Scheduled` job to scrape and poll APIs, plus a time-series of prices so
  alerts show trends like "price gap opened 3 days ago".
- **Better NLP.** Swap the lexicon for a fine-tuned multilingual model that handles Hinglish reviews, served behind the
  same `SentimentScorer` interface. Keep the lexicon as an explainable baseline.
- **Production hardening.**
  - Flyway migrations instead of `ddl-auto=update`
  - Resilience4j circuit breakers and retries on inter-service calls
  - Spring Cloud Gateway / Ingress with auth
  - OpenTelemetry tracing and Prometheus metrics
  - HPA
  - External Secrets / Sealed Secrets instead of plain K8s Secrets
  - a managed Postgres
  - CI (GitHub Actions) that builds, tests, scans and pushes images

---

## Project layout

```
.
├── pom.xml                       # parent POM (Spring Boot 3.3, Java 17), 4 modules
├── ingestion-service/            # Jsoup scraper, robots.txt policy, seed fallback, products/reviews API
├── sentiment-service/            # lexicon scorer + complaint categoriser (resources/lexicon/*.txt)
├── reconciliation-service/       # platform simulator + alert engine
├── dashboard-service/            # gateway + Thymeleaf/Chart.js UI (Chart.js served from a WebJar)
├── docker/postgres/              # DB-per-service init script
├── docker-compose.yml
├── k8s/                          # namespace, postgres StatefulSet, per-service ConfigMap/Secret/Deployment/Service
└── scripts/run-local.cmd         # start all 4 services locally on H2 (Windows cmd)
```
