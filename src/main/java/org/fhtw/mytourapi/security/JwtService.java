package org.fhtw.mytourapi.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fhtw.mytourapi.domain.UserEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public IssuedJwt issueToken(UserEntity user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.getExpiration());
        String token = encode(
                Map.of("alg", "HS256", "typ", "JWT"),
                Map.of(
                        "iss", properties.getIssuer(),
                        "sub", user.getId().toString(),
                        "username", user.getUsername(),
                        "iat", issuedAt.getEpochSecond(),
                        "exp", expiresAt.getEpochSecond()
                )
        );

        return new IssuedJwt(token, expiresAt);
    }

    public JwtClaims parse(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidJwtException("JWT must contain header, payload, and signature.");
        }

        String signedPart = parts[0] + "." + parts[1];
        String expectedSignature = sign(signedPart);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new InvalidJwtException("JWT signature is invalid.");
        }

        Map<String, Object> payload = decodePayload(parts[1]);
        if (!properties.getIssuer().equals(payload.get("iss"))) {
            throw new InvalidJwtException("JWT issuer is invalid.");
        }

        Instant expiresAt = Instant.ofEpochSecond(numberClaim(payload, "exp"));
        if (!expiresAt.isAfter(Instant.now())) {
            throw new InvalidJwtException("JWT is expired.");
        }

        return new JwtClaims(
                Long.valueOf(stringClaim(payload, "sub")),
                stringClaim(payload, "username"),
                expiresAt
        );
    }

    private String encode(Map<String, Object> header, Map<String, Object> payload) {
        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signedPart = encodedHeader + "." + encodedPayload;
        return signedPart + "." + sign(signedPart);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64UrlEncoder().encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new InvalidJwtException("Could not encode JWT.", exception);
        }
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        try {
            byte[] decoded = base64UrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(decoded, CLAIMS_TYPE);
        } catch (Exception exception) {
            throw new InvalidJwtException("JWT payload is invalid.", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return base64UrlEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new InvalidJwtException("Could not sign JWT.", exception);
        }
    }

    private String stringClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new InvalidJwtException("JWT claim is missing: " + name);
        }

        return value.toString();
    }

    private long numberClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new InvalidJwtException("JWT numeric claim is missing: " + name);
    }

    private Base64.Encoder base64UrlEncoder() {
        return Base64.getUrlEncoder().withoutPadding();
    }

    private Base64.Decoder base64UrlDecoder() {
        return Base64.getUrlDecoder();
    }

    public record IssuedJwt(
            String token,
            Instant expiresAt
    ) {
    }
}
