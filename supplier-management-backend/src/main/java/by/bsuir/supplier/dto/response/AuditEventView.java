package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.AuditCategory;

public record AuditEventView(
    UUID id,
    UUID actorId,
    Instant occurredAt,
    AuditCategory category,
    String action,
    String entityType,
    UUID entityId,
    String reason,
    UUID correlationId
) {}
