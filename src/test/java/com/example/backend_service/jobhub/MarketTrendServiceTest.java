package com.example.backend_service.jobhub;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend_service.jobhub.dto.MarketTrendEntry;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.example.backend_service.jobhub.service.MarketTrendService;
import com.fasterxml.jackson.databind.ObjectMapper;


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

    @Test
    void invalidate_forcesRecomputeEvenWithinTtl() {
        Instant now = Instant.now();
        // First call sees one posting; invalidate() is meant to model a job being posted/edited/
        // deleted/blocked right after - the very next getTrend() call must reflect that change
        // instead of replaying the cached pre-mutation answer for up to CACHE_TTL.
        when(jobRepository.findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(any()))
                .thenReturn(List.of(job("Data Analyst", now)))
                .thenReturn(List.of(job("Data Analyst", now), job("Data Analyst", now)));

        MarketTrendService service = serviceWithNoGeminiKey();
        List<MarketTrendEntry> beforeInvalidate = service.getTrend();
        service.invalidate();
        List<MarketTrendEntry> afterInvalidate = service.getTrend();

        assertThat(beforeInvalidate).containsExactly(new MarketTrendEntry("Data Analyst", 1));
        assertThat(afterInvalidate).containsExactly(new MarketTrendEntry("Data Analyst", 2));
        verify(jobRepository, times(2)).findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(any());
    }
}
