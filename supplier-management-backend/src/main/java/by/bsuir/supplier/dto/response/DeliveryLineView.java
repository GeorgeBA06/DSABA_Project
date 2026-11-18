package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeliveryLineView(
    UUID id,
    UUID deliveryId,
    UUID orderVersionLineId,
    BigDecimal quantity,
    UUID replacementForLineId,
    UUID claimId
) {}
