package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RescheduleRequest(
        @NotBlank String segmentKey, @NotNull LocalDate newDueOn, @NotBlank String reason) {}
