package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.AuditCategory;

import java.time.Instant;
import java.util.UUID;

public record AuditEventView(
        UUID id,
        UUID actorId,
        Instant occurredAt,
        AuditCategory category,
        String action,
        String entityType,
        UUID entityId,
        String reason,
        UUID correlationId) {}
