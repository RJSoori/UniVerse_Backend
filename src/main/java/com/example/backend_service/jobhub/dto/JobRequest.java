package com.example.backend_service.jobhub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobRequest(
        String title,
        String description,
        String requirements,
        String skills,
        String salaryInfo,
        String workType,
        String employmentType,
        String postedAt,
        Long recruiterId
) {
}
