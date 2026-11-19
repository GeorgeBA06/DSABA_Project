package by.bsuir.supplier.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OfferRequest(@NotNull UUID productId, String supplierItemCode) {}
