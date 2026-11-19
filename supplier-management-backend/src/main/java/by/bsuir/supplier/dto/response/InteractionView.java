package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.InteractionChannel;

import java.time.Instant;
import java.util.UUID;

public record InteractionView(
        UUID id,
        UUID supplierId,
        UUID employeeId,
        Instant occurredAt,
        InteractionChannel channel,
        String subject,
        String content,
        String outcome,
        UUID claimId) {}
