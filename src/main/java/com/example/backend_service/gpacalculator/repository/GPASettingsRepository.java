package com.example.backend_service.gpacalculator.repository;

import com.example.backend_service.gpacalculator.model.GPASettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GPASettingsRepository extends JpaRepository<GPASettings, String> {
    Optional<GPASettings> findByStudentId(Long studentId);
}
