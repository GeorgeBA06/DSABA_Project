package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.VersionState;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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
        String decisionComment) {}
