/**
 * <strong>Infrastructure layer</strong> — cross-cutting technical concerns
 * orthogonal to the domain.
 *
 * <p>Sub-packages:
 *
 * <ul>
 *   <li>{@code security} — Spring Security OAuth2 OIDC integration with Keycloak
 *       (SecurityConfig, SecurityAuditorAware). API and UI use separate filter
 *       chains; UI mutations are gated by {@code @PreAuthorize} at the service
 *       layer (see ADR-001).
 *   <li>{@code keycloak} — admin-API bridge for user/group lookup and attribute
 *       updates (KeycloakUserService).
 *   <li>{@code i18n} — TranslationProvider for de / en / it / fr bundles.
 *   <li>{@code push} — BroadcastService for WebSocket fan-out.
 *   <li>{@code config} — Spring config beans (audit, RestTemplate, etc.).
 * </ul>
 *
 * <p>Architecture rule: domain and ui never import from individual
 * {@code infrastructure.*} sub-packages directly — they depend on
 * Spring-injected interfaces / beans.
 */
package com.ordermgmt.railway.infrastructure;
