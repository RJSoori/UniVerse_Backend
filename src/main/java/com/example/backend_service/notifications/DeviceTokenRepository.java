package com.example.backend_service.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByStudentId(Long studentId);
    Optional<DeviceToken> findByToken(String token);
    void deleteByToken(String token);
}
