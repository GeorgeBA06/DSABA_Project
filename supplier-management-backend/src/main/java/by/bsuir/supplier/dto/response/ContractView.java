package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.ContractState;

public record ContractView(
    UUID id,
    UUID supplierId,
    ContractState state,
    UUID currentRegisteredVersionId,
    UUID createdById,
    Instant createdAt
) {}
