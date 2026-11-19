package by.bsuir.supplier.service.auth;

import by.bsuir.supplier.entity.auth.AuthSession;
import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.auth.UserRoleAssignment;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.UserStatus;
import by.bsuir.supplier.repository.auth.*;
import by.bsuir.supplier.service.common.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Account and role lifecycle only. Credential hashing, login, JWT and refresh rotation come in the
 * security stage.
 */
@Service
public class AccessAdministrationService {
    private final ActorAccess access;
    private final UserRepository users;
    private final UserRoleAssignmentRepository assignments;
    private final AuthSessionRepository sessions;
    private final AuditTrail audit;

    public AccessAdministrationService(
            ActorAccess access,
            UserRepository users,
            UserRoleAssignmentRepository assignments,
            AuthSessionRepository sessions,
            AuditTrail audit) {
        this.access = access;
        this.users = users;
        this.assignments = assignments;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Transactional
    public UserRoleAssignment grant(UUID adminId, UUID targetId, RoleCode role) {
        User admin = access.requireRole(adminId, RoleCode.ADMINISTRATOR);
        Rules.present(role, "Назначаемая роль");
        Rules.require(!adminId.equals(targetId), "Нельзя самостоятельно изменять свои роли");
        User target = locked(targetId);
        Rules.require(target.getStatus() == UserStatus.ACTIVE, "Аккаунт заблокирован");
        Rules.require(
                assignments.findByUser_IdAndRevokedAtIsNull(targetId).stream()
                        .noneMatch(r -> r.getRoleCode() == role),
                "Роль уже назначена");
        UserRoleAssignment a = new UserRoleAssignment();
        a.setUser(target);
        a.setRoleCode(role);
        a.setAssignedBy(admin);
        a.setAssignedAt(Instant.now());
        a = assignments.saveAndFlush(a);
        audit.security(admin, "ROLE_GRANTED", "User", targetId, role.name());
        return a;
    }

    @Transactional
    public void revoke(UUID adminId, UUID targetId, RoleCode role) {
        User admin = access.requireRole(adminId, RoleCode.ADMINISTRATOR);
        Rules.present(role, "Отзываемая роль");
        Rules.require(!adminId.equals(targetId), "Нельзя самостоятельно изменять свои роли");
        locked(targetId);
        UserRoleAssignment assignment =
                assignments.findByUser_IdAndRevokedAtIsNull(targetId).stream()
                        .filter(r -> r.getRoleCode() == role)
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("Назначение роли не найдено"));
        assignment.setRevokedAt(Instant.now());
        audit.security(admin, "ROLE_REVOKED", "User", targetId, role.name());
    }

    @Transactional
    public User block(UUID adminId, UUID targetId, String reason) {
        User admin = access.requireRole(adminId, RoleCode.ADMINISTRATOR);
        Rules.require(!adminId.equals(targetId), "Нельзя блокировать свою учётную запись");
        User user = locked(targetId);
        Rules.require(user.getStatus() == UserStatus.ACTIVE, "Учётная запись уже заблокирована");
        user.setStatus(UserStatus.BLOCKED);
        for (AuthSession session : sessions.findByUser_Id(targetId))
            if (session.getRevokedAt() == null) session.setRevokedAt(Instant.now());
        audit.security(admin, "ACCOUNT_BLOCKED", "User", targetId, Rules.text(reason, "Основание"));
        return user;
    }

    @Transactional
    public User unblock(UUID adminId, UUID targetId, String reason) {
        User admin = access.requireRole(adminId, RoleCode.ADMINISTRATOR);
        Rules.require(!adminId.equals(targetId), "Для собственной записи используйте поддержку");
        User user = locked(targetId);
        Rules.require(user.getStatus() == UserStatus.BLOCKED, "Учётная запись не заблокирована");
        user.setStatus(UserStatus.ACTIVE);
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
        audit.security(
                admin, "ACCOUNT_UNBLOCKED", "User", targetId, Rules.text(reason, "Основание"));
        return user;
    }

    private User locked(UUID id) {
        return users.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Пользователь не найден"));
    }
}
