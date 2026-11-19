package by.bsuir.supplier.repository.catalog;

import by.bsuir.supplier.entity.catalog.Unit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface UnitRepository extends JpaRepository<Unit, UUID> {}
