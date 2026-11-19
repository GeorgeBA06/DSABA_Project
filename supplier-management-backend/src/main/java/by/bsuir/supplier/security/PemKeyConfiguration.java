package by.bsuir.supplier.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;

import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.Base64;

@Configuration
public class PemKeyConfiguration {
    @Bean
    RSAPrivateKey privateJwtKey(
            @Value("${JWT_PRIVATE_KEY_PATH:./secrets/jwt-private.pem}") String path)
            throws Exception {
        return (RSAPrivateKey)
                KeyFactory.getInstance("RSA")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(decodePem(Path.of(path), "PRIVATE KEY")));
    }

    @Bean
    RSAPublicKey publicJwtKey(@Value("${JWT_PUBLIC_KEY_PATH:./secrets/jwt-public.pem}") String path)
            throws Exception {
        return (RSAPublicKey)
                KeyFactory.getInstance("RSA")
                        .generatePublic(
                                new X509EncodedKeySpec(decodePem(Path.of(path), "PUBLIC KEY")));
    }

    private static byte[] decodePem(Path path, String label) throws Exception {
        String pem = Files.readString(path);
        String b = "-----BEGIN " + label + "-----", e = "-----END " + label + "-----";
        if (!pem.contains(b) || !pem.contains(e))
            throw new IllegalStateException("Неверный формат JWT PEM: " + path);
        return Base64.getMimeDecoder()
                .decode(pem.replace(b, "").replace(e, "").replaceAll("\\s", ""));
    }

    @Bean
    JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        if (!publicKey.getModulus().equals(privateKey.getModulus()))
            throw new IllegalStateException("JWT private/public RSA keys do not match");
        RSAKey key =
                new RSAKey.Builder(publicKey)
                        .privateKey(privateKey)
                        .keyID("supplier-rs256")
                        .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            @Value("${security.jwt.issuer:supplier-management}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        var withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(
                new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                        withIssuer,
                        (Jwt jwt) ->
                                jwt.getAudience().contains("supplier-management")
                                        ? org.springframework.security.oauth2.core
                                                .OAuth2TokenValidatorResult.success()
                                        : org.springframework.security.oauth2.core
                                                .OAuth2TokenValidatorResult.failure(
                                                new org.springframework.security.oauth2.core
                                                        .OAuth2Error(
                                                        "invalid_token",
                                                        "Invalid audience",
                                                        null))));
        return decoder;
    }
}
