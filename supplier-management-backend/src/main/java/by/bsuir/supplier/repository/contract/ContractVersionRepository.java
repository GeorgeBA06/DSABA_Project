package by.bsuir.supplier.repository.contract;

import by.bsuir.supplier.entity.contract.ContractVersion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface ContractVersionRepository extends JpaRepository<ContractVersion, UUID> {
    java.util.List<by.bsuir.supplier.entity.contract.ContractVersion> findByContract_Id(
            java.util.UUID contractId);
}
