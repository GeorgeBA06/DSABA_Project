package by.bsuir.supplier.security;

import by.bsuir.supplier.entity.auth.AuthSession;
import by.bsuir.supplier.entity.auth.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccessTokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration ttl;

    public AccessTokenService(
            JwtEncoder encoder,
            @Value("${security.jwt.issuer:supplier-management}") String issuer,
            @Value("${security.jwt.access-minutes:15}") long accessMinutes) {
        if (accessMinutes < 1 || accessMinutes > 60)
            throw new IllegalArgumentException("JWT TTL must be 1..60 min");
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttl = Duration.ofMinutes(accessMinutes);
    }

    public String issue(User user, AuthSession session) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(user.getId().toString())
                        .audience(List.of("supplier-management"))
                        .issuedAt(now)
                        .notBefore(now)
                        .expiresAt(now.plus(ttl))
                        .id(UUID.randomUUID().toString())
                        .claim("sid", session.getId().toString())
                        .claim("token_use", "access")
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }
}
