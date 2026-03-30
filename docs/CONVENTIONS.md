# Coding Conventions

## Java
- Java 21, use records for DTOs where immutability fits
- Entities: `@Getter @Setter @NoArgsConstructor` (Lombok), UUID primary keys
- Services: `@Service` annotation, constructor injection
- No field injection (`@Autowired` on fields) — always constructor injection

## Naming
- Entities: singular (`Order`, `Customer`)
- Tables: plural (`orders`, `customers`)
- Repositories: `{Entity}Repository`
- Services: `{Entity}Service`
- Views: `{Entity}{Action}View` (e.g., `OrderListView`, `OrderDetailView`)
- DTOs: `{Entity}Dto` or `{Entity}Response`/`{Entity}Request`

## i18n Keys
- Pattern: `{module}.{context}.{name}`
- Examples: `order.status.DRAFT`, `nav.orders`, `common.save`
- All user-visible text MUST use `getTranslation()`, never hardcoded strings

## Database
- Flyway migrations: `V{version}__{description}.sql`
- Always include `CREATE INDEX` for foreign keys and frequently queried columns
- Use `TIMESTAMPTZ` for all timestamps

## Testing
- Unit tests: `{Class}Test.java`
- Integration tests: `{Feature}IntegrationTest.java` in `integration/` package
- Use Testcontainers for database tests
- Use Karibu Testing for Vaadin UI tests

## Git
- Branch naming: `feature/{ticket}-{short-description}`, `fix/{ticket}-{short-description}`
- Commit messages: imperative mood, max 72 chars first line
