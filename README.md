# Leuven Go

> **Cleanliness, gamified.** A 24-hour hackathon prototype that turns a single
> photo + location from a citizen into an actionable Planon work order, while
> letting students compete for their faculty's honour.

Leuven Go bridges what residents *see* on the street with what city teams
*do* about it. It ships two dashboards on the same dataset:

| Dashboard            | For                | Vibe                                                                                                                   |
| -------------------- | ------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| **Professional**     | City planners      | Dark, technical. Hotspot heatmap, AI cleanliness scores per street segment, Planon work-order monitor, crew routing.   |
| **Student**          | KU Leuven students | Warm, gamified. Faculty-coloured territories, 0–5 scoring, clan leaderboard, rewards marketplace.                      |

---

## Stack

- **Frontend** — React 18 + TypeScript + Vite, React Router, React-Leaflet for maps
- **Backend** — Java 17 + Spring Boot 3.3 (Web, Data JPA, Validation), in-memory H2
- **Maps** — CARTO basemaps + Leaflet (dark for pros, voyager for students)

```
HackTheWaste/
├── backend/   ← Spring Boot + H2
└── frontend/  ← Vite + React + TS
```

---

## Quick start

Two terminals, no setup beyond `mvn` + `npm`.

**1. Backend**

```bash
cd backend
mvn spring-boot:run
```

→ http://localhost:8080
- API root: `/api`
- H2 console: `/h2-console` (jdbc:h2:mem:leuvengo, user `sa`, no password)

The app seeds itself on first boot with Leuven-specific data: faculties
(Engineering, Law, Medicine, Arts, Economics, Science) with territories,
twelve street segments centred on iconic spots (Oude Markt, Naamsestraat,
Tiensestraat, Vaartkom, Heverlee Campus, Gasthuisberg…), a rewards catalogue
(Velo passes, Alma vouchers, STUK tickets…), and a backlog of demo reports
that already triggered Planon work orders so the dashboards have something
to show.

**2. Frontend**

```bash
cd frontend
npm install
npm run dev
```

→ http://localhost:5173

Vite proxies `/api` to `localhost:8080`, so no CORS hoops in dev.

---

## What's wired up

### Pages

- `/` — **Landing** with role selection, stats, and the 6-step explainer
- `/pro` — **Professional Dashboard**: heatmap, KPIs, 7-day trend, top clusters, segment ranking by AI score, recent Planon orders
- `/pro/operations` — Crew **operations queue**, complete a work order to resolve its hotspot
- `/student` — **Student Dashboard**: faculty territories on the map, picker for "your clan", clan rankings, dirtiest streets near you
- `/student/leaderboard` — Full **clan leaderboard** with progress bars
- `/report` — **One-message reporting**: rating · photo · location · tags · clan
- `/market` — **Rewards marketplace** filtered by category

### REST API (selected)

| Method | Path                                  | Purpose                                       |
| ------ | ------------------------------------- | --------------------------------------------- |
| POST   | `/api/reports`                        | Submit a one-message report                   |
| GET    | `/api/reports/recent`                 | 50 most recent reports                        |
| GET    | `/api/hotspots`                       | All hotspots, sorted by report count          |
| GET    | `/api/hotspots/active`                | Open + escalated + dispatched only            |
| GET    | `/api/segments`                       | Street segments with current AI scores        |
| GET    | `/api/faculties`                      | Faculties (clans) with territory GeoJSON      |
| GET    | `/api/leaderboard`                    | Ranked clans with running totals              |
| GET    | `/api/work-orders`                    | Recent Planon work orders                     |
| POST   | `/api/work-orders/{id}/complete`      | Mark order completed → resolves hotspot       |
| POST   | `/api/work-orders/dispatch/{hsId}`    | Manually escalate a hotspot to Planon         |
| GET    | `/api/rewards`                        | Marketplace items                             |
| GET    | `/api/stats`                          | Dashboard KPIs + 7-day trend + top hotspots   |
| GET    | `/api/stats/city`                     | City config (centre, thresholds)              |

---

## Core logic

### Traffic Logic — *prevent duplicate work orders*

`TrafficService` collapses any incoming report into the nearest **open** hotspot
inside `clusterRadiusM` (default **40 m**). Centroid is updated as a weighted
mean, severity as a running mean of citizen 0-5 ratings. If no open hotspot is
within range, a fresh one is opened.

A hotspot auto-escalates to a Planon work order once it crosses
`workOrderThreshold` reports (default **3**). Statuses flow:

```
OPEN → ESCALATED → DISPATCHED → RESOLVED
```

Idempotent — re-dispatching the same hotspot reuses the existing order.

### AI cleanliness score — 0-100 per segment

`ScoringService` recomputes after each new report. For each segment we sum a
recency-decayed penalty over reports within an 80 m radius: filthier ratings
penalise harder, recency follows a 24-hour half-life, score floors at 0 and
caps at 100. The interface is identical to what an image-classifier model
would produce, so swapping in a vision model is a drop-in change.

### Planon integration

`PlanonService.dispatch()` builds a `WorkOrder` (priority is derived from
severity + cluster size, crew is routed by district) and calls a
mock REST endpoint. The actual HTTP call is stubbed but the call site is
explicit so production can plug in `RestClient` against Planon's
`/Order/Create` endpoint. The Planon ref is persisted on the order.

### GDPR posture

- No PII stored. Reporters get a **rotating session pseudo-id** (e.g.
  `pid-x7y2z3a8`) generated client-side and stored in `sessionStorage`.
- **Photos are never persisted as raw bytes** — the frontend hashes them and
  sends only an opaque `imageRef` fingerprint. Production would forward the
  bytes to a private object store + classifier and persist only the resulting
  derived signal tags.
- Backend stores **only derived metrics** (rating, severity, tag string,
  geo metadata, recency). No phone numbers, no emails, no IPs in domain
  tables.
- Cluster radius and recency-window are tunable in `application.yml`.

### Leuven flavour

- Demo segments cover Oude Markt, Naamsestraat, Tiensestraat, Bondgenotenlaan,
  Brusselsestraat, Diestsestraat, Vaartkom, Heverlee Campus, Gasthuisberg,
  Sint-Maartensdal, Parkstraat, Kessel-Lo Station.
- Demo reports cluster around Oude Markt (party residue) and student housing
  near Naamsestraat — exactly the spots that surge with the academic
  calendar. Tweak `cluster-radius-m` and `work-order-threshold` to model
  seasonal population shifts.

---

## Tunables (`backend/src/main/resources/application.yml`)

```yaml
leuvengo:
  traffic:
    cluster-radius-m: 40        # how close two reports must be to merge
    report-ttl-hours: 72        # decay window
    work-order-threshold: 3     # auto-dispatch after N merged reports
  planon:
    base-url: http://localhost:8080/mock-planon
    api-key: demo-key
  city:
    name: Leuven
    centerLat: 50.8798
    centerLng: 4.7005
```

---

## Build commands

```bash
# Backend
mvn -f backend/pom.xml -DskipTests package

# Frontend
cd frontend && npm run build
```

Both produce deployable artefacts (`backend/target/leuven-go-0.1.0.jar`,
`frontend/dist/`).

---

Made for Leuven. ♻️
