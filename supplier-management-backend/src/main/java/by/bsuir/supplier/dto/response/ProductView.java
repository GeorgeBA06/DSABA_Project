package by.bsuir.supplier.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductView(
        UUID id,
        String sku,
        String name,
        UUID categoryId,
        UUID unitId,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
