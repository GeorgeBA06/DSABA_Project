package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.FactState;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeliveryView(
        UUID id,
        UUID orderId,
        LocalDate arrivedOn,
        Instant registeredAt,
        UUID registeredById,
        FactState state,
        String externalNumber,
        UUID supersedesId,
        UUID correctionId) {}
