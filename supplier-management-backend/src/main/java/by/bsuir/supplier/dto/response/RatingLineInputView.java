package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.SelectionReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
        Instant sourceCutoffAt) {}
