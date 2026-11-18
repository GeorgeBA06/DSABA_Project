package by.bsuir.supplier.security;

import java.time.Instant;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.entity.auth.AuthSession;
import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.enums.UserStatus;
import by.bsuir.supplier.repository.auth.*;

/** Rechecks current user, grants and session on EVERY request; token claims never authorize roles. */
@Component
public class SessionAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository users;
    private final UserRoleAssignmentRepository grants;
    private final AuthSessionRepository sessions;
    public SessionAuthenticationConverter(UserRepository users, UserRoleAssignmentRepository grants, AuthSessionRepository sessions) {
        this.users = users;
        this.grants = grants;
        this.sessions = sessions;
    }
    @Override @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            if (!"access".equals(jwt.getClaimAsString("token_use")))throw new AuthenticationFailure();
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
            User user = users.findById(userId).orElseThrow(AuthenticationFailure::new);
            if (user.getStatus() != UserStatus.ACTIVE || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())))throw new AuthenticationFailure();
            AuthSession session = sessions.findById(sessionId).orElseThrow(AuthenticationFailure::new);
            if (!session.getUser().getId().equals(userId) || session.getRevokedAt() != null || !session.getExpiresAt().isAfter(Instant.now()))throw new AuthenticationFailure();
            var roles = grants.findByUser_IdAndRevokedAtIsNull(userId).stream()
                .map(x->new SimpleGrantedAuthority("ROLE_"+x.getRoleCode().name())).toList();
            if (roles.isEmpty())throw new AuthenticationFailure();
            return new JwtAuthenticationToken(jwt, roles, userId.toString());
        }
        catch (AuthenticationFailure | IllegalArgumentException ex) {
            throw new BadCredentialsException("Недействительный токен");
        }
    }
}
