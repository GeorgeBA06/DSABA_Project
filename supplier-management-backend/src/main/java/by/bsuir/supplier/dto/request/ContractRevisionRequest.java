package by.bsuir.supplier.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContractRevisionRequest(
        @NotNull @Valid ContractTermsRequest terms, @NotBlank String reason) {}
