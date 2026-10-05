package com.securevault.service;

import com.securevault.entity.SecurityEvent;
import com.securevault.repository.SecurityEventRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    public SecurityEventService(
            SecurityEventRepository securityEventRepository) {

        this.securityEventRepository = securityEventRepository;
    }

    // =========================
    // RECORD LOGIN
    // =========================

    public void recordLogin(
            String email,
            String status,
            String ipAddress,
            String device) {

        SecurityEvent event = new SecurityEvent();

        event.setEmail(email);
        event.setDateTime(LocalDateTime.now());
        event.setStatus(status);
        event.setIpAddress(ipAddress);
        event.setDevice(device);

        securityEventRepository.save(event);
    }

    // =========================
    // RECORD GENERAL EVENT
    // =========================

    public void recordEvent(
            String email,
            String status,
            String ipAddress,
            String device) {

        SecurityEvent event = new SecurityEvent();

        event.setEmail(email);
        event.setDateTime(LocalDateTime.now());
        event.setStatus(status);
        event.setIpAddress(ipAddress);
        event.setDevice(device);

        securityEventRepository.save(event);
    }

    // =========================
    // LOGIN ANOMALY DETECTION
    // =========================

    public String analyzeLogin(
            String email,
            String ipAddress,
            String device) {

        List<SecurityEvent> history =
                securityEventRepository.findByEmail(email);

        // First login
        if (history.isEmpty()) {

            return "LOW";
        }

        boolean newIp = true;
        boolean newDevice = true;

        int failedAttempts = 0;

        LocalDateTime now =
                LocalDateTime.now();

        // Check previous login history
        for (SecurityEvent event : history) {

            // Check previous IP
            if (event.getIpAddress() != null &&
                    event.getIpAddress().equals(ipAddress)) {

                newIp = false;
            }

            // Check previous device
            if (event.getDevice() != null &&
                    event.getDevice().equals(device)) {

                newDevice = false;
            }

            // Count recent failed logins
            if ("FAILED".equalsIgnoreCase(
                    event.getStatus())) {

                if (event.getDateTime() != null &&
                        event.getDateTime()
                                .isAfter(
                                        now.minusMinutes(15)
                                )) {

                    failedAttempts++;
                }
            }
        }

        int riskScore = 0;

        // New IP
        if (newIp) {
            riskScore += 30;
        }

        // New device
        if (newDevice) {
            riskScore += 30;
        }

        // Failed login attempts
        if (failedAttempts >= 3) {
            riskScore += 40;
        } else if (failedAttempts >= 1) {
            riskScore += 20;
        }

        // Risk level
        if (riskScore >= 70) {
            return "HIGH";
        }

        if (riskScore >= 30) {
            return "MEDIUM";
        }

        return "LOW";
    }

    // =========================
    // RECORD ANOMALY
    // =========================

    public void recordLoginAnomaly(
            String email,
            String riskLevel,
            String ipAddress,
            String device) {

        String status =
                "ANOMALY_" + riskLevel;

        recordEvent(
                email,
                status,
                ipAddress,
                device
        );
    }

    // =========================
    // LOGIN HISTORY
    // =========================

    public List<SecurityEvent> getLoginHistory(
            String email) {

        return securityEventRepository.findByEmail(email);
    }

public List<SecurityEvent> getAnomalies(String email) {
    List<SecurityEvent> history = securityEventRepository.findByEmail(email);

    return history.stream()
            .filter(event -> event.getStatus() != null
                    && event.getStatus().startsWith("ANOMALY_"))
            .toList();
}
}