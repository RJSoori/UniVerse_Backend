package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.MoneyManagerSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoneyManagerSettingsRepository extends JpaRepository<MoneyManagerSettings, String> {
}
