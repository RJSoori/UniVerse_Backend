package com.example.backend_service.gpacalculator.repository;

import com.example.backend_service.gpacalculator.model.GPASubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GPASubjectRepository extends JpaRepository<GPASubject, String> {
    List<GPASubject> findBySemesterId(String semesterId);

    List<GPASubject> findByStudentId(String studentId);

    List<GPASubject> findByStudentIdAndIsGpaTrue(String studentId);
}
