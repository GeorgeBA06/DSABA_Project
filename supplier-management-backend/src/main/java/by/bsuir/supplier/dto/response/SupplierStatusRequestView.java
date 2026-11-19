package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.DecisionState;
import by.bsuir.supplier.entity.enums.SupplierStatus;

import java.time.Instant;
import java.util.UUID;

public record SupplierStatusRequestView(
        UUID id,
        UUID supplierId,
        SupplierStatus fromStatus,
        SupplierStatus toStatus,
        String reason,
        UUID requestedById,
        Instant requestedAt,
        DecisionState state,
        UUID decidedById,
        Instant decidedAt,
        String decisionComment) {}
