package com.example.backend_service.jobhub.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend_service.jobhub.enums.JobStatus;
import com.example.backend_service.jobhub.model.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);

    // Newest first for student browsing - createdAt DESC puts the most recently posted jobs on
    // top; jobs posted before that column existed (createdAt is null) sort last rather than
    // erroring, since SQL puts NULLs last in a DESC order. id DESC breaks ties (same-instant
    // postings, or among the null-createdAt jobs) so ordering stays stable page to page.
    List<Job> findByStatusAndActiveOrderByCreatedAtDescIdDesc(JobStatus status, boolean active);

    List<Job> findByRecruiterId(Long recruiterId);
    List<Job> findByRecruiterIdAndDeletedFalse(Long recruiterId);
    Optional<Job> findByIdAndRecruiterId(Long jobId, Long recruiterId);

    // Market Trend feature - real postings only (excludes deleted/blocked ones so removed or
    // upheld-report postings don't skew demand signal), created within the trailing window.
    List<Job> findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(Instant since);
}