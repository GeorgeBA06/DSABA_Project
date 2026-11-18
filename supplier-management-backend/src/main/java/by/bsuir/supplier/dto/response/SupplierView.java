package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.SupplierStatus;

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
    Instant updatedAt
) {}
