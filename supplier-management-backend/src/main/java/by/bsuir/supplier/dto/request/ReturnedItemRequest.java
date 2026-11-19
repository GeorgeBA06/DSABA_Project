package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnedItemRequest(
        @NotNull UUID acceptanceLineId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        @NotBlank String defectDescription) {}
