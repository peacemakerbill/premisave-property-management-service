<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&amp;color=1A3C34&amp;height=240&amp;section=header&amp;text=Premisave%20Property%20Service&amp;fontSize=52&amp;fontColor=ffffff&amp;fontAlignY=34&amp;desc=Property%20lease%20tenant%20and%20rent%20management%20microservice&amp;descSize=18&amp;descAlignY=58&amp;animation=fadeIn" alt="Premisave Property Service - property, lease, tenant and rent management microservice banner" />
</p>

<h1 align="center">Premisave Property Management Service: Property, Lease &amp; Rent Management API</h1>

<p align="center">
  <b>A production-minded Spring Boot 4 &amp; MongoDB microservice that runs the property, lease, tenant, and rent side of the Premisave platform — with every rent and utility payment settled through the sibling wallet service, never a payment provider directly.</b>
</p>

<p align="center">
  <a href="https://github.com/peacemakerbill">
    <img src="https://avatars.githubusercontent.com/u/262163756?v=4&amp;s=200" width="110" height="110" alt="Bill Graham Peacemaker (peacemakerbill) GitHub profile picture" />
  </a>
  <br/>
  <sub>Built by <a href="https://github.com/peacemakerbill"><b>Bill Graham Peacemaker</b></a> (<code>@peacemakerbill</code>) · Backend Developer &amp; API Support Engineer · Nairobi, Kenya</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&amp;logo=openjdk&amp;logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=for-the-badge&amp;logo=springboot&amp;logoColor=white" alt="Spring Boot 4.1.1" />
  <img src="https://img.shields.io/badge/Spring_Framework-7.0.9-6DB33F?style=for-the-badge&amp;logo=spring&amp;logoColor=white" alt="Spring Framework 7" />
  <img src="https://img.shields.io/badge/Spring_Security-7-6DB33F?style=for-the-badge&amp;logo=springsecurity&amp;logoColor=white" alt="Spring Security 7" />
  <img src="https://img.shields.io/badge/MongoDB-Spring_Data-47A248?style=for-the-badge&amp;logo=mongodb&amp;logoColor=white" alt="MongoDB with Spring Data" />
  <img src="https://img.shields.io/badge/Redis-Cache-DC382D?style=for-the-badge&amp;logo=redis&amp;logoColor=white" alt="Redis cache" />
  <img src="https://img.shields.io/badge/RabbitMQ-AMQP-FF6600?style=for-the-badge&amp;logo=rabbitmq&amp;logoColor=white" alt="RabbitMQ" />
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&amp;logo=apachemaven&amp;logoColor=white" alt="Apache Maven" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Leases_%26_Occupancy-UNIT_%2F_WHOLE__PROPERTY-1A3C34?style=for-the-badge" alt="Lease and occupancy management" />
  <img src="https://img.shields.io/badge/Rent_%26_Utility_Billing-USD-C9A84C?style=for-the-badge" alt="Rent and utility billing in USD" />
  <img src="https://img.shields.io/badge/Payments-via_Wallet_Service-635BFF?style=for-the-badge" alt="Payments settled via the wallet service" />
  <img src="https://img.shields.io/badge/OpenFeign-Service_to_Service-6DB33F?style=for-the-badge" alt="OpenFeign service-to-service calls" />
  <img src="https://img.shields.io/badge/OpenAPI-3-85EA2D?style=for-the-badge&amp;logo=swagger&amp;logoColor=white" alt="OpenAPI 3 / Swagger UI" />
</p>

<p align="center">
  <a href="https://github.com/peacemakerbill/premisave-property-management-service/stargazers"><img src="https://img.shields.io/github/stars/peacemakerbill/premisave-property-management-service?style=for-the-badge&amp;logo=github" alt="GitHub stars" /></a>
  <a href="https://github.com/peacemakerbill/premisave-property-management-service/network/members"><img src="https://img.shields.io/github/forks/peacemakerbill/premisave-property-management-service?style=for-the-badge&amp;logo=github" alt="GitHub forks" /></a>
  <a href="https://github.com/peacemakerbill/premisave-property-management-service/issues"><img src="https://img.shields.io/github/issues/peacemakerbill/premisave-property-management-service?style=for-the-badge" alt="Open issues" /></a>
  <a href="https://github.com/peacemakerbill/premisave-property-management-service/commits"><img src="https://img.shields.io/github/last-commit/peacemakerbill/premisave-property-management-service?style=for-the-badge" alt="Last commit" /></a>
  <img src="https://img.shields.io/github/repo-size/peacemakerbill/premisave-property-management-service?style=for-the-badge" alt="Repository size" />
  <img src="https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge" alt="Proprietary license" />
  <img src="https://img.shields.io/badge/Made_in-Nairobi,_Kenya-000000?style=for-the-badge" alt="Made in Nairobi, Kenya" />
</p>

<p align="center">
  <a href="#quick-start">Quick start</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="#how-a-rent-payment-works">How a rent payment works</a> ·
  <a href="#service-availability-and-resilience">Service availability</a> ·
  <a href="#api-reference">API reference</a> ·
  <a href="#configuration-reference">Configuration</a> ·
  <a href="#troubleshooting">Troubleshooting</a> ·
  <a href="#known-limits">Known limits</a>
</p>

> **If this saves you time building a property/lease domain service in Java, please star the repo.** It helps other Kenyan fintech and proptech developers find it.

