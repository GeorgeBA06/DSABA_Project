package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CredentialsRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
