package by.bsuir.supplier.dto.request;

import by.bsuir.supplier.entity.enums.RoleCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AccountRequest(
        @NotBlank String login,
        @NotBlank String displayName,
        @NotBlank String password,
        @NotEmpty Set<@NotNull RoleCode> roles) {}
