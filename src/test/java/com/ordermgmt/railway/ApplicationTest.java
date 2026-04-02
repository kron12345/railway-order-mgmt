package com.ordermgmt.railway;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires running Keycloak and PostgreSQL infrastructure")
class ApplicationTest {

    @Test
    void contextLoads() {}
}
