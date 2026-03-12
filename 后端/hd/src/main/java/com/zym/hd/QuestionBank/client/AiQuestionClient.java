package com.zym.hd.QuestionBank.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zym.hd.QuestionBank.config.AiQuestionProperties;
import com.zym.hd.QuestionBank.dto.PythonGenerateRequest;
import com.zym.hd.QuestionBank.dto.PythonGenerateResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AiQuestionClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiQuestionProperties properties;
    private final RestTemplate restTemplate;

    public AiQuestionClient(AiQuestionProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(properties.getConnectTimeoutMs(), 1000));
        requestFactory.setReadTimeout(Math.max(properties.getReadTimeoutMs(), 1000));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public PythonGenerateResponse generateQuestions(PythonGenerateRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("AI question generation is disabled");
        }
        if (!StringUtils.hasText(properties.getPythonBaseUrl())) {
            throw new IllegalArgumentException("AI python service base url is not configured");
        }
        if (request == null) {
            throw new IllegalArgumentException("AI python request is null");
        }

        try {
            String body = buildRequestBody(request);
            System.out.println("[AiQuestionClient] request body=" + body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizeBaseUrl(properties.getPythonBaseUrl()) + "/generate-questions",
                    HttpMethod.POST,
                    httpEntity,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException(
                        "AI service error: HTTP " + response.getStatusCode().value() + ", body=" + response.getBody());
            }
            if (!StringUtils.hasText(response.getBody())) {
                throw new IllegalArgumentException("AI service returned empty response");
            }
            return OBJECT_MAPPER.readValue(response.getBody(), PythonGenerateResponse.class);
        } catch (RestClientException e) {
            String message = e.getMessage();
            throw new IllegalArgumentException("AI service is unavailable: " + message, e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("AI service returned invalid JSON", e);
        }
    }

    private String buildRequestBody(PythonGenerateRequest request) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("courseId", request.getCourseId());
        payload.put("courseName", request.getCourseName());
        payload.put("knowledgePoints", request.getKnowledgePoints());
        payload.put("questionType", request.getQuestionType());
        payload.put("difficulty", request.getDifficulty());
        payload.put("count", request.getCount());
        payload.put("language", request.getLanguage());
        payload.put("extraRequirements", request.getExtraRequirements());
        return OBJECT_MAPPER.writeValueAsString(payload);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}