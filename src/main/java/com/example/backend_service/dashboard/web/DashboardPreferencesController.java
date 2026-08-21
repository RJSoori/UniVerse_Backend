package com.example.backend_service.dashboard.web;

import com.example.backend_service.dashboard.model.DashboardPreferences;
import com.example.backend_service.dashboard.repository.DashboardPreferencesRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/dashboard")
public class DashboardPreferencesController {

    private final DashboardPreferencesRepository preferencesRepository;

    public DashboardPreferencesController(DashboardPreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
    }

    @GetMapping("/preferences")
    public DashboardPreferences getPreferences(@AuthenticationPrincipal Long authStudentId) {
        return preferencesRepository.findByStudentId(authStudentId)
                .orElseGet(() -> {
                    DashboardPreferences preferences = new DashboardPreferences(null);
                    preferences.setStudentId(authStudentId);
                    return preferencesRepository.save(preferences);
                });
    }

    @PutMapping("/preferences")
    public DashboardPreferences savePreferences(@AuthenticationPrincipal Long authStudentId,
            @Valid @RequestBody DashboardPreferences incoming) {
        DashboardPreferences preferences = preferencesRepository.findByStudentId(authStudentId)
                .orElseGet(() -> {
                    DashboardPreferences fresh = new DashboardPreferences(null);
                    fresh.setStudentId(authStudentId);
                    return fresh;
                });
        preferences.setStudentId(authStudentId);
        preferences.setLayoutJson(incoming.getLayoutJson());
        return preferencesRepository.save(preferences);
    }
}
