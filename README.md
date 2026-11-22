# Gate Prep

A full-stack GATE Computer Science preparation platform with a Kotlin/Jetpack Compose Android client, Java Spring Boot API, PostgreSQL persistence, timed mock tests, and a Python PDF/OCR content pipeline.

## Current status

- Android practice and mock-test interfaces are implemented.
- Spring Boot authentication, content, progress, mock-session, grading, and image APIs run locally.
- The backend imports 2,108 practice questions and 325 mock questions.
- Android Login/Signup, protected navigation, post-login continuation, API-backed questions/progress, and authenticated S3 image delivery are implemented and verified locally.
- A private encrypted S3 bucket contains 5,010 question, solution, and figure images; the local uploader has restricted prefix-only permissions.
- AWS deployment is planned and has not yet been completed.

See [PROJECT_INTERVIEW_AND_RESUME_GUIDE.md](PROJECT_INTERVIEW_AND_RESUME_GUIDE.md) for the full architecture, verified feature list, roadmap, interview explanation, résumé bullets, testing plan, AWS cost estimate, and known limitations.

## Repository structure

```text
GAteApplication/
├── GateApp/          # Kotlin + Jetpack Compose Android client
├── gate-backend/     # Java 17 + Spring Boot + PostgreSQL API
├── PROJECT_INTERVIEW_AND_RESUME_GUIDE.md
└── README.md
```

The authoritative question/solution content is stored under `gate-backend/content`. Duplicate generated question and image assets inside the Android project are excluded from Git because the client is being migrated to authenticated API delivery.

## Main features

- 12 GATE CS subjects with module-wise practice.
- MCQ, MSQ, and numerical-answer questions.
- Native text/code rendering with original-image fallback for unsafe OCR output.
- Five 65-question, 100-mark, 180-minute mock tests.
- Server-controlled mock timer, autosave, review palette, server-side grading, results, and leaderboard API.
- BCrypt password hashing and short-lived JWT access tokens.
- Rotating refresh tokens stored as hashes with reuse detection.
- PostgreSQL/Flyway schema with monthly partitioning for attempt history.
- Protected S3 image delivery through short-lived pre-signed URLs.

## Planned AWS architecture

Android → Application Load Balancer → ECS Fargate (2–4 Spring Boot tasks) → RDS PostgreSQL / ElastiCache Valkey / private S3, with CloudWatch monitoring and Terraform provisioning.

## Run the backend locally

Java 17 is required. The `dev` profile starts an embedded PostgreSQL database, so a local PostgreSQL installation is not required.

```powershell
Set-Location gate-backend
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Run backend tests:

```powershell
Set-Location gate-backend
.\gradlew.bat test
```

## Build the Android app

The debug build reaches a backend on the development computer through `http://10.0.2.2:8080` from the Android emulator.

```powershell
Set-Location GateApp
.\gradlew.bat assembleDebug
```

The release API URL in `GateApp/app/build.gradle.kts` is still a placeholder and must be replaced with the deployed HTTPS domain.

## Secrets

Copy `gate-backend/.env.example` to `gate-backend/.env` for local configuration. Never commit `.env`, JWT secrets, database passwords, or AWS keys.

For ECS production, use an IAM task role instead of permanent AWS keys and load the JWT/database secrets from AWS Secrets Manager or SSM Parameter Store.

## Content notice

The repository contains extracted educational content from official GATE papers and third-party preparation materials. Verify redistribution rights before publishing the content or distributing the application publicly.
