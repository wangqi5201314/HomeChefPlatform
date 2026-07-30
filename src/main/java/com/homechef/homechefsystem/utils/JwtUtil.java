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

    /**
     * userType 用于区分当前 token 属于哪一类登录主体。
     * 拦截器解析 token 后，会根据这个字段把 userId/adminId/chefId 放入 LoginUserContext。
     */
    public static final String USER_TYPE_USER = "USER";
    public static final String USER_TYPE_ADMIN = "ADMIN";
    public static final String USER_TYPE_CHEF = "CHEF";

    /**
     * 本项目手写了一个轻量 JWT 实现：
     * header.payload.signature
     *
     * header: 记录签名算法和 token 类型
     * payload: 记录业务身份信息和过期时间
     * signature: 使用 HMAC-SHA256 对 header.payload 签名，防止 token 被篡改
     */
    private static final String JWT_ALG = "HS256";
    private static final String JWT_TYP = "JWT";
    /**
     * token 有效期：7 天。
     * 这里使用毫秒时间戳，生成时写入 payload.exp，校验时和当前时间比较。
     */
    private static final long EXPIRE_MILLIS = 7L * 24 * 60 * 60 * 1000;
    /**
     * HMAC 签名密钥。
     * 生产环境建议放到环境变量或配置中心，不建议直接硬编码在源码里。
     */
    private static final String SECRET = "homechef-platform-jwt-secret-20260317";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    /**
     * 生成普通用户 token。
     * payload 中写入 userId 和 userType=USER，后续用户端接口可通过 token 识别当前用户。
     */
    public String generateUserToken(Long userId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("userId", userId);
        claims.put("userType", USER_TYPE_USER);
        return generateToken(claims);
    }

    /**
     * 生成管理员 token。
     * 管理员和普通用户、厨师使用不同的身份字段，避免不同端身份混用。
     */
    public String generateAdminToken(Long adminId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("adminId", adminId);
        claims.put("userType", USER_TYPE_ADMIN);
        return generateToken(claims);
    }

    /**
     * 生成厨师 token。
     * 厨师端接口通常依赖 chefId 判断当前登录厨师，只允许操作自己的数据。
     */
    public String generateChefToken(Long chefId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("chefId", chefId);
        claims.put("userType", USER_TYPE_CHEF);
        return generateToken(claims);
    }

    /**
     * 通用 token 生成方法。
     * 入参 claims 是业务身份信息，例如 userId/adminId/chefId。
     * 方法内部会额外补充 iat 和 exp，再按 JWT 三段式拼接并签名。
     */
    public String generateToken(Map<String, Object> claims) {
        try {
            long now = System.currentTimeMillis();

            // JWT 第一段：header，说明 token 类型和签名算法。
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", JWT_ALG);
            header.put("typ", JWT_TYP);

            // JWT 第二段：payload，保存登录主体、签发时间和过期时间。
            Map<String, Object> payload = new LinkedHashMap<>(claims);
            payload.put("iat", now);
            payload.put("exp", now + EXPIRE_MILLIS);

            // header 和 payload 需要做 Base64URL 编码，避免普通 Base64 中的 + / = 影响 URL 或 HTTP 头传输。
            String headerPart = base64UrlEncode(OBJECT_MAPPER.writeValueAsBytes(header));
            String payloadPart = base64UrlEncode(OBJECT_MAPPER.writeValueAsBytes(payload));
            String content = headerPart + "." + payloadPart;
            // JWT 第三段：signature，对 header.payload 签名。只要 payload 被篡改，签名校验就会失败。
            String signature = sign(content);
            return content + "." + signature;
        } catch (Exception e) {
            // 工具类保持现有项目风格：生成失败返回 null，由调用方决定如何处理。
            return null;
        }
    }

    /**
     * 解析 token 并校验签名。
     * 注意：这个方法只负责判断 token 是否被篡改并解析 payload，
     * 是否过期由 isTokenValid 再检查 exp 字段。
     */
    public Map<String, Object> parseToken(String token) {
        try {
            String[] parts = splitToken(token);
            if (parts == null) {
                return null;
            }

            // 重新用服务端密钥计算签名，并与 token 第三段做常量时间比较，降低时序攻击风险。
            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            // 签名通过后再解析 payload，避免使用被篡改的身份信息。
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return OBJECT_MAPPER.readValue(payloadBytes, MAP_TYPE_REFERENCE);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断 token 是否有效。
     * 有效必须同时满足：
     * 1. 结构正确
     * 2. 签名正确
     * 3. exp 未过期
     */
    public boolean isTokenValid(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return false;
        }

        Long exp = getLongValue(claims.get("exp"));
        return exp != null && exp > System.currentTimeMillis();
    }

    /**
     * 从 token 中读取普通用户ID。
     * 如果 token 无效、不是用户 token，或者没有 userId，返回 null。
     */
    public Long getUserId(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return getLongValue(claims.get("userId"));
    }

    /**
     * 从 token 中读取管理员ID。
     */
    public Long getAdminId(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return getLongValue(claims.get("adminId"));
    }

    /**
     * 从 token 中读取厨师ID。
     */
    public Long getChefId(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return getLongValue(claims.get("chefId"));
    }

    /**
     * 读取当前 token 的主体类型：USER / ADMIN / CHEF。
     * 拦截器和权限注解会依赖这个字段区分不同端的登录态。
     */
    public String getUserType(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userType = claims.get("userType");
        return userType == null ? null : String.valueOf(userType);
    }

    /**
     * 从 HTTP Authorization 头中提取 token。
     * 标准格式是：Authorization: Bearer xxxxx.yyyyy.zzzzz
     */
    public String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    /**
     * JWT 必须由 3 段组成：
     * 1. header
     * 2. payload
     * 3. signature
     */
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

    /**
     * 使用 HMAC-SHA256 生成签名。
     * HMAC 是对称签名算法：生成和校验都使用同一个 SECRET。
     */
    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        return base64UrlEncode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * JWT 使用 Base64URL 编码，并去掉 padding。
     * 这样生成的 token 更适合放在 URL、HTTP Header、JSON 字段里传输。
     */
    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * JSON 反序列化后，数字可能是 Integer、Long，也可能被转成字符串。
     * 这里统一转成 Long，方便读取各种 id 和 exp 字段。
     */
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
