package by.bsuir.supplier.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import by.bsuir.supplier.entity.enums.RoleCode;

public record AccountRequest(
    @NotBlank String login,
    @NotBlank String displayName,
    @NotBlank String password,
    @NotEmpty Set<@NotNull RoleCode> roles
) {}
