package com.example.backend_service.jobhub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Body for editing an existing job posting - deliberately omits recruiterId/postedAt/status,
 * which either come from the URL/token or shouldn't change on an edit. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobUpdateRequest(
        String title,
        String description,
        String requirements,
        String skills,
        String salaryInfo,
        String workType,
        String employmentType
) {}
