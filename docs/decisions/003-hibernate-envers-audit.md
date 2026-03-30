# ADR-003: Use Hibernate Envers for Audit Trail

## Status
Accepted

## Context
Railway order management requires full traceability of all data changes for compliance and operational auditing.

## Decision
Use **Hibernate Envers** for entity-level audit trailing.

## Rationale
- Automatic versioning of `@Audited` entities with zero business logic changes
- Shadow tables (`_audit`) store every historical state
- `RevisionRepository` provides query API for revision history
- Integrates natively with JPA/Hibernate — no additional infrastructure needed
- Sufficient for entity-level audit (who changed what, when)

## Consequences
- Every `@Audited` entity gets a corresponding `_audit` table
- `revinfo` table tracks revision metadata (timestamp)
- Flyway migrations must include audit table DDL
- Slight storage overhead for audit tables
