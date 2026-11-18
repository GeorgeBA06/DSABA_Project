package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.OrderState;

public record PurchaseOrderView(
    UUID id,
    String number,
    UUID supplierId,
    UUID contractId,
    OrderState state,
    UUID currentApprovedVersionId,
    UUID createdById,
    Instant createdAt,
    Instant updatedAt
) {}
