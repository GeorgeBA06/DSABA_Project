package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ContractTermsRequest(
        @NotBlank String number,
        LocalDate signedOn,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validTo,
        @NotBlank String terms) {}
