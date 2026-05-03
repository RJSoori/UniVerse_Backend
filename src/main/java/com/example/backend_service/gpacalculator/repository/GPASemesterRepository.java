package com.example.backend_service.gpacalculator.repository;

import com.example.backend_service.gpacalculator.model.GPASemester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GPASemesterRepository extends JpaRepository<GPASemester, String> {
    List<GPASemester> findByStudentId(Long studentId);

    List<GPASemester> findByStudentIdOrderByYearDescSemesterDesc(Long studentId);

    Optional<GPASemester> findByIdAndStudentId(String id, Long studentId);
}
