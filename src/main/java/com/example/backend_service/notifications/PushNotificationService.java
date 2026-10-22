package com.example.backend_service.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends push notifications via the Expo push API (https://exp.host) — no
 * Firebase/APNs credentials are required for this; Expo brokers delivery to
 * FCM/APNs using its own shared credentials for development, or the project's
 * own credentials once configured via EAS for production builds.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final DeviceTokenRepository deviceTokenRepository;
    private final RestClient restClient = RestClient.create();

    public PushNotificationService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    public void sendToStudent(Long studentId, String title, String body) {
        List<DeviceToken> tokens = deviceTokenRepository.findByStudentId(studentId);
        if (tokens.isEmpty()) {
            return;
        }
        for (DeviceToken deviceToken : tokens) {
            sendOne(deviceToken.getToken(), title, body);
        }
    }

    private void sendOne(String expoPushToken, String title, String body) {
        try {
            restClient.post()
                    .uri(EXPO_PUSH_URL)
                    .body(Map.of("to", expoPushToken, "title", title, "body", body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Best-effort: a failed push should never break the calling request (e.g. a transaction save).
            log.warn("Failed to send push notification to token={}: {}", expoPushToken, e.getMessage());
        }
    }
}
