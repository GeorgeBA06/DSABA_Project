package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.AuditEventView;
import by.bsuir.supplier.entity.audit.AuditEvent;

/** Maps persistence entities to immutable public response DTOs. */
public final class AuditViewMapper {
    private AuditViewMapper() {
    }
    public static AuditEventView toView(AuditEvent e) {
        return new AuditEventView(
            e.getId(),
            e.getActor() == null?null:e.getActor().getId(),
            e.getOccurredAt(),
            e.getCategory(),
            e.getAction(),
            e.getEntityType(),
            e.getEntityId(),
            e.getReason(),
            e.getCorrelationId()
        );
    }
}
