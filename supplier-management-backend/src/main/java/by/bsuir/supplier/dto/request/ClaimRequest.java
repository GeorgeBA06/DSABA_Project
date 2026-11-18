package by.bsuir.supplier.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import by.bsuir.supplier.entity.enums.ViolationType;

public record ClaimRequest(
    @NotBlank String number,
    @NotNull UUID supplierId,
    @NotNull UUID orderLineId,
    UUID deliveryLineId,
    @NotNull ViolationType type,
    @NotBlank String description,
    @NotBlank String requirements,
    LocalDate responseDueOn
) {}
