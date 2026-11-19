package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UnitRequest(@NotBlank String code, @NotBlank String name) {}
