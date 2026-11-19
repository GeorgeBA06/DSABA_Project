package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.VersionState;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpertAssessmentView(
        UUID id,
        UUID supplierId,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate applicableFrom,
        LocalDate applicableTo,
        Integer score,
        String justification,
        VersionState state,
        UUID createdById,
        Instant createdAt,
        UUID approvedById,
        Instant approvedAt,
        UUID supersedesId) {}
