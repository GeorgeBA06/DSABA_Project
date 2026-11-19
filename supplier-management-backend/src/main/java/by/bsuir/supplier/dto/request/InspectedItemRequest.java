package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InspectedItemRequest(
        @NotNull UUID deliveryLineId,
        @NotNull @DecimalMin("0.0") BigDecimal accepted,
        @NotNull @DecimalMin("0.0") BigDecimal qualityRejected,
        @NotNull @DecimalMin("0.0") BigDecimal otherRejected,
        String reason) {}
