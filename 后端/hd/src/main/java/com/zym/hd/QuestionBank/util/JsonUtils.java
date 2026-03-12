package com.zym.hd.QuestionBank.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("json serialize failed", e);
        }
    }

    public static Map<String, Object> parseObject(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(text, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("json parse failed", e);
        }
    }

    public static List<Object> parseList(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(text, new TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("json parse failed", e);
        }
    }

    public static <T> T parse(String text, TypeReference<T> typeReference) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (Exception e) {
            throw new IllegalArgumentException("json parse failed", e);
        }
    }

    public static <T> T convert(Object value, Class<T> targetType) {
        try {
            return OBJECT_MAPPER.convertValue(value, targetType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("json convert failed", e);
        }
    }

    public static ObjectMapper mapper() {
        return OBJECT_MAPPER;
    }
}
