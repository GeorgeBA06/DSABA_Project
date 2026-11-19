package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String code, @NotBlank String name) {}
