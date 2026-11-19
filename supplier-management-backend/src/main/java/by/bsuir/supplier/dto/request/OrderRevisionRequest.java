package by.bsuir.supplier.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRevisionRequest(
        @NotEmpty List<@Valid OrderItemRequest> items, @NotBlank String reason) {}
