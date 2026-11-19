package by.bsuir.supplier.dto.request;

import by.bsuir.supplier.entity.enums.InteractionChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record InteractionRequest(
        @NotNull UUID supplierId,
        UUID claimId,
        @NotNull InteractionChannel channel,
        @NotBlank String subject,
        @NotBlank String content,
        String outcome,
        Instant occurredAt) {}
