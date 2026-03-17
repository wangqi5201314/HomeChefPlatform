package com.homechef.homechefsystem.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtUtil {

    public static final String USER_TYPE_USER = "USER";
    public static final String USER_TYPE_ADMIN = "ADMIN";

    private static final String JWT_ALG = "HS256";
    private static final String JWT_TYP = "JWT";
    private static final long EXPIRE_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private static final String SECRET = "homechef-platform-jwt-secret-20260317";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    public String generateUserToken(Long userId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("userId", userId);
        claims.put("userType", USER_TYPE_USER);
        return generateToken(claims);
    }

    public String generateAdminToken(Long adminId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("adminId", adminId);
        claims.put("userType", USER_TYPE_ADMIN);
        return generateToken(claims);
    }

    public String generateToken(Map<String, Object> claims) {
        try {
            long now = System.currentTimeMillis();

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", JWT_ALG);
            header.put("typ", JWT_TYP);

            Map<String, Object> payload = new LinkedHashMap<>(claims);
            payload.put("iat", now);
            payload.put("exp", now + EXPIRE_MILLIS);

            String headerPart = base64UrlEncode(OBJECT_MAPPER.writeValueAsBytes(header));
            String payloadPart = base64UrlEncode(OBJECT_MAPPER.writeValueAsBytes(payload));
            String content = headerPart + "." + payloadPart;
            String signature = sign(content);
            return content + "." + signature;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> parseToken(String token) {
        try {
            String[] parts = splitToken(token);
            if (parts == null) {
                return null;
            }

            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return OBJECT_MAPPER.readValue(payloadBytes, MAP_TYPE_REFERENCE);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return false;
        }

        Long exp = getLongValue(claims.get("exp"));
        return exp != null && exp > System.currentTimeMillis();
    }

    public Long getUserId(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return getLongValue(claims.get("userId"));
    }

    public Long getAdminId(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return getLongValue(claims.get("adminId"));
    }

    public String getUserType(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userType = claims.get("userType");
        return userType == null ? null : String.valueOf(userType);
    }

    public String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    private String[] splitToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        return parts;
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        return base64UrlEncode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
