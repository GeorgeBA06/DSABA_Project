package by.bsuir.supplier.security;

import static org.junit.jupiter.api.Assertions.*;

import by.bsuir.supplier.entity.auth.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

/** Uses real Nimbus/Spring JWT classes in Maven; no database or Docker required. */
class JwtTokenTest {
    @Test
    void signsAndVerifiesJwtWithSessionBinding() throws Exception {
        PemKeyConfiguration keys = new PemKeyConfiguration();
        var privateKey = keys.privateJwtKey("src/test/resources/jwt-test-private.pem");
        var publicKey = keys.publicJwtKey("src/test/resources/jwt-test-public.pem");
        var service =
                new AccessTokenService(
                        keys.jwtEncoder(publicKey, privateKey), "supplier-management", 15);
        User user = new User();
        user.setId(UUID.randomUUID());
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID());
        String signed = service.issue(user, session);
        Jwt decoded = keys.jwtDecoder(publicKey, "supplier-management").decode(signed);
        assertEquals(user.getId().toString(), decoded.getSubject());
        assertEquals(session.getId().toString(), decoded.getClaimAsString("sid"));
        assertEquals("access", decoded.getClaimAsString("token_use"));
        assertTrue(decoded.getExpiresAt().isAfter(Instant.now()));
    }
}
