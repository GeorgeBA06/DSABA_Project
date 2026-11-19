package by.bsuir.supplier.dto.request;

import by.bsuir.supplier.entity.enums.ClaimResult;
import by.bsuir.supplier.entity.enums.RemedyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolveClaimRequest(
        @NotNull ClaimResult result, @NotNull RemedyType remedy, @NotBlank String description) {}
