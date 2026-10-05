package com.securevault.controller;

import com.securevault.entity.SecurityEvent;
import com.securevault.service.SecurityAnalyticsService;
import com.securevault.service.SecurityEventService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
public class SecurityAnalyticsController {

    private final SecurityAnalyticsService securityAnalyticsService;
    private final SecurityEventService securityEventService;

    public SecurityAnalyticsController(
            SecurityAnalyticsService securityAnalyticsService,
            SecurityEventService securityEventService) {

        this.securityAnalyticsService =
                securityAnalyticsService;

        this.securityEventService =
                securityEventService;
    }

    // =========================
    // SECURITY ANALYTICS
    // =========================

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        Map<String, Object> analytics =
                securityAnalyticsService
                        .getAnalytics(email);

        return ResponseEntity.ok(analytics);
    }

    // =========================
    // LOGIN ANOMALIES
    // =========================

    @GetMapping("/anomalies")
    public ResponseEntity<?> getAnomalies() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        List<SecurityEvent> anomalies =
                securityEventService
                        .getAnomalies(email);

        return ResponseEntity.ok(anomalies);
    }

    // =========================
    // SECURITY EVENTS EXPORT
    // =========================

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSecurityEvents() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        List<SecurityEvent> events =
                securityEventService
                        .getLoginHistory(email);

        StringBuilder csv = new StringBuilder();

        csv.append(
                "ID,Email,Date & Time,Status,IP Address,Device\n"
        );

        for (SecurityEvent event : events) {

            csv.append(event.getId()).append(",");

            csv.append(
                    escapeCsv(event.getEmail())
            ).append(",");

            csv.append(
                    escapeCsv(
                            event.getDateTime() != null
                                    ? event.getDateTime().toString()
                                    : ""
                    )
            ).append(",");

            csv.append(
                    escapeCsv(event.getStatus())
            ).append(",");

            csv.append(
                    escapeCsv(event.getIpAddress())
            ).append(",");

            csv.append(
                    escapeCsv(event.getDevice())
            ).append("\n");
        }

        byte[] csvBytes =
                csv.toString()
                        .getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType("text/csv")
        );

        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=securevault-security-events.csv"
        );

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(csvBytes);
    }

    // =========================
    // CSV ESCAPE
    // =========================

    private String escapeCsv(String value) {

        if (value == null) {
            return "";
        }

        String escaped =
                value.replace("\"", "\"\"");

        if (escaped.contains(",")
                || escaped.contains("\"")
                || escaped.contains("\n")) {

            return "\"" + escaped + "\"";
        }

        return escaped;
    }
}