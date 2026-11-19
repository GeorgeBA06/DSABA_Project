package by.bsuir.supplier.repository.audit;

import by.bsuir.supplier.entity.audit.AuditEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {}
