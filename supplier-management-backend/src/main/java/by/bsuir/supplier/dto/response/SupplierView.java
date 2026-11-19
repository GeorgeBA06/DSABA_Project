package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.SupplierStatus;

import java.time.Instant;
import java.util.UUID;

public record SupplierView(
        UUID id,
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        String registrationNumber,
        SupplierStatus status,
        UUID createdById,
        Instant createdAt,
        Instant updatedAt) {}
