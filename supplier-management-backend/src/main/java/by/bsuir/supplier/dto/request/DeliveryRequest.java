package by.bsuir.supplier.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record DeliveryRequest(@NotNull LocalDate arrivedOn, String externalNumber, @NotEmpty List<@Valid ReceivedItemRequest> items) {}
