package by.bsuir.supplier.dto.response;
/** Tokens and their expiration returned from authentication endpoints. */
public record Tokens(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds
) {}
