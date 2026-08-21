package com.example.backend_service.skills;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentSkillProfileRepository extends JpaRepository<StudentSkillProfile, Long> {
    Optional<StudentSkillProfile> findByStudentId(Long studentId);
}
