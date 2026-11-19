package by.bsuir.supplier.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ContractCreateRequest(
        @NotNull UUID supplierId, @NotNull @Valid ContractTermsRequest terms) {}
