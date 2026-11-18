package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.ClaimResult;
import by.bsuir.supplier.entity.enums.ClaimState;
import by.bsuir.supplier.entity.enums.InteractionChannel;
import by.bsuir.supplier.entity.enums.RemedyType;
import by.bsuir.supplier.entity.enums.ViolationType;

public record ClaimView(
    UUID id,
    UUID supplierId,
    String number,
    UUID orderLineId,
    UUID deliveryLineId,
    ViolationType violationType,
    ClaimState state,
    String description,
    String requirements,
    LocalDate responseDueOn,
    Instant sentAt,
    InteractionChannel sendChannel,
    UUID createdById,
    Instant createdAt,
    ClaimResult result,
    RemedyType remedyType,
    String resolutionDescription,
    Instant resolvedAt,
    UUID approvedById
) {}
