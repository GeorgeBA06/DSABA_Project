package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.ClosureAction;
import by.bsuir.supplier.entity.enums.ClosureResult;

import java.time.Instant;
import java.util.UUID;

public record OrderClosureView(
        UUID id,
        UUID orderId,
        ClosureAction action,
        ClosureResult result,
        String reason,
        UUID requestedById,
        UUID decidedById,
        Instant occurredAt) {}
