package by.bsuir.supplier.service.common;

import by.bsuir.supplier.entity.audit.AuditEvent;
import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.enums.AuditCategory;
import by.bsuir.supplier.repository.audit.AuditEventRepository;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Persisted in the caller's business transaction. Only minimal, non-sensitive metadata. */
@Service
public class AuditTrail {
    private final AuditEventRepository repo;

    public AuditTrail(AuditEventRepository repo) {
        this.repo = repo;
    }

    public void record(
            User actor,
            String action,
            Object entity,
            String entityType,
            UUID entityId,
            String reason) {
        Rules.present(entityId, "Идентификатор события");
        returnRecord(actor, action, entityType, entityId, reason, AuditCategory.BUSINESS);
    }

    public void security(
            User actor, String action, String entityType, UUID entityId, String reason) {
        returnRecord(actor, action, entityType, entityId, reason, AuditCategory.SECURITY);
    }

    private void returnRecord(
            User actor,
            String action,
            String entityType,
            UUID entityId,
            String reason,
            AuditCategory category) {
        AuditEvent e = new AuditEvent();
        e.setActor(actor);
        e.setCategory(category);
        e.setAction(action);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setOccurredAt(Instant.now());
        e.setCorrelationId(UUID.randomUUID());
        e.setReason(reason);
        repo.save(e);
    }
}
