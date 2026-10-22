package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public health-check endpoint, meant for uptime monitors (e.g.
 * UptimeRobot) pinging the deployed backend on a free-tier Render
 * service to prevent it from spinning down due to inactivity, and for
 * basic liveness checks (load balancers, manual "is it up" checks).
 *
 * <p>Deliberately does not touch the database or any other dependency —
 * it only confirms the application itself is running and able to serve
 * HTTP requests. This keeps the check fast and cheap enough to be
 * pinged every few minutes without any real load, and avoids false
 * "down" reports caused by a transient DB hiccup that isn't actually a
 * backend outage.
 *
 * <p>Public per {@code SecurityConfig} ({@code PUBLIC_GET_ENDPOINTS}) —
 * no authentication required, since uptime monitors can't supply a JWT.
 */
@RestController
public class HealthController {

    @GetMapping(ApiPaths.Health.CHECK)
    public ResponseEntity<Map<String, Object>> check() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}
