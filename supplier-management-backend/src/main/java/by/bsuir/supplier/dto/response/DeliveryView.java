package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.FactState;

public record DeliveryView(
    UUID id,
    UUID orderId,
    LocalDate arrivedOn,
    Instant registeredAt,
    UUID registeredById,
    FactState state,
    String externalNumber,
    UUID supersedesId,
    UUID correctionId
) {}
