package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.FactState;

public record AcceptanceDocumentView(
    UUID id,
    UUID deliveryId,
    LocalDate inspectedOn,
    UUID registeredById,
    Instant registeredAt,
    FactState state,
    UUID supersedesId,
    UUID correctionId
) {}
