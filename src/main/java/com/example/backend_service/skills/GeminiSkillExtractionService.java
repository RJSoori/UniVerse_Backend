package com.example.backend_service.skills;

import com.example.backend_service.common.exception.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Extracts a skills list from resume text using Google's Gemini API — plain REST, no SDK,
 * mirroring how {@link com.example.backend_service.notifications.PushNotificationService}
 * calls the Expo push API elsewhere in this codebase. Uses Gemini's structured-output mode
 * (a JSON response schema) so the response is guaranteed to parse as a JSON string array,
 * rather than relying on a "return ONLY JSON" prompt instruction the model might not follow.
 */
@Service
public class GeminiSkillExtractionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiSkillExtractionService.class);
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiSkillExtractionService(
            ObjectMapper objectMapper,
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-3.1-flash-lite}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * @param resumeText raw text pulled from the uploaded CV (see {@link PdfTextExtractor})
     * @return deduplicated, trimmed skill names as identified by the model
     */
    public List<String> extractSkills(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new BadRequestException(
                    "Couldn't find any readable text in that file. If it's a scanned or image-only PDF, try a text-based export instead.");
        }
        if (apiKey.isBlank()) {
            log.error("Gemini API key is not configured (GEMINI_API_KEY)");
            throw new BadRequestException("CV analysis isn't configured yet. Please try again later.");
        }

        String prompt = buildPrompt(resumeText);
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING"))));

        String url = API_URL_TEMPLATE.formatted(model, apiKey);
        try {
            String rawResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseSkills(rawResponse);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini skill extraction failed", e);
            throw new BadRequestException("Unable to analyze that CV right now. Please try again shortly.");
        }
    }

    private List<String> parseSkills(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
        if (textNode.isMissingNode() || !textNode.isTextual()) {
            log.error("Unexpected Gemini response shape: {}", rawResponse);
            throw new BadRequestException("Unable to analyze that CV right now. Please try again shortly.");
        }

        List<String> skills = objectMapper.readValue(textNode.asText(), new TypeReference<List<String>>() {
        });
        return skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String buildPrompt(String resumeText) {
        return """
                You are extracting a skills inventory from a student's resume/CV for a university job board profile.
                Read the resume text below and identify every technical skill, tool, programming language, framework, and clearly relevant soft skill it mentions -\s
                anywhere in the document, not only under a "Skills" heading.

                Rules:
                - Return each skill as a short, human-readable name (e.g. "React", "Project Management", "SQL") - not a sentence or description.
                - Merge near-identical entries into one (e.g. "React" and "React.js" become just "React").
                - Do not invent skills that are not actually stated or clearly implied by the resume content.
                - If nothing resembling a skill is found, return an empty array.

                Resume text:
                %s
                """.formatted(resumeText);
    }
}
