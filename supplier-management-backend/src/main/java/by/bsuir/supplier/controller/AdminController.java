package by.bsuir.supplier.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.request.AccountRequest;
import by.bsuir.supplier.dto.request.PasswordResetRequest;
import by.bsuir.supplier.dto.request.ReasonRequest;
import by.bsuir.supplier.dto.request.RoleRequest;
import by.bsuir.supplier.dto.response.AuditEventView;
import by.bsuir.supplier.dto.response.UserView;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.mapper.AuthViewMapper;
import by.bsuir.supplier.security.AuthService;
import by.bsuir.supplier.service.auth.AccessAdministrationService;
import by.bsuir.supplier.service.query.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminController {
    private final AuthService auth;
    private final AccessAdministrationService access;
    private final AuthQueries users;
    private final AuditQueries audit;
    public AdminController(AuthService auth, AccessAdministrationService access, AuthQueries users, AuditQueries audit) {
        this.auth = auth;
        this.access = access;
        this.users = users;
        this.audit = audit;
    }
    @GetMapping("/users")
    public PageResponse<UserView> users(
        Authentication a,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return users.listUser(ApiActor.id(a), page, size);
    }
    @GetMapping("/users/{id}")
    public UserView user(Authentication a, @PathVariable UUID id) {
        return users.getUser(ApiActor.id(a), id);
    }
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(Authentication a, @Valid @RequestBody AccountRequest data) {
        return AuthViewMapper.toView(auth.createUser(ApiActor.id(a), data.login(), data.displayName(), data.password(), data.roles()));
    }
    @GetMapping("/users/{id}/roles")
    public List<RoleCode> roles(Authentication a, @PathVariable UUID id) {
        return users.activeRoles(ApiActor.id(a), id);
    }
    @PostMapping("/users/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void role(Authentication a, @PathVariable UUID id, @Valid @RequestBody RoleRequest data) {
        access.grant(ApiActor.id(a), id, data.role());
    }
    @DeleteMapping("/users/{id}/roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(Authentication a, @PathVariable UUID id, @PathVariable RoleCode role) {
        access.revoke(ApiActor.id(a), id, role);
    }
    @PostMapping("/users/{id}/block")
    public UserView block(Authentication a, @PathVariable UUID id, @Valid @RequestBody ReasonRequest data) {
        return AuthViewMapper.toView(access.block(ApiActor.id(a), id, data.reason()));
    }
    @PostMapping("/users/{id}/unblock")
    public UserView unblock(Authentication a, @PathVariable UUID id, @Valid @RequestBody ReasonRequest d) {
        return AuthViewMapper.toView(access.unblock(ApiActor.id(a), id, d.reason()));
    }
    @PostMapping("/users/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(Authentication a, @PathVariable UUID id, @Valid @RequestBody PasswordResetRequest d) {
        auth.adminResetPassword(ApiActor.id(a), id, d.newPassword());
    }
    @GetMapping("/audit")
    public PageResponse<AuditEventView> audit(
        Authentication a,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return audit.listAuditEvent(ApiActor.id(a), page, size);
    }
}
