package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.SelectionReason;

public record RatingLineInputView(
    UUID id,
    UUID ratingId,
    UUID orderVersionLineId,
    SelectionReason selectionReason,
    BigDecimal orderedQty,
    BigDecimal deliveredOnTimeQty,
    BigDecimal fulfilledQty,
    BigDecimal qualityCheckedQty,
    BigDecimal qualityGoodQty,
    BigDecimal timelinessScore,
    BigDecimal completenessScore,
    BigDecimal qualityScore,
    Instant sourceCutoffAt
) {}
