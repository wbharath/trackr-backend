# Trackr — Backend

REST API for Trackr, an AI-powered job application tracker. Built with Spring Boot 4, handles authentication, job management, Gmail sync, and AI-powered email categorization.

---

## Features

- **JWT Authentication** — Register, login, and secure all endpoints with JSON Web Tokens
- **Job CRUD** — Create, read, update, and delete job applications with filtering, search, and pagination
- **Gmail Sync** — Fetches emails via Gmail API and sends them in batches to Claude AI for categorization
- **AI Extraction** — Uses Claude Haiku to extract job details (position, company, location) from any job page text
- **Stats & Analytics** — Returns application counts by status and monthly application trends

---

## Tech Stack

- Java 25
- Spring Boot 4.0.1
- Spring Security + JWT (jjwt 0.11.5)
- Spring Data JPA + Hibernate 7
- MySQL 8
- Anthropic Claude API (Haiku for extraction, Sonnet for Gmail categorization)
- Gmail REST API
- Lombok
- Maven

---

## Project Structure

```
src/main/java/com/example/jobster_backend/
├── config/
│   ├── AppConfig.java          # RestTemplate and ObjectMapper beans
│   └── SecurityConfig.java     # JWT filter chain, CORS config
├── controller/
│   ├── AuthController.java     # /api/v1/auth — register, login
│   ├── JobController.java      # /api/v1/jobs — CRUD, extract, stats
│   └── GmailController.java    # /api/v1/gmail — sync
├── dto/
│   ├── JobDto.java
│   ├── GmailSyncRequest.java
│   ├── GmailSyncResponse.java
│   ├── StatsResponseDto.java
│   └── MonthlyApplicationDto.java
├── entity/
│   ├── Job.java                # Job entity with JobStatus enum
│   └── User.java               # User entity with Gmail fields
├── repository/
│   ├── JobRepository.java
│   └── UserRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
├── service/
│   ├── JobService.java
│   ├── AnthropicService.java   # AI extraction via Claude Haiku
│   ├── GmailSyncService.java   # Gmail fetch + Claude categorization
│   └── impl/
│       └── JobServiceImpl.java
└── utils/
    └── JwtUtil.java
```

---

## API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login and get JWT |

### Jobs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/jobs` | Get all jobs (search, filter, sort, paginate) |
| POST | `/api/v1/jobs` | Create job |
| PATCH | `/api/v1/jobs/:id` | Update job |
| DELETE | `/api/v1/jobs/:id` | Delete job |
| GET | `/api/v1/jobs/stats` | Get stats and monthly applications |
| POST | `/api/v1/jobs/extract` | Extract job details from page text using AI |

### Gmail
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/gmail/sync` | Sync Gmail inbox and categorize job emails |

---

## Setup

### Prerequisites
- Java 25
- MySQL 8
- Anthropic API key
- Google Cloud project with Gmail API enabled

### 1. Clone the repo
```bash
git clone https://github.com/wbharath/trackr-backend.git
cd trackr-backend
```

### 2. Create MySQL database
```sql
CREATE DATABASE jobster;
```

### 3. Configure application.properties
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/jobster
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

### 4. Add secrets file
Create `src/main/resources/application-secret.properties` (gitignored):
```properties
anthropic.api.key=your_anthropic_api_key
jwt.secret=your_jwt_secret_key
```

### 5. Run
```bash
mvn spring-boot:run
```

Server starts on `http://localhost:8080`

---

## Environment Variables for Deployment

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | MySQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `ANTHROPIC_API_KEY` | Anthropic API key |
| `JWT_SECRET` | JWT signing secret |

---

## Key Design Decisions

- **Gmail sync deduplication** — Each email is stored with its Gmail message ID. On subsequent syncs, already-saved emails are skipped using `existsByGmailMessageId`
- **Batch categorization** — Emails are sent to Claude in batches of 10 to minimize API calls
- **Status enum** — `APPLIED`, `INTERVIEW`, `REJECTED`, `OFFER` stored as strings in MySQL
- **First sync window** — On first sync, emails from the past 24 hours are fetched. Subsequent syncs fetch only new emails since last sync date

---

## Related Repositories

- [trackr-extension](https://github.com/wbharath/Trackr-Extension) — Chrome MV3 extension
- [trackr-frontend](https://github.com/wbharath/trackr-frontend) — React dashboard
