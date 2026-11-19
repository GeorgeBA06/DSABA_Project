package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IncreaseRequest(
        @NotNull UUID orderLineId,
        @NotNull @DecimalMin("0.001") BigDecimal additional,
        @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
        @NotNull LocalDate dueOn,
        @NotBlank String reason) {}
