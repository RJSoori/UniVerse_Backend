package com.example.backend_service.notifications;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class NotificationController {

    private final DeviceTokenRepository deviceTokenRepository;

    public NotificationController(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostMapping("/api/notifications/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDeviceToken(@AuthenticationPrincipal Long authStudentId, @RequestBody DeviceTokenRequest request) {
        DeviceToken deviceToken = deviceTokenRepository.findByToken(request.token()).orElseGet(DeviceToken::new);
        deviceToken.setToken(request.token());
        deviceToken.setStudentId(authStudentId);
        deviceToken.setPlatform(request.platform());
        if (deviceToken.getCreatedAt() == null) {
            deviceToken.setCreatedAt(LocalDateTime.now());
        }
        deviceTokenRepository.save(deviceToken);
    }

    @DeleteMapping("/api/notifications/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDeviceToken(@RequestBody DeviceTokenRequest request) {
        deviceTokenRepository.deleteByToken(request.token());
    }

    public record DeviceTokenRequest(@NotBlank String token, String platform) {
    }
}
