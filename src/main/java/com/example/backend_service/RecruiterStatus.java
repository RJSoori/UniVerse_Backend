package com.example.backend_service;

public enum RecruiterStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    /**
     * A previously-VERIFIED recruiter edited their profile/documents (see
     * JobHubController#updateOwnProfile) and must be re-approved by an admin before they can
     * post new jobs again. Distinct from PENDING (a brand-new recruiter's first review) so the
     * Admin portal can queue the two separately.
     */
    RE_VERIFICATION
}