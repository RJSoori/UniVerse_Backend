package com.example.backend_service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface RecruiterRepository extends JpaRepository<Recruiter, Long> {}

@Repository
interface JobRepository extends JpaRepository<Job, Long> {}
