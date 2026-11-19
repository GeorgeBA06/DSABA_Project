package by.bsuir.supplier.service.auth;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.auth.UserRoleAssignment;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.UserStatus;
import by.bsuir.supplier.repository.auth.UserRepository;
import by.bsuir.supplier.repository.auth.UserRoleAssignmentRepository;
import by.bsuir.supplier.service.common.BusinessException;
import by.bsuir.supplier.service.common.Rules;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Actor ID must come from authenticated server-side principal, NEVER from an untrusted request
 * body.
 */
@Service
public class ActorAccess {
    private final UserRepository users;
    private final UserRoleAssignmentRepository assignments;

    public ActorAccess(UserRepository users, UserRoleAssignmentRepository assignments) {
        this.users = users;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public User requireRole(UUID actorId, RoleCode... permitted) {
        User actor =
                users.findById(Rules.present(actorId, "Пользователь"))
                        .orElseThrow(() -> new BusinessException("Пользователь не найден"));
        Rules.require(actor.getStatus() == UserStatus.ACTIVE, "Учётная запись заблокирована");
        Rules.require(
                actor.getLockedUntil() == null || !actor.getLockedUntil().isAfter(Instant.now()),
                "Учётная запись временно заблокирована");
        Set<RoleCode> allowed = EnumSet.noneOf(RoleCode.class);
        allowed.addAll(Arrays.asList(permitted));
        boolean granted =
                assignments.findByUser_IdAndRevokedAtIsNull(actorId).stream()
                        .map(UserRoleAssignment::getRoleCode)
                        .anyMatch(allowed::contains);
        if (!granted)
            throw new org.springframework.security.access.AccessDeniedException(
                    "Недостаточно полномочий");
        return actor;
    }

    public static void independent(User author, User decisionMaker) {
        Rules.require(
                !author.getId().equals(decisionMaker.getId()),
                "Нельзя согласовывать собственное действие");
    }
}
