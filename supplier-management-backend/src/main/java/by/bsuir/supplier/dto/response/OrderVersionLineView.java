package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.OriginReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OrderVersionLineView(
        UUID id,
        UUID orderVersionId,
        UUID orderLineId,
        UUID productId,
        Integer lineNo,
        String segmentKey,
        UUID sourceVersionLineId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        LocalDate executionDueOn,
        LocalDate ratingDueOn,
        OriginReason originReason,
        String productNameSnapshot,
        String categoryCodeSnapshot,
        String categoryNameSnapshot,
        String unitCodeSnapshot) {}
