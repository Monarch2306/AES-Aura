package com.aes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures newer User columns exist on databases created from an older schema.
 * Hibernate will not add NOT NULL columns to tables that already have rows.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaCompatibilityPatcher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityPatcher.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityPatcher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'ACTIVE'");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        jdbcTemplate.update("UPDATE users SET status = 'ACTIVE' WHERE status IS NULL");
        jdbcTemplate.update("UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL");
        log.info("Verified users.status and users.updated_at schema compatibility");
    }
}
