package com.ordermgmt.railway.infrastructure.keycloak;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads and writes user attributes from Keycloak via the Admin REST API. Preferences (theme,
 * locale, timezone, etc.) are stored as Keycloak user attributes.
 */
@Service
public class KeycloakUserService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserService.class);
    private static final String ADMIN_REALMS_PATH = "/admin/realms/";
    private static final String USERS_COLLECTION_PATH = "/users";
    private static final String USER_PATH_PREFIX = "/users/";
    private static final String GROUPS_PATH = "/groups";
    private static final String MASTER_TOKEN_PATH = "/realms/master/protocol/openid-connect/token";
    private static final int SEARCH_LIMIT = 50;

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
            String url = userUrl(keycloakUserId);

            ResponseEntity<JsonNode> response =
                    rest.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(bearerHeaders(token)),
                            JsonNode.class);

            Map<String, String> userAttributes = new LinkedHashMap<>();
            JsonNode body = response.getBody();
            if (body != null) {
                userAttributes.put("username", body.path("username").asText(""));
                userAttributes.put("email", body.path("email").asText(""));
                userAttributes.put("firstName", body.path("firstName").asText(""));
                userAttributes.put("lastName", body.path("lastName").asText(""));

                JsonNode attributes = body.path("attributes");
                if (attributes.isObject()) {
                    attributes
                            .fields()
                            .forEachRemaining(
                                    attribute -> {
                                        JsonNode values = attribute.getValue();
                                        if (values.isArray() && !values.isEmpty()) {
                                            userAttributes.put(
                                                    attribute.getKey(), values.get(0).asText());
                                        }
                                    });
                }
            }
            return userAttributes;
        } catch (Exception e) {
            log.error("Failed to get user attributes from Keycloak", e);
            return Collections.emptyMap();
        }
    }

    /** Get user's realm roles from Keycloak. */
    public List<String> getUserRoles(String keycloakUserId) {
        try {
            String token = getAdminToken();
            String url = userUrl(keycloakUserId) + "/role-mappings/realm";

            ResponseEntity<JsonNode> response =
                    rest.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(bearerHeaders(token)),
                            JsonNode.class);

            List<String> roles = new ArrayList<>();
            if (response.getBody() != null && response.getBody().isArray()) {
                response.getBody().forEach(role -> roles.add(role.path("name").asText()));
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
            String url = userUrl(keycloakUserId);

            Map<String, List<String>> keycloakAttributes = new LinkedHashMap<>();
            attributes.forEach((name, value) -> keycloakAttributes.put(name, List.of(value)));

            Map<String, Object> body = Map.of("attributes", keycloakAttributes);

            HttpHeaders headers = bearerHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            rest.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers),
                    Void.class);
            return true;
        } catch (Exception e) {
            log.error("Failed to update user attributes in Keycloak", e);
            return false;
        }
    }

    /** Search users in Keycloak by username or email. */
    public List<Map<String, String>> searchUsers(String query) {
        try {
            String token = getAdminToken();
            String encodedQuery = encodeQuery(query);
            String url =
                    keycloakUrl
                            + ADMIN_REALMS_PATH
                            + realm
                            + USERS_COLLECTION_PATH
                            + "?"
                            + "firstNameOrLastName=true"
                            + "&email="
                            + encodedQuery
                            + "&username="
                            + encodedQuery
                            + "&max="
                            + SEARCH_LIMIT;

            ResponseEntity<JsonNode[]> response =
                    rest.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(bearerHeaders(token)),
                            JsonNode[].class);

            List<Map<String, String>> users = new ArrayList<>();
            if (response.getBody() != null) {
                for (JsonNode user : response.getBody()) {
                    users.add(toUserSearchResult(user));
                }
            }
            return users;
        } catch (Exception e) {
            log.error("Failed to search users in Keycloak", e);
            return Collections.emptyList();
        }
    }

    /** Search groups in Keycloak by name. */
    public List<Map<String, String>> searchGroups(String query) {
        try {
            String token = getAdminToken();
            String encodedQuery = encodeQuery(query);
            String url =
                    keycloakUrl
                            + ADMIN_REALMS_PATH
                            + realm
                            + GROUPS_PATH
                            + "?"
                            + "search="
                            + encodedQuery
                            + "&max="
                            + SEARCH_LIMIT;

            ResponseEntity<JsonNode[]> response =
                    rest.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(bearerHeaders(token)),
                            JsonNode[].class);

            List<Map<String, String>> groups = new ArrayList<>();
            if (response.getBody() != null) {
                for (JsonNode group : response.getBody()) {
                    groups.add(toGroupSearchResult(group));
                }
            }
            return groups;
        } catch (Exception e) {
            log.error("Failed to search groups in Keycloak", e);
            return Collections.emptyList();
        }
    }

    private String getAdminToken() {
        String tokenUrl = keycloakUrl + MASTER_TOKEN_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("client_id", adminClientId);
        params.add("username", adminUsername);
        params.add("password", adminPassword);
        params.add("grant_type", "password");

        ResponseEntity<JsonNode> response =
                rest.exchange(
                        tokenUrl,
                        HttpMethod.POST,
                        new HttpEntity<>(params, headers),
                        JsonNode.class);

        return response.getBody().path("access_token").asText();
    }

    private String userUrl(String keycloakUserId) {
        return keycloakUrl + ADMIN_REALMS_PATH + realm + USER_PATH_PREFIX + keycloakUserId;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String encodeQuery(String query) {
        return URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
    }

    private Map<String, String> toUserSearchResult(JsonNode user) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", user.path("id").asText());
        result.put("username", user.path("username").asText(""));
        result.put("email", user.path("email").asText(""));
        result.put("firstName", user.path("firstName").asText(""));
        result.put("lastName", user.path("lastName").asText(""));
        return result;
    }

    private Map<String, String> toGroupSearchResult(JsonNode group) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", group.path("id").asText());
        result.put("name", group.path("name").asText(""));
        result.put("path", group.path("path").asText(""));
        return result;
    }
}
