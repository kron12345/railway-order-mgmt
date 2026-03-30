package com.ordermgmt.railway.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables JPA auditing across the application. */
@Configuration
@EnableJpaAuditing
public class AuditConfig {}
