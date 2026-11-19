package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.ReturnState;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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
        UUID correctionId) {}
