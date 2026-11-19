package by.bsuir.supplier.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CorrectInspectionRequest(
        @NotNull LocalDate inspectedOn,
        @NotBlank String reason,
        @NotEmpty List<@Valid InspectionFixRequest> items) {}
