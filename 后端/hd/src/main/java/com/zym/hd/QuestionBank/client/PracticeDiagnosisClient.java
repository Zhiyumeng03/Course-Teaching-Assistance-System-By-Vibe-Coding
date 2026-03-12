package com.zym.hd.QuestionBank.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zym.hd.QuestionBank.config.PracticeDiagnosisProperties;
import com.zym.hd.QuestionBank.dto.PracticeDiagnosisRequest;
import com.zym.hd.QuestionBank.dto.PracticeDiagnosisResponse;
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
public class PracticeDiagnosisClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PracticeDiagnosisProperties properties;
    private final RestTemplate restTemplate;

    public PracticeDiagnosisClient(PracticeDiagnosisProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(properties.getConnectTimeoutMs(), 1000));
        requestFactory.setReadTimeout(Math.max(properties.getReadTimeoutMs(), 1000));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public PracticeDiagnosisResponse diagnose(PracticeDiagnosisRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("practice diagnosis is disabled");
        }
        if (!StringUtils.hasText(properties.getPythonBaseUrl())) {
            throw new IllegalArgumentException("practice diagnosis python base url is not configured");
        }
        if (request == null) {
            throw new IllegalArgumentException("practice diagnosis request is null");
        }

        try {
            String body = OBJECT_MAPPER.writeValueAsString(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizeBaseUrl(properties.getPythonBaseUrl()) + "/diagnose-practice",
                    HttpMethod.POST,
                    httpEntity,
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException(
                        "practice diagnosis service error: HTTP " + response.getStatusCode().value());
            }
            if (!StringUtils.hasText(response.getBody())) {
                throw new IllegalArgumentException("practice diagnosis service returned empty response");
            }
            return OBJECT_MAPPER.readValue(response.getBody(), PracticeDiagnosisResponse.class);
        } catch (RestClientException e) {
            throw new IllegalArgumentException("practice diagnosis service is unavailable: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("practice diagnosis service returned invalid JSON", e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
