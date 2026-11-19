package by.bsuir.supplier.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderItemsRequest(@NotEmpty List<@Valid OrderItemRequest> items) {}
