package com.sentinelapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SentinelAPI — Secure API Monitoring & Uptime Platform
 *
 * This is the entry point of the application.
 *
 * @SpringBootApplication combines three annotations:
 * 1. @Configuration      — Marks this class as a source of bean definitions
 * 2. @EnableAutoConfiguration — Tells Spring Boot to auto-configure beans based on
 *                               classpath dependencies (e.g., DataSource for PostgreSQL)
 * 3. @ComponentScan      — Scans com.sentinelapi and all sub-packages for
 *                          @Service, @Repository, @Controller, @Component classes
 */
@SpringBootApplication
public class SentinelApiApplication {

    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the application:
        // 1. Creates the Spring ApplicationContext (the IoC container)
        // 2. Performs auto-configuration
        // 3. Starts the embedded Tomcat server
        // 4. Runs Flyway migrations
        // 5. Makes the app ready to serve HTTP requests
        SpringApplication.run(SentinelApiApplication.class, args);
    }
}
