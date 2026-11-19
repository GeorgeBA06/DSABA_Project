package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AssessmentRequest(
        @NotNull UUID supplierId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull LocalDate applicableFrom,
        @NotNull LocalDate applicableTo,
        @NotNull @Min(0) @Max(100) Integer score,
        @NotBlank String justification) {}
