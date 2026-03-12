package com.zym.hd.security;

import com.zym.hd.user.entity.UserEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final String JWT_TYP = "JWT";
    private static final String JWT_ALG = "HS256";
    private static final long EXPIRE_SECONDS = 24 * 60 * 60;
    private static final String SECRET = "change-this-to-your-production-secret-key";

    public String generateToken(UserEntity user) {
        long now = Instant.now().getEpochSecond();
        long exp = now + EXPIRE_SECONDS;

        try {
            String headerJson = "{\"typ\":\"" + JWT_TYP + "\",\"alg\":\"" + JWT_ALG + "\"}";
            String payloadJson = "{\"uid\":" + user.getId()
                    + ",\"sub\":\"" + escapeJson(user.getUsername())
                    + "\",\"role\":\"" + escapeJson(normalizeRole(user.getRole()))
                    + "\",\"iat\":" + now
                    + ",\"exp\":" + exp + "}";
            String headerPart = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadPart = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
            String content = headerPart + "." + payloadPart;
            String sign = sign(content);
            return content + "." + sign;
        } catch (Exception e) {
            throw new RuntimeException("generate token failed", e);
        }
    }

    public LoginUser parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("token format invalid");
            }
            String content = parts[0] + "." + parts[1];
            String expectedSign = sign(content);
            if (!MessageDigest.isEqual(
                    expectedSign.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("token signature invalid");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Long expValue = extractLong(payload, "exp");
            if (expValue == null) {
                throw new IllegalArgumentException("token payload invalid");
            }
            long exp = expValue;
            long now = Instant.now().getEpochSecond();
            if (exp < now) {
                throw new IllegalArgumentException("token expired");
            }

            Long uid = extractLong(payload, "uid");
            String username = extractString(payload, "sub");
            String role = normalizeRole(extractString(payload, "role"));
            if (uid == null || username == null || role == null) {
                throw new IllegalArgumentException("token payload invalid");
            }
            return new LoginUser(uid, username, role);
        } catch (Exception e) {
            throw new IllegalArgumentException("token invalid", e);
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALG);
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
        byte[] signed = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return base64Url(signed);
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Long extractLong(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }
}

