package com.ordermgmt.railway.infrastructure.keycloak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads and writes user attributes from Keycloak via the Admin REST API.
 * Preferences (theme, locale, timezone, etc.) are stored as Keycloak user attributes.
 */
@Service
public class KeycloakUserService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserService.class);
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${keycloak.admin.url:https://sso.animeland.de}")
    private String keycloakUrl;

    @Value("${keycloak.admin.realm:railway}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:}")
    private String adminPassword;

    /** Get user attributes from Keycloak. */
    public Map<String, String> getUserAttributes(String keycloakUserId) {
        try {
            String token = getAdminToken();
            String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<JsonNode> resp = rest.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

            Map<String, String> result = new LinkedHashMap<>();
            JsonNode body = resp.getBody();
            if (body != null) {
                result.put("username", body.path("username").asText(""));
                result.put("email", body.path("email").asText(""));
                result.put("firstName", body.path("firstName").asText(""));
                result.put("lastName", body.path("lastName").asText(""));

                JsonNode attrs = body.path("attributes");
                if (attrs.isObject()) {
                    attrs.fields().forEachRemaining(e -> {
                        JsonNode val = e.getValue();
                        if (val.isArray() && !val.isEmpty()) {
                            result.put(e.getKey(), val.get(0).asText());
                        }
                    });
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get user attributes from Keycloak", e);
            return Collections.emptyMap();
        }
    }

    /** Get user's realm roles from Keycloak. */
    public List<String> getUserRoles(String keycloakUserId) {
        try {
            String token = getAdminToken();
            String url = keycloakUrl + "/admin/realms/" + realm
                    + "/users/" + keycloakUserId + "/role-mappings/realm";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<JsonNode> resp = rest.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

            List<String> roles = new java.util.ArrayList<>();
            if (resp.getBody() != null && resp.getBody().isArray()) {
                resp.getBody().forEach(r -> roles.add(r.path("name").asText()));
            }
            return roles;
        } catch (Exception e) {
            log.error("Failed to get user roles from Keycloak", e);
            return Collections.emptyList();
        }
    }

    /** Update user attributes in Keycloak. */
    public boolean updateUserAttributes(String keycloakUserId, Map<String, String> attributes) {
        try {
            String token = getAdminToken();
            String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;

            // Convert to Keycloak format: {"attributes": {"key": ["value"]}}
            Map<String, List<String>> kcAttrs = new LinkedHashMap<>();
            attributes.forEach((k, v) -> kcAttrs.put(k, List.of(v)));

            Map<String, Object> body = Map.of("attributes", kcAttrs);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            rest.exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers), Void.class);
            return true;
        } catch (Exception e) {
            log.error("Failed to update user attributes in Keycloak", e);
            return false;
        }
    }

    private String getAdminToken() {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("client_id", adminClientId);
        params.add("username", adminUsername);
        params.add("password", adminPassword);
        params.add("grant_type", "password");

        ResponseEntity<JsonNode> resp = rest.exchange(
                tokenUrl, HttpMethod.POST,
                new HttpEntity<>(params, headers), JsonNode.class);

        return resp.getBody().path("access_token").asText();
    }
}
