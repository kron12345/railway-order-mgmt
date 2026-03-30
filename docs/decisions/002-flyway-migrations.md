# ADR-002: Use Flyway for Database Migrations

## Status
Accepted

## Context
Need a database migration tool. Main options: Flyway or Liquibase.

## Decision
Use **Flyway** with plain SQL migrations.

## Rationale
- Single database target (PostgreSQL) — Liquibase's database-agnostic abstraction adds complexity without benefit
- Plain SQL migrations are transparent: what you write is what runs
- Simpler to review in PRs
- First-class Spring Boot auto-configuration
- More widely adopted in the Spring Boot ecosystem

## Consequences
- Migrations in `src/main/resources/db/migration/V{n}__{desc}.sql`
- No automatic rollback in Community edition — use roll-forward strategy
- Schema changes must be written as raw PostgreSQL DDL
