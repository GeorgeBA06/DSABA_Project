package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReasonRequest(@NotBlank String reason) {}
