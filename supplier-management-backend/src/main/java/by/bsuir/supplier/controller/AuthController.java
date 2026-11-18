package by.bsuir.supplier.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import by.bsuir.supplier.dto.request.CredentialsRequest;
import by.bsuir.supplier.dto.request.LoginRequest;
import by.bsuir.supplier.dto.request.RefreshRequest;
import by.bsuir.supplier.dto.response.MeView;
import by.bsuir.supplier.dto.response.Tokens;
import by.bsuir.supplier.security.AuthService;
import by.bsuir.supplier.service.query.AuthQueries;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthQueries queries;
    public AuthController(AuthService auth, AuthQueries queries) {
        this.auth = auth;
        this.queries = queries;
    }
    @PostMapping("/login")
    public Tokens login(@Valid @RequestBody LoginRequest data) {
        return auth.login(data.login(), data.password());
    }
    @PostMapping("/refresh")
    public Tokens refresh(@Valid @RequestBody RefreshRequest data) {
        return auth.refresh(data.refreshToken());
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication principal) {
        auth.logout(ApiActor.id(principal), ApiActor.session(principal));
    }
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(Authentication principal, @Valid @RequestBody CredentialsRequest data) {
        auth.changePassword(ApiActor.id(principal), data.currentPassword(), data.newPassword());
    }
    @GetMapping("/me")
    public MeView me(Authentication principal) {
        return queries.me(ApiActor.id(principal));
    }
}
