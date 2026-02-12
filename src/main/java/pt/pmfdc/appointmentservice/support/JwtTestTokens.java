package pt.pmfdc.appointmentservice.bdd.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public final class JwtTestTokens {

    private JwtTestTokens() {}

    public static String token(String secret, Map<String, Object> claims) {
        try {
            Instant now = Instant.now();

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .issuer("local-test")
                    .subject("bdd-user")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)));

            claims.forEach(builder::claim);

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to create test JWT", e);
        }
    }

    public static String adminToken(String secret) {
        return token(secret, Map.of("admin", true));
    }

    public static String nonAdminToken(String secret) {
        return token(secret, Map.of("admin", false));
    }
}