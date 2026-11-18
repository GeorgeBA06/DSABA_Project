package by.bsuir.supplier.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssessmentRequest(
    @NotNull UUID supplierId,
    @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    @NotNull LocalDate applicableFrom,
    @NotNull LocalDate applicableTo,
    @NotNull @Min(0) @Max(100) Integer score,
    @NotBlank String justification
) {}
