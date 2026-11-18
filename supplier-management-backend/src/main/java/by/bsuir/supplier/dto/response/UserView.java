package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import by.bsuir.supplier.entity.enums.UserStatus;

public record UserView(
    UUID id,
    String login,
    String displayName,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
