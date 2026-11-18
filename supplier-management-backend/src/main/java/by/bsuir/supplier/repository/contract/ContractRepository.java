package by.bsuir.supplier.repository.contract;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.contract.Contract;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Contract c where c.id = :id")
    java.util.Optional<by.bsuir.supplier.entity.contract.Contract> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
