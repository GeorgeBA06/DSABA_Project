package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserView(
        UUID id,
        String login,
        String displayName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {}
