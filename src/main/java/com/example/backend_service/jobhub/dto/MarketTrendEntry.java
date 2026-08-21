package com.example.backend_service.jobhub.dto;

/** One row of the Market Trend feature - a job title and how many postings for it (across all
 * recruiters) were created in the trailing window, used as a proxy for hiring demand. */
public record MarketTrendEntry(String title, long postingCount) {
}
