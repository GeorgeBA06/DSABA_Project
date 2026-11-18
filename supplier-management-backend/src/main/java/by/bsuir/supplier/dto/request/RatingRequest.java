package by.bsuir.supplier.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import by.bsuir.supplier.entity.enums.RatingScope;

public record RatingRequest(@NotNull UUID supplierId, @NotNull RatingScope scope, UUID categoryId) {}
