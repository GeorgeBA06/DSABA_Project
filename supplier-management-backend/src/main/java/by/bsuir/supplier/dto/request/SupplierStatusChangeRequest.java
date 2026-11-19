package by.bsuir.supplier.dto.request;

import by.bsuir.supplier.entity.enums.SupplierStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplierStatusChangeRequest(
        @NotNull SupplierStatus status, @NotBlank String reason) {}
