package by.bsuir.supplier.repository.audit;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.audit.AuditEvent;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
}
