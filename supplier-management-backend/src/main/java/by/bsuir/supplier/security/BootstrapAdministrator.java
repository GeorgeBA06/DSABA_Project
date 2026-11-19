package by.bsuir.supplier.security;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.auth.UserRoleAssignment;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.UserStatus;
import by.bsuir.supplier.repository.auth.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Explicit one-shot environment bootstrap. Never exposes an HTTP registration endpoint. */
@Component
public class BootstrapAdministrator implements ApplicationRunner {
    private final UserRepository users;
    private final UserRoleAssignmentRepository grants;
    private final PasswordEncoder passwords;
    private final String login, password;

    public BootstrapAdministrator(
            UserRepository users,
            UserRoleAssignmentRepository grants,
            PasswordEncoder passwords,
            @Value("${BOOTSTRAP_ADMIN_LOGIN:}") String login,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.grants = grants;
        this.passwords = passwords;
        this.login = login;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        if (login.isBlank() || password.isBlank())
            throw new IllegalStateException(
                    "Пустая БД: необходимо указать BOOTSTRAP_ADMIN_LOGIN и BOOTSTRAP_ADMIN_PASSWORD для первого запуска");
        if (!login.matches("[a-zA-Z0-9._-]{3,80}")
                || password.length() < 12
                || password.length() > 128)
            throw new IllegalStateException("Недопустимые реквизиты начального администратора");
        User user = new User();
        user.setLogin(login);
        user.setDisplayName("Администратор");
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setPasswordHash(passwords.encode(password));
        user = users.saveAndFlush(user);
        UserRoleAssignment grant = new UserRoleAssignment();
        grant.setUser(user);
        grant.setAssignedBy(user);
        grant.setRoleCode(RoleCode.ADMINISTRATOR);
        grant.setAssignedAt(Instant.now());
        grants.saveAndFlush(grant);
    }
}
