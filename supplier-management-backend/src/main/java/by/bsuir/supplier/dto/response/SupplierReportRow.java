package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.RatingBand;
import by.bsuir.supplier.entity.enums.RatingState;
import by.bsuir.supplier.entity.enums.SupplierStatus;

public record SupplierReportRow(
    UUID supplierId,
    String name,
    SupplierStatus status,
    BigDecimal latestScore,
    RatingBand band,
    RatingState ratingState
) {}
