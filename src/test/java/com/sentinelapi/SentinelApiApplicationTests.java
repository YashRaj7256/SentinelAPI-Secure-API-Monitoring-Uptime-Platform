package com.sentinelapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies that the Spring ApplicationContext loads successfully.
 *
 * @SpringBootTest loads the FULL application context (all beans, all auto-configuration).
 * If any bean fails to initialize (bad config, missing dependency, etc.), this test fails.
 *
 * This is the most basic integration test — if this passes, your app can start.
 */
@SpringBootTest
class SentinelApiApplicationTests {

    @Test
    void contextLoads() {
        // This test intentionally has no assertions.
        // The test passes if the Spring context loads without throwing an exception.
        // If it fails, it means something is misconfigured (database, beans, etc.).
    }
}
