package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.MoneyManagerSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MoneyManagerSettingsRepository extends JpaRepository<MoneyManagerSettings, String> {
    Optional<MoneyManagerSettings> findByStudentId(Long studentId);
    void deleteByStudentId(Long studentId);
}
