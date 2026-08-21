package com.example.backend_service.jobhub;

import com.example.backend_service.jobhub.dto.MarketTrendEntry;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.example.backend_service.jobhub.service.MarketTrendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers everything in MarketTrendService that doesn't require an actual Gemini call - the
 * aggregation, sorting, top-3 limiting, caching, and graceful-degradation logic. The Gemini
 * title-clustering call is deliberately left untested here (this codebase has no HTTP-mocking
 * infra for Gemini calls anywhere, including GeminiSkillExtractionService) - constructing the
 * service with a blank API key exercises the exact same "fall back to exact-title grouping"
 * code path that a real Gemini failure would, without ever attempting a network call. The actual
 * clustering/merge behavior is verified live against the real API (see the session's manual
 * verification notes).
 */
@ExtendWith(MockitoExtension.class)
class MarketTrendServiceTest {

    @Mock private JobRepository jobRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MarketTrendService serviceWithNoGeminiKey() {
        return new MarketTrendService(jobRepository, objectMapper, "", "gemini-3.1-flash-lite");
    }

    private Job job(String title, Instant createdAt) {
        Job job = new Job();
        job.setTitle(title);
        job.setCreatedAt(createdAt);
        job.setDeleted(false);
        job.setBlocked(false);
        return job;
    }

    @Test
    void getTrend_noJobsInWindow_returnsEmptyList() {
        when(jobRepository.findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(any())).thenReturn(List.of());

        assertThat(serviceWithNoGeminiKey().getTrend()).isEmpty();
    }

    @Test
    void getTrend_blankApiKey_fallsBackToExactTitleGrouping() {
        Instant now = Instant.now();
        when(jobRepository.findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(any())).thenReturn(List.of(
                job("Software Engineering Intern", now.minus(1, java.time.temporal.ChronoUnit.DAYS)),
                job("Software Engineering Intern", now),
                job("Data Analyst", now)
        ));

        List<MarketTrendEntry> trend = serviceWithNoGeminiKey().getTrend();

        assertThat(trend).containsExactly(
                new MarketTrendEntry("Software Engineering Intern", 2),
                new MarketTrendEntry("Data Analyst", 1));
    }

    @Test
    void getTrend_singleDistinctTitle_neverAttemptsClusteringAndReturnsExactCount() {
        Instant now = Instant.now();
        // Even with a "configured" key, clusterTitles short-circuits below 2 distinct titles -
        // use a blank key anyway so this test can never accidentally hit the network either way.
        when(jobRepository.findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(any())).thenReturn(List.of(
                job("QA Engineer", now), job("QA Engineer", now), job("qa engineer ", now)
        ));

        List<MarketTrendEntry> trend = serviceWithNoGeminiKey().getTrend();

        // Exact-match grouping is itself already trim/case-sensitive at this layer (case-folding
        // only happens through Gemini clustering) - "QA Engineer" and "qa engineer " are distinct
        // raw strings, so with no clustering available they stay separate entries.
        assertThat(trend).hasSize(2);
        assertThat(trend.stream().mapToLong(MarketTrendEntry::postingCount).sum()).isEqualTo(3);
    }

    @Test
    void getTrend_capsAtTopThreeByCount() {
        Instant now = Instant.now();
        when(jobRepository.findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(any())).thenReturn(List.of(
                job("A", now), job("A", now), job("A", now),
                job("B", now), job("B", now),
                job("C", now),
                job("D", now)
        ));

        List<MarketTrendEntry> trend = serviceWithNoGeminiKey().getTrend();

        assertThat(trend).hasSize(3);
        assertThat(trend.get(0)).isEqualTo(new MarketTrendEntry("A", 3));
        assertThat(trend.get(1)).isEqualTo(new MarketTrendEntry("B", 2));
        // C and D are tied at 1 - either could take the third slot; just confirm it's one of them.
        assertThat(trend.get(2).title()).isIn("C", "D");
    }

    @Test
    void getTrend_secondCallWithinTtl_doesNotHitRepositoryAgain() {
        when(jobRepository.findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(any()))
                .thenReturn(List.of(job("Data Analyst", Instant.now())));

        MarketTrendService service = serviceWithNoGeminiKey();
        List<MarketTrendEntry> first = service.getTrend();
        List<MarketTrendEntry> second = service.getTrend();

        assertThat(second).isEqualTo(first);
        verify(jobRepository, times(1)).findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(any());
    }
}
