package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.RatingBand;
import by.bsuir.supplier.entity.enums.RatingState;
import by.bsuir.supplier.entity.enums.SupplierStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierReportRow(
        UUID supplierId,
        String name,
        SupplierStatus status,
        BigDecimal latestScore,
        RatingBand band,
        RatingState ratingState) {}
