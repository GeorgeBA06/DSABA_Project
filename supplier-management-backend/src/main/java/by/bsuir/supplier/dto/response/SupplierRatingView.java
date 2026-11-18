package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.RatingBand;
import by.bsuir.supplier.entity.enums.RatingScope;
import by.bsuir.supplier.entity.enums.RatingState;

public record SupplierRatingView(
    UUID id,
    UUID supplierId,
    RatingScope scope,
    UUID categoryId,
    LocalDate periodStart,
    LocalDate periodEnd,
    Instant asOfAt,
    String methodVersion,
    UUID expertAssessmentId,
    BigDecimal timelinessScore,
    BigDecimal completenessScore,
    BigDecimal qualityScore,
    BigDecimal interactionScore,
    BigDecimal totalScore,
    RatingBand band,
    RatingState state,
    Integer orderCount,
    Integer lineCount,
    Instant createdAt,
    UUID supersedesId
) {}
