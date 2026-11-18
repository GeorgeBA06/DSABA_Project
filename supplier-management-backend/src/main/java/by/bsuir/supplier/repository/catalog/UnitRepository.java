package by.bsuir.supplier.repository.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.catalog.Unit;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface UnitRepository extends JpaRepository<Unit, UUID> {
}
