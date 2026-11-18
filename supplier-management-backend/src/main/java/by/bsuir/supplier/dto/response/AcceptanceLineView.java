package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AcceptanceLineView(
    UUID id,
    UUID acceptanceDocumentId,
    UUID deliveryLineId,
    BigDecimal acceptedQty,
    BigDecimal qualityRejectedQty,
    BigDecimal otherRejectedQty,
    String otherReason
) {}
