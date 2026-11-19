package by.bsuir.supplier.dto.request;

import by.bsuir.supplier.entity.enums.ViolationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ClaimRequest(
        @NotBlank String number,
        @NotNull UUID supplierId,
        @NotNull UUID orderLineId,
        UUID deliveryLineId,
        @NotNull ViolationType type,
        @NotBlank String description,
        @NotBlank String requirements,
        LocalDate responseDueOn) {}
