package com.zym.hd.report.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zym.hd.report.config.ReportAiDetectionProperties;
import com.zym.hd.report.dto.ReportAiDetectionRequest;
import com.zym.hd.report.dto.ReportAiDetectionResponse;
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
public class ReportAiDetectionClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ReportAiDetectionProperties properties;
    private final RestTemplate restTemplate;

    public ReportAiDetectionClient(ReportAiDetectionProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(properties.getConnectTimeoutMs(), 1000));
        requestFactory.setReadTimeout(Math.max(properties.getReadTimeoutMs(), 1000));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public ReportAiDetectionResponse detect(ReportAiDetectionRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("report AI detection is disabled");
        }
        if (!StringUtils.hasText(properties.getPythonBaseUrl())) {
            throw new IllegalArgumentException("report AI python base url is not configured");
        }
        if (request == null) {
            throw new IllegalArgumentException("report AI request is null");
        }

        try {
            String body = OBJECT_MAPPER.writeValueAsString(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizeBaseUrl(properties.getPythonBaseUrl()) + "/detect-report-ai",
                    HttpMethod.POST,
                    httpEntity,
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException(
                        "report AI service error: HTTP " + response.getStatusCode().value());
            }
            if (!StringUtils.hasText(response.getBody())) {
                throw new IllegalArgumentException("report AI service returned empty response");
            }
            return OBJECT_MAPPER.readValue(response.getBody(), ReportAiDetectionResponse.class);
        } catch (RestClientException e) {
            throw new IllegalArgumentException("report AI service is unavailable: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("report AI service returned invalid JSON", e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
