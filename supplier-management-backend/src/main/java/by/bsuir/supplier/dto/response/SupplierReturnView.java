package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.ReturnState;

public record SupplierReturnView(
    UUID id,
    UUID orderId,
    LocalDate returnedOn,
    ReturnState state,
    String reason,
    UUID createdById,
    UUID approvedById,
    Instant approvedAt,
    UUID supersedesId,
    UUID correctionId
) {}
