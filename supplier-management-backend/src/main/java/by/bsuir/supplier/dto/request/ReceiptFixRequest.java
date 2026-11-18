package by.bsuir.supplier.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ReceiptFixRequest(@NotNull UUID approvedSegmentId, @NotNull @DecimalMin("0.001") BigDecimal quantity) {}
