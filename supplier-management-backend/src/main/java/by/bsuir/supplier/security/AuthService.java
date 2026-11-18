package by.bsuir.supplier.security;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.dto.response.Tokens;
import by.bsuir.supplier.entity.auth.AuthSession;
import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.auth.UserRoleAssignment;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.UserStatus;
import by.bsuir.supplier.repository.auth.*;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;

/** Login, rotation, logout and administrative account creation. No password or token is audited. */
@Service
public class AuthService {
    private final UserRepository users;
    private final UserRoleAssignmentRepository roles;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final AccessTokenService jwt;
    private final ActorAccess access;
    private final AuditTrail audit;
    private final SecureRandom rng = new SecureRandom();
    private final String unknownUserHash;
    public AuthService(UserRepository users, UserRoleAssignmentRepository roles, AuthSessionRepository sessions,
        PasswordEncoder encoder, AccessTokenService jwt, ActorAccess access, AuditTrail audit) {
        this.users = users;
        this.roles = roles;
        this.sessions = sessions;
        this.encoder = encoder;
        this.jwt = jwt;
        this.access = access;
        this.audit = audit;
        this.unknownUserHash = encoder.encode("a-constant-dummy-for-login-timing");
    }
    @Transactional(noRollbackFor = AuthenticationFailure.class)
    public Tokens login(String login, String password) {
        if (login == null || password == null || login.length()>80 || password.length()>1024)throw new AuthenticationFailure();
        User user = users.findByLoginForUpdate(login.strip()).orElse(null);
        if (user == null) {
            encoder.matches(password, unknownUserHash);
            throw new AuthenticationFailure();
        }
        Instant now = Instant.now();
        if (user.getStatus() != UserStatus.ACTIVE || user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))throw new AuthenticationFailure();
        if (!encoder.matches(password, user.getPasswordHash())) {
            int failures = user.getFailedLoginCount() == null?1:user.getFailedLoginCount()+1;
            user.setFailedLoginCount(failures);
            if (failures>=5) {
                user.setLockedUntil(now.plus(Duration.ofMinutes(15)));
                user.setFailedLoginCount(0);
            }
            users.saveAndFlush(user);
            // noRollbackFor ensures the attempt/temporary lock is actually persisted.
            throw new AuthenticationFailure();
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        if (roles.findByUser_IdAndRevokedAtIsNull(user.getId()).isEmpty())throw new AuthenticationFailure();
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setTokenGeneration(0L);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(Duration.ofDays(7)));
        session.setRefreshTokenHash(hash(randomSecret()));
        session = sessions.saveAndFlush(session);
        String refresh = session.getId()+"."+randomSecret();
        session.setRefreshTokenHash(hash(refresh));
        sessions.saveAndFlush(session);
        audit.security(user,"LOGIN","User", user.getId(),"Успешный вход");
        return new Tokens(jwt.issue(user, session), refresh,"Bearer", jwt.ttlSeconds());
    }
    @Transactional(noRollbackFor = AuthenticationFailure.class)
    public Tokens refresh(String refreshToken) {
        UUID sessionId = tokenSessionId(refreshToken);
        AuthSession session = sessions.findByIdForUpdate(sessionId).orElseThrow(AuthenticationFailure::new);
        Instant now = Instant.now();
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(now))throw new AuthenticationFailure();
        if (!constantTime(session.getRefreshTokenHash(), hash(refreshToken))) {
            session.setRevokedAt(now);
            sessions.saveAndFlush(session);
            throw new AuthenticationFailure();
            // reuse of rotated token revokes the session
        }
        User user = users.findById(session.getUser().getId()).orElseThrow(AuthenticationFailure::new);
        if (user.getStatus() != UserStatus.ACTIVE || user.getLockedUntil() != null && user.getLockedUntil().isAfter(now) || roles.findByUser_IdAndRevokedAtIsNull(user.getId()).isEmpty())throw new AuthenticationFailure();
        session.setTokenGeneration(session.getTokenGeneration()+1L);
        session.setRotatedAt(now);
        String next = session.getId()+"."+randomSecret();
        session.setRefreshTokenHash(hash(next));
        sessions.saveAndFlush(session);
        return new Tokens(jwt.issue(user, session), next,"Bearer", jwt.ttlSeconds());
    }
    @Transactional
    public void logout(UUID userId, UUID sessionId) {
        AuthSession session = sessions.findByIdForUpdate(sessionId).orElseThrow(AuthenticationFailure::new);
        if (!session.getUser().getId().equals(userId))throw new AuthenticationFailure();
        session.setRevokedAt(Instant.now());
    }
    @Transactional
    public User createUser(UUID actorId, String login, String displayName, String password, Set<RoleCode> requestedRoles) {
        User admin = access.requireRole(actorId, RoleCode.ADMINISTRATOR);
        String name = Rules.text(login,"Логин").strip();
        Rules.require(name.matches("[a-zA-Z0-9._-]{3,80}"),"Логин: 3–80 латинских символов, цифр, ._- ");
        passwordRules(password);
        Rules.require(users.findByLogin(name).isEmpty(),"Логин уже используется");
        Rules.require(requestedRoles != null && !requestedRoles.isEmpty(),"Укажите роль");
        User user = new User();
        user.setLogin(name);
        user.setDisplayName(Rules.text(displayName,"Имя"));
        user.setPasswordHash(encoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user = users.saveAndFlush(user);
        for (RoleCode role:requestedRoles) {
            UserRoleAssignment a = new UserRoleAssignment();
            a.setUser(user);
            a.setAssignedBy(admin);
            a.setRoleCode(role);
            a.setAssignedAt(Instant.now());
            roles.save(a);
        }
        audit.security(admin,"ACCOUNT_CREATED","User", user.getId(),"Создана учётная запись");
        return user;
    }
    @Transactional
    public void adminResetPassword(UUID adminId, UUID targetId, String next) {
        User admin = access.requireRole(adminId, RoleCode.ADMINISTRATOR);
        Rules.require(!adminId.equals(targetId),"Для собственной записи используйте смену пароля");
        passwordRules(next);
        User target = users.findByIdForUpdate(targetId).orElseThrow(()->new BusinessException("Пользователь не найден"));
        target.setPasswordHash(encoder.encode(next));
        target.setFailedLoginCount(0);
        target.setLockedUntil(null);
        for (AuthSession session:sessions.findByUser_Id(targetId))if (session.getRevokedAt() == null)session.setRevokedAt(Instant.now());
        audit.security(admin,"PASSWORD_RESET","User", targetId,"Сброс пароля администратором");
    }
    @Transactional
    public void changePassword(UUID actorId, String current, String next) {
        User user = users.findByIdForUpdate(actorId).orElseThrow(AuthenticationFailure::new);
        if (!encoder.matches(current, user.getPasswordHash()))throw new AuthenticationFailure();
        passwordRules(next);
        user.setPasswordHash(encoder.encode(next));
        for (AuthSession session:sessions.findByUser_Id(actorId))if (session.getRevokedAt() == null)session.setRevokedAt(Instant.now());
        audit.security(user,"PASSWORD_CHANGED","User", user.getId(),"Пароль изменён");
    }
    private static void passwordRules(String password) {
        Rules.require(password != null && password.length()>=12 && password.length()<=128,"Пароль должен содержать 12–128 символов");
    }
    private String randomSecret() {
        byte[] data = new byte[32];
        rng.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
    private static String hash(String value) {
        try{
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    private static boolean constantTime(String a, String b) {
        return a != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
    }
    private static UUID tokenSessionId(String value) {
        try{
            if (value == null || !value.matches("[0-9a-fA-F-]{36}\\.[A-Za-z0-9_-]{43}"))throw new IllegalArgumentException();
            return UUID.fromString(value.substring(0, 36));
        }
        catch (Exception e) {
            throw new AuthenticationFailure();
        }
    }
}
