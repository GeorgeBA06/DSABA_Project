package by.bsuir.supplier.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CorrectReceiptRequest(
        @NotNull LocalDate arrivedOn,
        @NotBlank String reason,
        @NotEmpty List<@Valid ReceiptFixRequest> items) {}
