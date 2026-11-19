package by.bsuir.supplier.dto.request;

import by.bsuir.supplier.entity.enums.RoleCode;

import jakarta.validation.constraints.NotNull;

public record RoleRequest(@NotNull RoleCode role) {}
