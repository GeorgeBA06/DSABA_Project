package by.bsuir.supplier.repository.supplier;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.supplier.Supplier;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    Page<Supplier> findByNameContainingIgnoreCase(String name, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Supplier s where s.id = :id")
    java.util.Optional<by.bsuir.supplier.entity.supplier.Supplier> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
