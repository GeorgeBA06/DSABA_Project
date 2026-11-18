package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.VersionState;

public record FactCorrectionView(
    UUID id,
    String reason,
    VersionState state,
    LocalDate effectiveOn,
    UUID createdById,
    Instant createdAt,
    UUID decidedById,
    Instant decidedAt,
    String decisionComment
) {}
