# ADR-001: Use Spring Security OAuth2 Client for Keycloak Integration

## Status
Accepted

## Context
We need to integrate Keycloak for user authentication and authorization. Two approaches exist:
1. Keycloak Spring Boot Adapter (deprecated)
2. Standard Spring Security OAuth2 Client

## Decision
Use **Spring Security OAuth2 Client** (`spring-boot-starter-oauth2-client`).

## Rationale
- Keycloak Spring Boot/Security adapters are deprecated since Keycloak 26 and incompatible with Spring Boot 3.x / Jakarta EE
- Spring Security's OIDC support is the officially recommended approach by both Keycloak and Spring teams
- Provider-agnostic: switching from Keycloak to another OIDC provider only requires YAML changes
- Role mapping via custom `GrantedAuthoritiesMapper` is straightforward

## Consequences
- Need custom `GrantedAuthoritiesMapper` to map Keycloak realm/client roles (~30 LOC)
- Configuration via `spring.security.oauth2.client.*` properties in application.yaml
