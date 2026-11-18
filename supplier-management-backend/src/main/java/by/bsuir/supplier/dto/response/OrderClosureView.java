package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.ClosureAction;
import by.bsuir.supplier.entity.enums.ClosureResult;

public record OrderClosureView(
    UUID id,
    UUID orderId,
    ClosureAction action,
    ClosureResult result,
    String reason,
    UUID requestedById,
    UUID decidedById,
    Instant occurredAt
) {}
