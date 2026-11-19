package by.bsuir.supplier.repository.supplier;

import by.bsuir.supplier.entity.supplier.SupplierStatusRequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface SupplierStatusRequestRepository
        extends JpaRepository<SupplierStatusRequest, UUID> {}
