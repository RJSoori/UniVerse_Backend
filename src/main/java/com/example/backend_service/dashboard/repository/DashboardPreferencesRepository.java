package com.example.backend_service.dashboard.repository;

import com.example.backend_service.dashboard.model.DashboardPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardPreferencesRepository extends JpaRepository<DashboardPreferences, String> {
    Optional<DashboardPreferences> findByStudentId(Long studentId);
}
