package by.bsuir.supplier.dto.request;

import by.bsuir.supplier.entity.enums.RatingScope;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RatingRequest(
        @NotNull UUID supplierId, @NotNull RatingScope scope, UUID categoryId) {}
