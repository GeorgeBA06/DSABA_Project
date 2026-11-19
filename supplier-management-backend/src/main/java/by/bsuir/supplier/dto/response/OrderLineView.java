package by.bsuir.supplier.dto.response;

import java.util.UUID;

public record OrderLineView(UUID id, UUID orderId, Integer lineNo, UUID productId) {}
