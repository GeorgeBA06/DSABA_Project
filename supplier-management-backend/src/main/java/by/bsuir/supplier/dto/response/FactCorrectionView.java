package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.VersionState;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FactCorrectionView(
        UUID id,
        String reason,
        VersionState state,
        LocalDate effectiveOn,
        UUID createdById,
        Instant createdAt,
        UUID decidedById,
        Instant decidedAt,
        String decisionComment) {}
