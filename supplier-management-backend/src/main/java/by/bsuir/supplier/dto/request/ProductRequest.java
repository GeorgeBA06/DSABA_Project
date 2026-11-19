package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull UUID categoryId,
        @NotNull UUID unitId) {}
