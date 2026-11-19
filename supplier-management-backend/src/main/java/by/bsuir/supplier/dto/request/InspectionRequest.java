package by.bsuir.supplier.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record InspectionRequest(
        @NotNull LocalDate inspectedOn, @NotEmpty List<@Valid InspectedItemRequest> items) {}