> **Disclaimer.** This is an independent, community-built project shared as a portfolio and reference implementation. It is not an official product of, and is not affiliated with or endorsed by, Safaricom or any payment provider. This service never talks to M-Pesa, Stripe, PayPal, Flutterwave, or NOWPayments directly — every payment is settled by the sibling [Premisave Wallet Service](https://github.com/peacemakerbill/premisave-wallet-service), which owns that integration surface. M-PESA is a trademark of Safaricom PLC.

---

## Table of contents

1. [What is this?](#what-is-this)
2. [Features](#features)
3. [Architecture](#architecture)
4. [How a rent payment works](#how-a-rent-payment-works)
5. [Service availability and resilience](#service-availability-and-resilience)
6. [Currency and payment model](#currency-and-payment-model)
7. [Quick start](#quick-start)
8. [Build and run](#build-and-run)
9. [Configuration reference](#configuration-reference)
10. [API reference](#api-reference)
11. [Testing with curl](#testing-with-curl)
12. [Data model](#data-model)
13. [Going live](#going-live)
14. [Security notes](#security-notes)
15. [Project structure](#project-structure)
16. [Troubleshooting](#troubleshooting)
17. [Upgrading from earlier deliveries](#upgrading-from-earlier-deliveries)
18. [Known limits](#known-limits)
19. [Roadmap ideas](#roadmap-ideas)
20. [Contributing](#contributing)
21. [Author](#author)

---

## What is this?

**Premisave Property Management Service** is the property, lease, and tenancy backbone of the Premisave platform: a Java Spring Boot microservice that owns everything about a property from the moment it is listed to the moment a tenant moves out — units, leases, occupancy, rent schedules, security deposits, utility bills, maintenance, and inspections.

It deliberately does **not** own payments. Every rent and utility payment is a wallet-to-wallet transfer, debiting the tenant's Premisave wallet and crediting the property owner's, executed by a single call to the sibling [wallet service](https://github.com/peacemakerbill/premisave-wallet-service)'s internal API. This service:

- models both lease-backed tenancies (with a generated monthly rent schedule) and directly-occupied units (with a running arrears/credit ledger), under one payment API,
- moves money by delegating to the wallet service, never a payment gateway, and never lets a payment request through if the wallet service or auth-service is unreachable,
- makes every payment idempotent by reference, so a retried request after a timeout can never double-charge a tenant,
- sends a fully designed HTML (plus plain-text) email for every payment, deposit, utility bill, and notice, and
- fails fast and clearly — a dependency being down returns a plain-language "service is offline" response instead of a timeout or a stack trace.

It was built for the Premisave platform's own microservice ecosystem (alongside `auth-service` and `wallet-service`), reached by end users via JWT and by sibling services via a shared internal API key.

## Features

| | |
|---|---|
| **Two tenancy models, one payment API** | Lease-backed tenancies (unit or whole-property, with a generated `RentSchedule`) and directly-occupied units (with a running `RentBalance` ledger) share the same payment, deposit, and notice endpoints. |
| **Wallet-funded payments** | Rent and utility bill payments call the wallet service's `/internal/transfer` once, moving money from the tenant to the owner. This service books the payment locally only after that transfer succeeds. |
| **Idempotent by design** | Every payment accepts an optional client reference. A `wallet_transfers` collection (unique-indexed on reference) makes a retried request safe — it is never charged twice, whether the previous attempt succeeded, failed, or timed out. |
| **Service availability checks** | Endpoints that need auth-service or the wallet service check reachability first, and return a friendly, structured 503 — not a hang or a 500 — when a dependency is down. A `GET /system/dependencies` endpoint reports live status for a frontend to poll. |
| **Security deposits with partial refunds** | Deposits are held once per tenancy, with a running refund history; a partial refund requires a reason, a final refund closes the deposit out. |
| **Utility billing from meter readings** | A meter reading can auto-generate a bill using a configured per-unit rate, with duplicate-reading and duplicate-bill guards. |
| **Maintenance to work order pipeline** | A tenant's maintenance request can be turned into an assigned work order, with status kept in sync in both directions. |
| **Bulk and scheduled notices** | Rent reminders and other notices can be sent to many units/leases at once, immediately or at a future time, with per-recipient delivery results and a 24-hour duplicate-notice guard. |
| **Designed HTML email notifications** | Payment receipts, deposit and refund notices, utility bills, failed-payment alerts, and owner-written notices are all sent as branded HTML (Thymeleaf) with a plain-text fallback — no plain default Spring emails. |
| **Owner and tenant "quick" onboarding** | One-click profile creation that pulls name/phone/email straight from the caller's own auth-service account, plus a sync-status check so the frontend can prompt "your profile changed — sync now?". |
| **Dual authentication model** | JWT for end users (via a shared secret with auth-service), a shared internal API key for service-to-service calls from the rest of the Premisave ecosystem. |
| **OpenAPI 3 documentation** | Full API spec generation via springdoc-openapi. |

## Architecture

```
┌──────────────┐      JWT       ┌────────────────────────┐
│   End User   │ ─────────────► │                        │
└──────────────┘                │                        │      ┌─────────────────┐
                                 │  Premisave Property     │ ───► │  auth-service   │  profile lookups,
┌──────────────┐  X-API-Key     │  Management Service     │ ◄─── │                 │  quick-create/sync
│ sibling svcs │ ◄────────────► │   (this repository)     │      ├─────────────────┤
└──────────────┘                │                        │ ───► │  wallet-service │  /internal/transfer
                                 │  MongoDB · Redis        │      │                 │  (rent & bill payments)
                                 │  RabbitMQ · Mail (SMTP) │      ├─────────────────┤
                                 │                        │ ───► │ listing-service │  property sync
                                 │                        │ ───► │ booking-service │  unit availability
                                 └────────────────────────┘      └─────────────────┘
```

**Design choices**

- **This service never touches a payment gateway.** A rent or utility payment is a single, idempotent call to the wallet service's `/internal/transfer` — M-Pesa, Stripe, PayPal, Flutterwave, and crypto are entirely the wallet service's concern.
- **A dependency being down is a first-class, user-facing state, not a stack trace.** Payment and profile endpoints declare which services they need; if one is unreachable, the caller gets a clear 503 with a retryable reference, before any validation or database write happens.
- **The owner's wallet account is resolved server-side, never trusted from the request.** It comes from auth-service via the owner's own `userId`, not from a freely-editable email field on the owner's profile — so a payment can never be misdirected to the wrong wallet.
- **Two tenancy shapes, one mental model.** A lease-backed tenancy and a directly-occupied unit differ in how "what's owed" is derived (a generated schedule vs. a running balance), but share the same payment request shape, the same deposit rules, and the same notice endpoints.

## How a rent payment works

```mermaid
sequenceDiagram
    autonumber
    participant T as Tenant
    participant PS as Property Service
    participant WS as Wallet Service
    participant DB as MongoDB
    participant O as Property Owner

    T->>PS: POST /rent/pay { leaseId, amount, reference }
    PS->>PS: Check auth-service & wallet-service are reachable
    PS->>PS: Validate lease, deposit requirement, outstanding schedule
    PS->>WS: POST /internal/transfer (tenant wallet to owner wallet)
    WS-->>PS: Transfer confirmed
    PS->>DB: Apply payment to deposit / rent schedule, mark wallet_transfers BOOKED
    PS-->>T: 200 — payment receipt
    par Async, off the request thread
        PS-->>T: Email + record: payment received
    and
        PS-->>O: Email: rent received into your wallet
    end
```

If the wallet service can't be reached, or the transfer's outcome can't be confirmed, the request fails **before** any local record is booked — the response tells the tenant plainly that nothing was charged, or gives back the same `reference` to safely retry. The same shape covers direct-unit rent payments and utility bill payments, against a running balance and a bill's paid-so-far total respectively, instead of a generated schedule.

## Service availability and resilience

Every endpoint that depends on auth-service or the wallet service is annotated to declare that dependency. Before the request does anything else, a background health monitor's cached status is checked:

- **A service counts as online** if its health path answers with anything other than a gateway-style 502/503/504 — even a 401 or 404 proves it is alive, so a protected or missing health path is not mistaken for an outage.
- **Results are cached** (30 seconds when up, 3 seconds when down, refreshed every 10 seconds in the background), so a busy endpoint almost never waits on a live probe, and a real request that fails to connect marks the service down immediately.
- **A dependency being down never surfaces as a 500 or a hang.** The response is a 503 with a plain-language message (for example, *"We can't process payments right now because our wallet is offline. Nothing has been charged."*), a `retryAfterSeconds`, and, for payments, the reference to retry with.
- **`GET /system/dependencies`** reports live per-service status, so a frontend can grey out a "Pay rent" button before the tenant ever taps it.

## Currency and payment model

- **USD is the only currency this service ever records**, matching the wallet service's own USD-denominated wallets — a rent amount, a utility rate, and a deposit are all plain USD figures.
- **This service moves no money itself.** A payment request results in exactly one wallet-service transfer call; this service only ever books the *result* of that transfer against a lease, a unit's balance, a deposit, or a bill.
- **Amounts have at most 2 decimal places** and must be positive. Bills generated from a meter reading are rounded half up to the cent.
- **Idempotency**: every payment accepts an optional caller-supplied `reference`. Retrying with the same one is guaranteed safe — it returns the original result rather than moving money again — tracked via a unique-indexed `wallet_transfers` collection.

## Quick start

### Prerequisites

- **Java 21**
- **Maven 3.9+**
- **MongoDB** running locally, in Docker, or on Atlas
- **Redis** running locally or hosted (used for `@EnableCaching`)
- **RabbitMQ** running locally or hosted
- The sibling [**auth-service**](https://github.com/peacemakerbill/premisave_auth_service) and [**wallet-service**](https://github.com/peacemakerbill/premisave-wallet-service) running, or their `/health`-style endpoints reachable — otherwise payment and profile-sync endpoints will correctly, but visibly, answer 503

```bash
# 1. Clone
git clone https://github.com/peacemakerbill/premisave-property-management-service.git
cd premisave-property-management-service

# 2. Start MongoDB, Redis, and RabbitMQ (skip any you already have)
docker run -d --name property-mongo -p 27017:27017 mongo:8
docker run -d --name property-redis -p 6379:6379 redis:7
docker run -d --name property-rabbitmq -p 5672:5672 rabbitmq:4-management

# 3. Create your .env (see the next section), then run
mvn spring-boot:run
```

Create a `.env` file next to `pom.xml`:

```properties
# Core
MONGODB_URI=mongodb://localhost:27017/premisave-property
REDIS_HOST=localhost
REDIS_PORT=6379
RABBITMQ_HOST=localhost
JWT_SECRET=<a long, random secret shared with auth-service>
INTERNAL_API_KEY=<a shared secret, identical across every Premisave microservice>

# Sibling services
AUTH_SERVICE_URL=http://localhost:8080
WALLET_SERVICE_URL=http://localhost:8084
LISTING_SERVICE_URL=http://localhost:8082
BOOKING_SERVICE_URL=http://localhost:8083

# Outgoing email (payment receipts, deposit/bill notices, owner notices)
MAIL_USERNAME=...
MAIL_PASSWORD=...
NOTICE_EMAIL_FROM=no-reply@premisave.com

# Utility billing rates (USD per unit of metered consumption)
UTILITY_RATE_ELECTRICITY=0.15
UTILITY_RATE_WATER=1.50

# Browser origins allowed to call this API (comma separated)
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

> **Important:** write `.env` values without quotes and without trailing spaces. `INTERNAL_API_KEY` must be byte-for-byte identical to the value configured in auth-service and wallet-service — a mismatch shows up as 401s on every internal call, not a startup error. A *blank* `JWT_SECRET` or `INTERNAL_API_KEY` does stop the service at startup, with a message naming the missing variable.

When the service is healthy you will see lines like:

```
Tomcat started on port 8085 (http) with context path '/'
Monitor thread successfully connected to server with description ServerDescription{address=localhost:27017, ...}
Started PremisavePropertyManagementApplication in 5.6 seconds
```

## Build and run

```bash
# Compile and package an executable jar
mvn clean package

# Run it (environment variables or a .env in the working directory)
java -jar target/premisave-property-management-service-0.0.1-SNAPSHOT.jar
```

**Notes**

- `.env` is loaded from the working directory. Real environment variables (Docker, Kubernetes, systemd) also work and take precedence over `.env`.
- The service starts on **port 8085** by default (`server.port` in `application.yml`).
- Set `SERVICE_HEALTH_CHECKS_ENABLED=false` to switch off the auth-service/wallet-service reachability checks entirely — useful for isolated local testing, at the cost of payment requests going back to hanging or erroring against a dead dependency instead of failing fast.

## Configuration reference

Every setting lives in `src/main/resources/application.yml` and can be overridden by an environment variable or `.env`.

| Environment variable | Default | Required | Description |
|---|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/premisave-property` | no | MongoDB connection string. |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | no | Redis connection, backing `@EnableCaching`. |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | no | RabbitMQ connection. |
| `JWT_SECRET` | none | **yes** | Must match every other Premisave service validating the same tokens. The service will not start without it. |
| `INTERNAL_API_KEY` | none | **yes** | The service will not start without it. Shared secret this service presents as `X-API-Key` to auth-service and wallet-service, and expects from sibling services calling in. |
| `AUTH_SERVICE_URL` | `http://localhost:8080` | no | Base URL of auth-service. |
| `WALLET_SERVICE_URL` | `http://localhost:8084` | no | Base URL of the wallet service; every rent/utility payment goes through its `/internal/transfer`. |
| `LISTING_SERVICE_URL` / `BOOKING_SERVICE_URL` | `http://localhost:8082` / `8083` | no | Base URLs of the listing and booking services. |
| `SERVICE_HEALTH_CHECKS_ENABLED` | `true` | no | Turns the auth-service/wallet-service reachability checks on or off. |
| `AUTH_SERVICE_HEALTH_PATH` / `WALLET_SERVICE_HEALTH_PATH` | `/health` / `/system/health` | no | Health path probed on each dependency. |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | `smtp.gmail.com` / `587` / — / — | **yes, for email** | SMTP credentials for outgoing notifications. |
| `NOTICE_EMAIL_FROM` / `NOTICE_EMAIL_FROM_NAME` | `no-reply@premisave.com` / `Premisave` | no | From address and display name on every outgoing email. |
| `SUPPORT_EMAIL` | `support@premisave.com` | no | Shown as the contact address in every email footer. |
| `UTILITY_RATE_ELECTRICITY` / `UTILITY_RATE_WATER` | `0.15` / `1.50` | no | USD charged per unit of metered consumption when auto-generating a bill from a meter reading. Starting points; set your real rates. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:4200,http://localhost:5173` | **yes, when deploying** | Comma-separated browser origins allowed to call the API. A single `*` allows every origin and turns credentials off; use it only for local experiments. |
| `LOG_LEVEL` | `INFO` | no | Log level for `com.premisave.property`. Use `DEBUG` while developing. |
| `SYSTEM_TEST_TOKEN_ENABLED` | `false` | no | Turns on `GET /system/test-token`, which decodes a JWT and shows its claims. Leave off outside development. |
| `FRONTEND_URL` | `http://localhost:3000` | no | Used as the link behind every email's call-to-action button. |

The service **refuses to start** if `INTERNAL_API_KEY` or `JWT_SECRET` is blank. A blank `JWT_SECRET` would otherwise sign and accept tokens with an all-zero key that anyone can forge.

### Environment variables by group

`additional-spring-configuration-metadata.json` documents the keys for the IDE.

| Group | Variables |
|:-|:-|
| Database and cache | `MONGODB_URI`, `REDIS_HOST`, `REDIS_PORT` |
| Messaging | `RABBITMQ_HOST`, `RABBITMQ_PORT` |
| Auth | `JWT_SECRET` |
| Internal calls | `INTERNAL_API_KEY` |
| Sibling services | `AUTH_SERVICE_URL`, `WALLET_SERVICE_URL`, `LISTING_SERVICE_URL`, `BOOKING_SERVICE_URL` |
| Availability checks | `SERVICE_HEALTH_CHECKS_ENABLED`, `AUTH_SERVICE_HEALTH_PATH`, `WALLET_SERVICE_HEALTH_PATH` |
| Mail | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (Gmail needs an app password), `NOTICE_EMAIL_FROM`, `NOTICE_EMAIL_FROM_NAME`, `SUPPORT_EMAIL` |
| Frontend and browsers | `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` |
| Billing | `UTILITY_RATE_ELECTRICITY`, `UTILITY_RATE_WATER` |
| Operations | `LOG_LEVEL`, `SYSTEM_TEST_TOKEN_ENABLED` |

### What the service checks at startup

- **Secrets.** A blank `JWT_SECRET` or `INTERNAL_API_KEY` stops the service with a message naming the variable.
- **The payment ledger index.** The unique index on `wallet_transfers.reference` is what makes a duplicate wallet payment impossible under concurrent requests, so if it cannot be created the service does not start.
- **Every other index** (one row per owner, tenant, deposit and bill, plus the lookup indexes the endpoints use) is created on a best effort basis. One that cannot be built, for example because existing rows already break a unique rule, is logged as a warning and skipped, so one bad row cannot take the service down. Unique indexes are sparse, so older rows missing the field do not clash.

## API reference

Base URL (local): `http://localhost:8085`

| Namespace | Auth | Purpose |
|---|---|---|
| `/api/v1/properties/**`, `/api/v1/units/**` | JWT (owner-only for writes) | Properties and rental units |
| `/api/v1/leases/**`, `/api/v1/occupancy/**` | JWT | Leases and move-in/move-out history |
| `/api/v1/owners/**`, `/api/v1/tenants/**` | JWT | Owner and tenant profiles, including one-click create/sync |
| `/api/v1/rent/**` | JWT | Lease-backed and direct-unit rent payments, balances, and schedules |
| `/api/v1/security-deposits/**` | JWT | Deposit hold, refund, and refund-preview |
| `/api/v1/utility-bills/**`, `/api/v1/meter-readings/**` | JWT | Utility billing and meter readings |
| `/api/v1/maintenance/**`, `/api/v1/work-orders/**`, `/api/v1/inspections/**` | JWT | Maintenance requests, work orders, inspections |
| `/api/v1/notices/**` | JWT (owner-only for bulk) | Single, bulk, and scheduled notices |
| `/api/v1/blacklisted-tenants/**` | JWT | Tenant blacklist |
| `/api/v1/dashboard/**`, `/api/v1/reports/**` | JWT (owner) | Owner dashboard and occupancy/revenue reports |
| `/system/**` | Mixed | Health checks and `GET /system/dependencies` |

### `POST /api/v1/rent/pay`: pay rent on a lease

| | |
|---|---|
| **Header** | `Authorization: Bearer <JWT>` |

Request:

```json
{
  "leaseId": "6a30a1b2c3d4e5f607182930",
  "amount": 500.00,
  "paymentMethod": "WALLET",
  "reference": "rent-sep-2026"
}
```

Response (`200`):

```json
{
  "id": "6a31b7c8d9e0f1a2b3c4d5e6",
  "leaseId": "6a30a1b2c3d4e5f607182930",
  "paymentType": "RENT",
  "amount": 500.00,
  "rentAmountApplied": 500.00,
  "depositAmountApplied": 0,
  "status": "PAID",
  "paymentMethod": "WALLET",
  "paymentReference": "PRP-<tenantId>-rent-sep-2026",
  "paidAt": "2026-09-19T15:42:10.482"
}
```

`paymentMethod` accepts only `WALLET` (or omitted) — wallets are topped up with M-Pesa/Stripe/etc. in the wallet service, not here. `reference` is optional but strongly recommended: reusing it on retry never double-charges.

### `POST /api/v1/rent/units/pay`: pay rent on a directly-occupied unit

Same shape as above, with `rentalUnitId` in place of `leaseId`; the response carries `resultingBalance` (positive = arrears remain, negative = the tenant is in credit) instead of a rent-schedule breakdown.

### `POST /api/v1/utility-bills/pay`: pay a utility bill

| | |
|---|---|
| **Header** | `Authorization: Bearer <JWT>` |

```json
{
  "billId": "6a3122b3c4d5e6f708192a3b",
  "amount": 30.00,
  "paymentMethod": "WALLET",
  "reference": "elec-sep-2026-1"
}
```

### `GET /system/dependencies`: live status of auth-service and wallet-service

| | |
|---|---|
| **Header** | `Authorization: Bearer <JWT>` |

```json
{
  "status": "DEGRADED",
  "services": {
    "auth-service":   { "name": "Authentication service", "status": "UP" },
    "wallet-service": { "name": "Premisave Wallet",       "status": "DOWN" }
  },
  "timestamp": "2026-09-22T21:01:15.556234"
}
```

## Testing with curl

```bash
BASE=http://localhost:8085
TOKEN="<a real JWT for a test user>"

# Check dependency status before attempting a payment
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/system/dependencies"

# Pay rent on a lease
curl -s -X POST "$BASE/api/v1/rent/pay" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"leaseId":"<lease id>","amount":500.00,"paymentMethod":"WALLET","reference":"rent-test-1"}'

# Check what a tenant currently owes on a lease
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/rent/due/<lease id>"

# One-click owner profile creation, pulled from the caller's own auth-service account
curl -s -X POST "$BASE/api/v1/owners/quick" -H "Authorization: Bearer $TOKEN"
```

**Variables worth saving in Postman:** `base_url_property`, `token`, `lease_id`, `rental_unit_id`, `unit_id`, `payment_reference`, `internal_api_key`.

## Data model

Selected MongoDB collections and the fields most integration partners need:

| Collection | Purpose |
|---|---|
| `properties` / `rental_units` | A property and its individual rentable units, with occupancy status. |
| `owners` / `tenants` | Profiles synced from auth-service, plus property-domain-specific fields (bank details, current address, blacklist flag). |
| `leases` | `leaseType` (`UNIT` or `WHOLE_PROPERTY`), term, monthly rent, and status. |
| `rent_schedules` | One generated row per billing period of a lease, with amount due/paid and status. |
| `rent_balances` | A running arrears/credit ledger for directly-occupied (no-lease) units. |
| `lease_rent_unit_payments` / `unit_rent_payments` | Every rent payment transaction, carrying the wallet-service `paymentReference` that ties it back to the money movement. |
| `security_deposits` | One deposit per tenancy, with an embedded refund history. |
| `utility_bills` / `meter_readings` | Metered consumption and the bills generated from it. |
| `maintenance_requests` / `work_orders` | A tenant's request and the assigned work carried out against it. |
| `inspections` | Scheduled and completed property inspections, with or without a system-account inspector. |
| `notices` / `scheduled_notices` | Individual and bulk/scheduled tenant communications, with per-recipient delivery results. |
| `occupancy_history` | Every move-in/move-out, lease-backed or direct. |
| `wallet_transfers` | The idempotency ledger for every payment this service has asked the wallet service to move — what makes retries safe. |
| `blacklisted_tenants` / `audit_logs` | Tenant blacklist entries and a general-purpose audit trail. |

## Going live

1. **Confirm `INTERNAL_API_KEY` and `JWT_SECRET`** are strong, and identical to the values configured in auth-service and wallet-service.
2. **Point `AUTH_SERVICE_URL` and `WALLET_SERVICE_URL`** at their real, network-reachable addresses, and confirm each one's health path (`AUTH_SERVICE_HEALTH_PATH`, `WALLET_SERVICE_HEALTH_PATH`) is reachable from this service.
3. **Set real `UTILITY_RATE_*` values** — the defaults are placeholders and will misprice any bill generated from a meter reading until set.
   **Set `CORS_ALLOWED_ORIGINS`** to the real frontend address(es), and leave `SYSTEM_TEST_TOKEN_ENABLED` off.
4. **Confirm outgoing email** works end-to-end (a payment, a deposit, and a notice) against your real SMTP credentials before relying on it for tenant communication.
5. **Decide on `SERVICE_HEALTH_CHECKS_ENABLED`** deliberately — leaving it `true` (the default) is almost always right in a real deployment, since it is what turns a dead dependency into a clear 503 instead of a hang.

## Security notes

- **JWT authentication** for all end-user-facing endpoints, validated against the platform's shared secret.
- **Internal API key authentication** (`X-API-Key`) for service-to-service calls, kept entirely separate from user JWTs.
- **Method-level authorization** (`@PreAuthorize`) restricts owner-only actions (creating a property, bulk notices, dashboard/reports) to the `HOME_OWNER` role.
- **Record-level authorization** (`AccessGuard`) decides who may touch a specific record. A role only says the caller *is* an owner, never that they own *this* property, so every check walks record, unit, property, owner. See [Who can access what](#who-can-access-what).
- **Unauthenticated requests get a JSON 401** with `code` `AUTHENTICATION_REQUIRED` or `SESSION_EXPIRED`, so the frontend can send the user back to sign in. A signed-in caller reaching for someone else's record gets a 403.
- **Money inputs are validated at the edge**: amounts must be positive with at most 2 decimal places, and bills generated from a meter reading are rounded to the cent (and rejected if they round to zero) so every bill can actually be paid.
- **A half-finished booking is rolled back.** If the money has moved but a local step fails, the deposit hold and rent application made by that attempt are undone, and the transfer remembers how the payment was split, so the retry with the same `reference` finishes it without applying anything twice.
- **The property owner's wallet destination is never taken from the request or from a freely-edited profile field** — it is resolved from auth-service via the owner's own `userId` at payment time.
- **Every payment is idempotent by reference**, closing off retry-driven double-charging as a failure mode entirely.
- **Stateless sessions** — no server-side session state, fully horizontally scalable.

### Who can access what

| Record | Who can read or change it |
|---|---|
| Property, units, leases, deposits, occupancy history | The owner of the property (or an admin) manages them. A tenant can read what concerns their own lease or unit. |
| Rent due, payment history, rent schedule, notices, maintenance, inspections | The property's owner, plus the tenant concerned. Inspections also go to the person who scheduled or was assigned them. |
| A tenant's profile, leases, bills, notices and requests | The tenant, an admin, or an owner they rent from. An owner only ever sees the part tied to their own properties. |
| The tenant's total rent summary (`/rent/balance/tenant/{id}`) | The tenant or an admin. It adds up balances across every landlord, so owners use the per-lease and per-unit balances. |
| An owner profile by id (`/owners/{id}`) | The owner or an admin, since it carries bank details. |
| Blacklist entries | Any registered owner can look a tenant up when screening. Only the owner who listed a tenant (or an admin) can remove them, and an owner's list shows only their own entries. |
| Work orders | The unit's owner, the assigned technician, and (read only) the tenant who raised the request. |
| `/system/health/details` | Admins only. `/system/health` stays public and returns status only. |

## Project structure

```
premisave-property-management-service/
├── src/main/java/com/premisave/property/
│   ├── config/          # Security, Mongo, Redis, RabbitMQ, CORS, Async, Feign, OpenAPI
│   ├── client/           # Feign clients: auth-service, wallet-service, listing-service, booking-service
│   ├── controller/       # REST controllers
│   ├── service/          # Domain services (one per aggregate) + WalletPaymentService, PaymentNotificationService
│   ├── health/           # ServiceHealthMonitor, @RequiresServices, the offline-check interceptor
│   ├── dto/               # Request/response DTOs
│   ├── entity/            # MongoDB documents
│   ├── repository/        # Spring Data MongoDB repositories
│   ├── security/          # JWT filter & service, AccessGuard (record-level access), 401 entry point
│   ├── exception/         # Domain exceptions + GlobalExceptionHandler
│   └── PremisavePropertyManagementApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── templates/email/   # Thymeleaf HTML email templates
└── pom.xml
```

## Troubleshooting

<details>
<summary><b>App fails to start: "No qualifying bean of type 'CacheManager' available"</b></summary>

`@EnableCaching` (in `CacheConfig`) has nothing to bind to unless `spring-boot-starter-cache` is on the classpath alongside `spring-boot-starter-data-redis` — having Redis alone is not enough. Add `spring-boot-starter-cache` to `pom.xml`.
</details>

<details>
<summary><b>App fails to start: "No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available"</b></summary>

Something is asking Spring to inject a Jackson 2 `ObjectMapper` bean. Under Jackson 3 (the default), Spring no longer auto-configures one. If the usage is a narrow, internal JSON parse rather than the app's actual HTTP message conversion, construct a plain `new ObjectMapper()` locally instead of requesting one from the container.
</details>

<details>
<summary><b>A payment returns 503 with "service is offline" even though the dependency looks fine</b></summary>

Check `AUTH_SERVICE_HEALTH_PATH` / `WALLET_SERVICE_HEALTH_PATH` actually resolve against the configured base URL, and that the target answers something other than 502/503/504 — any other status, including 401/404, is treated as "online". A connection refused, DNS failure, or timeout is what actually trips the check.
</details>

<details>
<summary><b>A payment returns 503 saying it couldn't be confirmed, with a reference to retry</b></summary>

This means the wallet-service call either timed out or the connection was lost mid-request — the outcome is genuinely unknown from this service's side. Retry the exact same request with the `reference` given in the error; the `wallet_transfers` ledger guarantees it will not charge the tenant twice, whichever way the first attempt actually landed.
</details>

<details>
<summary><b>MongoDB connects to the wrong database, or the URI seems to be ignored</b></summary>

Confirm the property is `spring.mongodb.uri`, not `spring.data.mongodb.uri` — Spring Boot 4 moved MongoDB's connection properties out of the Spring Data namespace into their own `spring.mongodb.*` prefix.
</details>

<details>
<summary><b>App fails to start: "JWT_SECRET is not set" or "INTERNAL_API_KEY is not set"</b></summary>

Both are required and the service stops on purpose when either is blank. Set `JWT_SECRET` to the value auth-service signs tokens with, and `INTERNAL_API_KEY` to the key shared with auth-service and wallet-service. Check that the `.env` line has no quotes or trailing spaces and that the file sits in the working directory.
</details>

<details>
<summary><b>App fails to start on the <code>wallet_transfers</code> index</b></summary>

The unique index on `reference` could not be created, almost always because two existing rows share a reference. Find them with an aggregation that groups `wallet_transfers` by `reference` and keeps counts above 1, resolve the duplicates, then restart. The service will not run without this index, because it is what stops a duplicate payment under concurrent retries.
</details>

<details>
<summary><b>Startup warning: Could not create index on '...'</b></summary>

Another index could not be built, usually because existing rows already break a unique rule (two owners with one `userId`, for example). The service starts normally and the other indexes are still created. Clean up the duplicates and restart to get the missing index.
</details>

<details>
<summary><b>Every call returns 401 with code SESSION_EXPIRED or AUTHENTICATION_REQUIRED</b></summary>

`SESSION_EXPIRED` means the token was present but expired, so the app should send the user back to sign in. `AUTHENTICATION_REQUIRED` means there was no usable token: none sent, a malformed header, or a signature that does not verify. In the last case check that `JWT_SECRET` is identical to auth-service's. A token that fails verification is logged as a warning ("Rejected an invalid token"); an expired one is logged at `DEBUG`.
</details>

<details>
<summary><b>A signed-in user gets 403 on a record they expect to reach</b></summary>

Access is decided per record, not just by role. An owner reaches a property only if its `ownerId` is their owner profile, and a tenant reaches a lease or unit only if they are the tenant on it. See [Who can access what](#who-can-access-what). A record that does not exist is a 404, so a 403 means it exists but belongs to someone else.
</details>

<details>
<summary><b>The browser blocks calls with a CORS error</b></summary>

Add the exact origin of the frontend (scheme, host and port) to `CORS_ALLOWED_ORIGINS`, comma separated, and restart. A single `*` allows every origin but turns credentials off, so use it only for local experiments.
</details>

<details>
<summary><b><code>GET /system/test-token</code> returns 404</b></summary>

It is off by default. Set `SYSTEM_TEST_TOKEN_ENABLED=true` while developing, and leave it off in production.
</details>

## Upgrading from earlier deliveries

* **Set the new variables.** `CORS_ALLOWED_ORIGINS` has development defaults, so set it to the real frontend address before deploying. `LOG_LEVEL` now defaults to `INFO`. `SYSTEM_TEST_TOKEN_ENABLED` defaults to `false`. Utility rates default to `0.15` (electricity) and `1.50` (water) in USD.
* **Startup is stricter.** A blank `JWT_SECRET` or `INTERNAL_API_KEY` now stops the service. Deployments that relied on a blank value need a real one.
* **Unauthenticated calls now get a JSON 401** with `code` `AUTHENTICATION_REQUIRED` or `SESSION_EXPIRED`, where the old build answered 403. Frontends that treated 403 as "sign in again" should switch to the code.
* **Access is checked per record.** Endpoints that used to accept any signed-in owner or tenant now check the record. Two responses became narrower on purpose: `/owners/{id}` is for the owner or an admin, and the tenant rent summary `/rent/balance/tenant/{id}` is for the tenant or an admin.
* **Blacklist entries are scoped to the owner who created them.** New entries record `createdByOwnerId`. Entries created before the upgrade have none, so only an admin can remove or list them.
* **Indexes are created at startup.** If your data already breaks a unique rule you will see a warning for that index (see [Troubleshooting](#troubleshooting)); the service still starts.
* **Retried payments reuse the saved split.** `wallet_transfers` rows now store how a payment was divided between deposit and rent, so a retry with the same `reference` finishes the booking without applying anything twice. Rows written before the upgrade have no split and fall back to recomputing it, as before.
* **Bills from meter readings are rounded to the cent** and rejected if they round to zero. Money fields in requests accept at most 2 decimal places.
* **Spring Boot 4.1.1.** MongoDB settings use `spring.mongodb.uri`, and JSON handling is Jackson 3.
* **No SMS and no generated password.** Twilio and SMS were removed, and the default Spring Security generated password no longer appears in the log. Remove any `TWILIO_*` variables from `.env`.

## Known limits

* **Deposit refunds.** A refund is recorded locally and does not yet move money out of the owner's wallet. It has to be settled outside this service.
* **Standalone deposits.** `POST /api/v1/security-deposits` books a deposit with no wallet transfer. Only deposits taken as part of a wallet-funded rent payment move money.
* **Crash during a booking.** A failed step after the money has moved is rolled back in code, and a retry with the same `reference` completes it. A JVM crash between two steps cannot be undone that way. Closing that gap fully needs MongoDB transactions, which need a replica set.
* **Cash and off-platform rent.** Payments are wallet only, so an owner cannot log a rent payment made outside the platform.
* **Currency.** USD only. There is no conversion and no other currency is recorded.
* **Notifications.** Emails are sent in the background once and are not retried. A failed email is logged and never fails the payment that triggered it.
* **Sibling services.** The listing and booking Feign clients exist, but their calls are not exercised end to end yet.

## Roadmap ideas

Real, currently-known gaps and follow-ups, not commitments:

- [ ] Move deposit refunds out of the owner's wallet back to the tenant through the wallet service (see [Known limits](#known-limits))
- [ ] Route the standalone `POST /security-deposits` endpoint through a wallet transfer, like the deposits taken with rent
- [ ] Let an owner record an off-platform (cash) rent payment, clearly marked as not wallet-settled
- [ ] A known upstream Spring Framework 7 issue (spring-framework#35287) can turn a method-level `@PreAuthorize` denial into a 500 instead of a 403. `GlobalExceptionHandler` now maps `AccessDeniedException` to 403, so it is covered here; worth re-checking once a fix ships
- [ ] Run on a MongoDB replica set and wrap each booking in a transaction, which closes the crash-mid-booking gap
- [ ] RabbitMQ queues are declared (`property.auth.queue`, `property.wallet.queue`, `property.booking.queue`) ahead of an event-driven flow that isn't wired up yet
- [ ] `listing-service` and `booking-service` Feign clients exist but their call sites (property to listing sync, unit availability checks) are not yet exercised end-to-end

## Contributing

This is a proprietary service for the Premisave platform. If you have been granted access to contribute:

1. Fork the repository and create a branch: `git checkout -b feature/my-improvement`
2. Make your change and keep the code style consistent.
3. Commit with a clear message and open a pull request describing what and why.

Found a bug or have an integration question? [Open an issue](https://github.com/peacemakerbill/premisave-property-management-service/issues).

## Author

<table>
  <tr>
    <td align="center" width="180">
      <a href="https://github.com/peacemakerbill">
        <img src="https://avatars.githubusercontent.com/u/262163756?v=4&amp;s=160" width="120" height="120" alt="Bill Graham Peacemaker, GitHub @peacemakerbill" /><br/>
        <sub><b>Bill Graham Peacemaker</b></sub>
      </a>
    </td>
    <td>
      <b>Backend Developer &amp; API Support Engineer at Safaricom PLC</b><br/>
      Nairobi, Kenya<br/><br/>
      Enterprise systems developer and API integration specialist, working across backend microservices (Java/Spring Boot), Flutter frontends, and DevOps — with deep, hands-on production and personal-project experience in Safaricom's M-Pesa Daraja APIs.<br/><br/>
      <a href="https://github.com/peacemakerbill"><img src="https://img.shields.io/badge/GitHub-@peacemakerbill-181717?style=for-the-badge&amp;logo=github" alt="GitHub @peacemakerbill" /></a>
      <a href="https://github.com/peacemakerbill?tab=followers"><img src="https://img.shields.io/github/followers/peacemakerbill?style=for-the-badge&amp;logo=github&amp;label=Followers" alt="GitHub followers" /></a>
      <br/><br/>
      More from me: <a href="https://github.com/peacemakerbill/premisave-wallet-service">premisave-wallet-service</a> ·
      <a href="https://github.com/peacemakerbill/premisave-c2b-hakikisha-service-m-pesa">premisave-c2b-hakikisha-service-m-pesa</a> ·
      <a href="https://github.com/peacemakerbill/premisave_auth_service">premisave_auth_service</a> ·
      <a href="https://github.com/peacemakerbill/premisave_flutter_frontend">premisave_flutter_frontend</a> ·
      <a href="https://github.com/peacemakerbill?tab=repositories">all repositories</a>
    </td>
  </tr>
</table>

### Star history

<a href="https://star-history.com/#peacemakerbill/premisave-property-management-service&amp;Date">
  <img src="https://api.star-history.com/svg?repos=peacemakerbill/premisave-property-management-service&amp;type=Date" alt="Star history chart for premisave-property-management-service" width="600" />
</a>

---

<details>
<summary>Search keywords</summary>

Spring Boot microservice · Java property management backend · lease management API Java · rent payment system · tenant management microservice · property management SaaS backend · wallet-funded payments Java · microservice-to-microservice payments · idempotent payment processing · service availability circuit breaker Spring · MongoDB Spring Boot microservice · Spring Data MongoDB · Redis caching Spring Boot · RabbitMQ Spring Boot · JWT authentication Spring Security 7 · Spring Boot 4 · Spring Framework 7 · OpenFeign service-to-service · security deposit management · utility billing from meter readings · maintenance work order system · property inspection scheduling · fintech backend Kenya · proptech Kenya · East Africa payments infrastructure · Premisave · Thymeleaf HTML email notifications

</details>

<p align="center">
  <sub>Made in Nairobi, Kenya · <a href="https://github.com/peacemakerbill">@peacemakerbill</a></sub>
</p>