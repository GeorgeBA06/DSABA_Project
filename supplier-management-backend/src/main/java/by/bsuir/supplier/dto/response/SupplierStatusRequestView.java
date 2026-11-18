package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.DecisionState;
import by.bsuir.supplier.entity.enums.SupplierStatus;

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
    String decisionComment
) {}
