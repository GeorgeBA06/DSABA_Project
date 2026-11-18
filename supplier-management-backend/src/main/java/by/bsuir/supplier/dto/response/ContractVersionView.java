package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.VersionState;

public record ContractVersionView(
    UUID id,
    UUID contractId,
    Integer versionNo,
    String number,
    LocalDate signedOn,
    LocalDate validFrom,
    LocalDate validTo,
    String terms,
    String changeReason,
    VersionState state,
    UUID createdById,
    Instant createdAt,
    UUID decidedById,
    Instant decidedAt,
    String decisionComment
) {}
