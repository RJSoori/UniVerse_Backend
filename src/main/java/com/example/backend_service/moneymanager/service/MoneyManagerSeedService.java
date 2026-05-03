package com.example.backend_service.moneymanager.service;

import com.example.backend_service.moneymanager.model.MoneyManagerSettings;
import com.example.backend_service.moneymanager.repository.MoneyManagerSettingsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class MoneyManagerSeedService implements CommandLineRunner {

    private final MoneyManagerSettingsRepository settingsRepository;

    public MoneyManagerSeedService(MoneyManagerSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public void run(String... args) {
        // Per-student settings are created on first use in MoneyManagerController.
    }
}
