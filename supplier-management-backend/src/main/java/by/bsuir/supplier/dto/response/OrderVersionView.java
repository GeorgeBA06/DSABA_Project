package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.Initiator;
import by.bsuir.supplier.entity.enums.OrderVersionKind;
import by.bsuir.supplier.entity.enums.VersionState;

public record OrderVersionView(
    UUID id,
    UUID orderId,
    Integer versionNo,
    UUID baseVersionId,
    UUID contractVersionId,
    OrderVersionKind kind,
    Initiator initiator,
    String reason,
    VersionState state,
    UUID createdById,
    Instant createdAt,
    Instant submittedAt,
    UUID decidedById,
    Instant decidedAt,
    String decisionComment
) {}
