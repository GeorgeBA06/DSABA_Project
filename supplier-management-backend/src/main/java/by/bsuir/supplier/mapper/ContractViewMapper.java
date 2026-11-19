package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.ContractVersionView;
import by.bsuir.supplier.dto.response.ContractView;
import by.bsuir.supplier.entity.contract.Contract;
import by.bsuir.supplier.entity.contract.ContractVersion;

/** Maps persistence entities to immutable public response DTOs. */
public final class ContractViewMapper {
    private ContractViewMapper() {}

    public static ContractVersionView toView(ContractVersion e) {
        return new ContractVersionView(
                e.getId(),
                e.getContract() == null ? null : e.getContract().getId(),
                e.getVersionNo(),
                e.getNumber(),
                e.getSignedOn(),
                e.getValidFrom(),
                e.getValidTo(),
                e.getTerms(),
                e.getChangeReason(),
                e.getState(),
                e.getCreatedBy() == null ? null : e.getCreatedBy().getId(),
                e.getCreatedAt(),
                e.getDecidedBy() == null ? null : e.getDecidedBy().getId(),
                e.getDecidedAt(),
                e.getDecisionComment());
    }

    public static ContractView toView(Contract e) {
        return new ContractView(
                e.getId(),
                e.getSupplier() == null ? null : e.getSupplier().getId(),
                e.getState(),
                e.getCurrentRegisteredVersion() == null
                        ? null
                        : e.getCurrentRegisteredVersion().getId(),
                e.getCreatedBy() == null ? null : e.getCreatedBy().getId(),
                e.getCreatedAt());
    }
}
