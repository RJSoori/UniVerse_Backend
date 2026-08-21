package com.example.backend_service.jobhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend_service.jobhub.model.JobReport;

@Repository
public interface JobReportRepository extends JpaRepository<JobReport, Long> {
    List<JobReport> findByResolvedFalseOrderByReportedAtDesc();
    List<JobReport> findByJobIdAndResolvedFalse(Long jobId);
    boolean existsByJobIdAndReportedByStudentIdAndResolvedFalse(Long jobId, Long reportedByStudentId);
}
