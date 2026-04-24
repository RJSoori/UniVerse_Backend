package com.example.backend_service.gpacalculator.repository;

import com.example.backend_service.gpacalculator.model.GPASemester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GPASemesterRepository extends JpaRepository<GPASemester, String> {
    List<GPASemester> findByStudentId(String studentId);

    List<GPASemester> findByStudentIdOrderByYearDescSemesterDesc(String studentId);
}
