package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.ContractState;

import java.time.Instant;
import java.util.UUID;

public record ContractView(
        UUID id,
        UUID supplierId,
        ContractState state,
        UUID currentRegisteredVersionId,
        UUID createdById,
        Instant createdAt) {}
