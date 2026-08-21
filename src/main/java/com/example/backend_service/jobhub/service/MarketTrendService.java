package com.example.backend_service.jobhub.service;

import com.example.backend_service.jobhub.dto.MarketTrendEntry;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes the "Market Trend" top-3-job-titles-by-posting-volume signal for the trailing 3
 * calendar months. Recruiters word the same underlying role differently ("Software Engineering
 * Intern", "SE Intern", "Intern - Software Engineer"), so exact-string grouping alone
 * undercounts real demand - this asks Gemini to cluster the distinct titles from the window into
 * consistent canonical names in a single batched call (same REST/structured-output integration
 * pattern as {@link com.example.backend_service.skills.GeminiSkillExtractionService}), then
 * re-aggregates counts by canonical name.
 *
 * <p>All distinct titles are clustered in <b>one</b> call rather than canonicalizing each job's
 * title independently at posting time - if "Software Engineering Intern" and "SE Intern" were
 * each sent to Gemini in isolation, nothing would guarantee the two calls converge on the exact
 * same canonical string, which would defeat the whole point. Giving Gemini the full distinct-title
 * list at once lets it assign internally-consistent labels for that computation.
 *
 * <p>The result is cached in memory for {@link #CACHE_TTL} since {@code GET /api/jobs/market-trend}
 * is public and unauthenticated - hit on every Job Hub page load - and market trend is inherently
 * a slow-moving aggregate, so serving a slightly stale answer is a non-issue.
 */
@Service
public class MarketTrendService {

    private static final Logger log = LoggerFactory.getLogger(MarketTrendService.class);
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int TOP_N = 3;

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final String apiKey;
    private final String model;

    private volatile List<MarketTrendEntry> cachedResult;
    private volatile Instant cachedAt;

    public MarketTrendService(
            JobRepository jobRepository,
            ObjectMapper objectMapper,
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-3.1-flash-lite}") String model) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public synchronized List<MarketTrendEntry> getTrend() {
        if (cachedResult != null && cachedAt != null
                && Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cachedResult;
        }
        cachedResult = computeTrend();
        cachedAt = Instant.now();
        return cachedResult;
    }

    private List<MarketTrendEntry> computeTrend() {
        Instant since = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(3).toInstant();
        List<Job> recentJobs = jobRepository.findByCreatedAtAfterAndDeletedFalseAndBlockedFalse(since);

        // Exact-match counts first - cheap, deterministic, and the fallback if Gemini is
        // unavailable or its response can't be trusted.
        Map<String, Long> exactCounts = recentJobs.stream()
                .map(Job::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .map(String::trim)
                .collect(Collectors.groupingBy(title -> title, LinkedHashMap::new, Collectors.counting()));

        if (exactCounts.isEmpty()) {
            return List.of();
        }

        Map<String, String> canonicalByRawTitle = clusterTitles(exactCounts.keySet());

        Map<String, Long> canonicalCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : exactCounts.entrySet()) {
            String canonical = canonicalByRawTitle.getOrDefault(entry.getKey(), entry.getKey());
            canonicalCounts.merge(canonical, entry.getValue(), Long::sum);
        }

        return canonicalCounts.entrySet().stream()
                .map(e -> new MarketTrendEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(MarketTrendEntry::postingCount).reversed())
                .limit(TOP_N)
                .toList();
    }

    /**
     * Asks Gemini to assign each distinct raw title a canonicalTitle in one batched call.
     * Returns rawTitle -&gt; canonicalTitle; on any failure (missing API key, request error,
     * malformed/mismatched response) returns an empty map so callers fall back to exact-string
     * grouping instead of erroring - Market Trend degrading to "less clustered" is fine, a 500 is not.
     */
    private Map<String, String> clusterTitles(Collection<String> rawTitles) {
        if (apiKey.isBlank()) {
            log.warn("Gemini API key not configured - Market Trend falling back to exact-title grouping");
            return Map.of();
        }
        // A single distinct title has nothing to cluster against - skip the round trip.
        if (rawTitles.size() < 2) {
            return Map.of();
        }

        List<String> titles = List.copyOf(rawTitles);
        String prompt = buildPrompt(titles);
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "title", Map.of("type", "STRING"),
                                                "canonicalTitle", Map.of("type", "STRING")),
                                        "required", List.of("title", "canonicalTitle")))));

        String url = API_URL_TEMPLATE.formatted(model, apiKey);
        try {
            String rawResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseClusters(rawResponse, titles);
        } catch (Exception e) {
            log.error("Gemini title clustering failed - Market Trend falling back to exact-title grouping", e);
            return Map.of();
        }
    }

    private Map<String, String> parseClusters(String rawResponse, List<String> expectedTitles) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode() || !textNode.isTextual()) {
                log.error("Unexpected Gemini response shape for title clustering: {}", rawResponse);
                return Map.of();
            }

            List<TitleCluster> clusters = objectMapper.readValue(
                    textNode.asText(), new TypeReference<List<TitleCluster>>() {});

            // Trust position, not the echoed "title" field, for lining results back up to the
            // input - a model that alters whitespace/casing while echoing a title back shouldn't
            // silently break the mapping. If the count doesn't match one-to-one, the response
            // can't be trusted at all - fall back rather than guess.
            if (clusters.size() != expectedTitles.size()) {
                log.warn("Gemini returned {} entries for {} input titles - discarding clustering result",
                        clusters.size(), expectedTitles.size());
                return Map.of();
            }

            Map<String, String> result = new LinkedHashMap<>();
            for (int i = 0; i < expectedTitles.size(); i++) {
                String canonical = clusters.get(i).canonicalTitle();
                result.put(expectedTitles.get(i),
                        (canonical == null || canonical.isBlank()) ? expectedTitles.get(i) : canonical.trim());
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse Gemini title clustering response", e);
            return Map.of();
        }
    }

    private String buildPrompt(List<String> titles) {
        String titleList = titles.stream().map(t -> "- " + t).collect(Collectors.joining("\n"));
        return """
                You are grouping job posting titles that refer to the same underlying role, even
                when different companies word them differently (different word order,
                abbreviations, or phrasing).

                For each title below, output its "canonicalTitle" - a clear, standard,
                human-readable name for the role. Two titles that represent the same role MUST
                get the exact same canonicalTitle string, so they can be counted together.

                Examples of titles that should share the same canonicalTitle:
                - "Software Engineering Intern", "SE Intern", "Intern - Software Engineer" -> "Software Engineering Intern"
                - "Full Stack Developer", "Full-Stack Dev", "Fullstack Engineer" -> "Full Stack Developer"

                Rules:
                - Use Title Case for the canonicalTitle.
                - Do not merge genuinely different roles into the same canonicalTitle (e.g.
                  "Software Engineering Intern" and "Data Analyst Intern" must NOT match).
                - Return exactly one output entry per input title below, in the same order.

                Titles:
                %s
                """.formatted(titleList);
    }

    private record TitleCluster(String title, String canonicalTitle) {}
}
