package by.bsuir.supplier.repository.supplier;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.supplier.SupplierStatusRequest;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface SupplierStatusRequestRepository extends JpaRepository<SupplierStatusRequest, UUID> {
}
