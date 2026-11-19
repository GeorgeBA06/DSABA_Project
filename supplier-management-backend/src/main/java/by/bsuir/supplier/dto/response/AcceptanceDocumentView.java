package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.FactState;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AcceptanceDocumentView(
        UUID id,
        UUID deliveryId,
        LocalDate inspectedOn,
        UUID registeredById,
        Instant registeredAt,
        FactState state,
        UUID supersedesId,
        UUID correctionId) {}
