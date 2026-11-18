package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.InteractionChannel;

public record InteractionView(
    UUID id,
    UUID supplierId,
    UUID employeeId,
    Instant occurredAt,
    InteractionChannel channel,
    String subject,
    String content,
    String outcome,
    UUID claimId
) {}
