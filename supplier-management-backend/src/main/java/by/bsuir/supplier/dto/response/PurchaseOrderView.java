package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.OrderState;

import java.time.Instant;
import java.util.UUID;

public record PurchaseOrderView(
        UUID id,
        String number,
        UUID supplierId,
        UUID contractId,
        OrderState state,
        UUID currentApprovedVersionId,
        UUID createdById,
        Instant createdAt,
        Instant updatedAt) {}
