# Contributing

Thank you for your interest in contributing to Railway Order Management!

## Prerequisites

- Java 21
- Maven 3.9+ (or use `./mvnw`)
- Docker & Docker Compose
- Keycloak knowledge (for auth changes)

## Getting Started

```bash
# Clone the repository
git clone https://github.com/kron12345/railway-order-mgmt.git
cd railway-order-mgmt

# Copy environment template
cp .env.example .env

# Start infrastructure
docker compose -f docker/docker-compose.yml up -d

# Run the application
./mvnw spring-boot:run
```

## Development Workflow

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make your changes
3. Format code: `./mvnw spotless:apply`
4. Run tests: `./mvnw verify`
5. Commit with a clear message (imperative mood)
6. Push and create a Pull Request

## Code Quality Checks

All of these run in CI and must pass before merge:

| Check | Command | What it does |
|---|---|---|
| Compile | `./mvnw clean compile` | Java + Vaadin frontend build |
| Spotless | `./mvnw spotless:check` | Google Java Style formatting |
| Unit Tests | `./mvnw test` | JUnit 5 + ArchUnit |
| SpotBugs | `./mvnw spotbugs:check` | Static bug analysis |
| Coverage | `./mvnw jacoco:report` | Line coverage (min 60%) |

## Conventions

- See [docs/CONVENTIONS.md](docs/CONVENTIONS.md) for coding standards
- See [docs/GLOSSARY.md](docs/GLOSSARY.md) for domain terminology
- All user-visible text must use `getTranslation()` (i18n)
- All entities must have `@Audited` (Hibernate Envers)
- No hardcoded values — use `.env` or `application.yaml`
- Max 300-500 lines per file

## Architecture

- See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- DDD package structure: `domain/{context}/{model,repository,service,event}`
- Architecture rules enforced via ArchUnit tests

## Adding a New Entity

1. Create model class in `domain/{context}/model/` with `@Audited`
2. Create repository interface in `domain/{context}/repository/`
3. Add Flyway migration in `src/main/resources/db/migration/`
4. Add translation keys to all 4 language files
5. Update `docs/PROJECT_STATUS.md`

## Pull Request Checklist

See the PR template for the full checklist. Key points:
- Code compiles and tests pass
- Spotless formatting applied
- Translations in all 4 languages
- PROJECT_STATUS.md updated
