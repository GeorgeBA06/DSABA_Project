package by.bsuir.supplier.controller;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** JWT subject is always resolved by Spring Security, never read from client input DTOs. */
public final class ApiActor {
    private ApiActor() {
    }
    public static UUID id(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())throw new org.springframework.security.authentication.BadCredentialsException("Неавторизован");
        return UUID.fromString(jwt.getName());
    }
    public static UUID session(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())throw new org.springframework.security.authentication.BadCredentialsException("Неавторизован");
        return UUID.fromString(jwt.getToken().getClaimAsString("sid"));
    }
}
