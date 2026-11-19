package by.bsuir.supplier.dto.response;

import java.util.UUID;

/** Response after adding an item to a supplier assortment. */
public record OfferView(UUID supplierId, UUID productId, String supplierItemCode, Boolean active) {}
