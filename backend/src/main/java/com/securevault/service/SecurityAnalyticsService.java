package com.securevault.service;

import com.securevault.entity.SecurityEvent;
import com.securevault.repository.SecurityEventRepository;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecurityAnalyticsService {

    private final SecurityEventRepository securityEventRepository;

    public SecurityAnalyticsService(
            SecurityEventRepository securityEventRepository) {

        this.securityEventRepository =
                securityEventRepository;
    }

    public Map<String, Object> getAnalytics(String email) {

        List<SecurityEvent> events =
                securityEventRepository.findByEmail(email);

        long successfulLogins = events.stream()
                .filter(event ->
                        "SUCCESS".equals(event.getStatus()))
                .count();

        long failedLogins = events.stream()
                .filter(event ->
                        "FAILED".equals(event.getStatus()))
                .count();

        long credentialsAdded = events.stream()
                .filter(event ->
                        "CREDENTIAL_ADDED"
                                .equals(event.getStatus()))
                .count();

        long credentialsUpdated = events.stream()
                .filter(event ->
                        "CREDENTIAL_UPDATED"
                                .equals(event.getStatus()))
                .count();

        long credentialsDeleted = events.stream()
                .filter(event ->
                        "CREDENTIAL_DELETED"
                                .equals(event.getStatus()))
                .count();

        Map<String, Object> analytics =
                new HashMap<>();

        analytics.put(
                "successfulLogins",
                successfulLogins);

        analytics.put(
                "failedLogins",
                failedLogins);

        analytics.put(
                "credentialsAdded",
                credentialsAdded);

        analytics.put(
                "credentialsUpdated",
                credentialsUpdated);

        analytics.put(
                "credentialsDeleted",
                credentialsDeleted);

        analytics.put(
                "totalEvents",
                events.size());

        return analytics;
    }
}