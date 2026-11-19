package by.bsuir.supplier.dto.request;

import by.bsuir.supplier.entity.enums.InteractionChannel;

import jakarta.validation.constraints.NotNull;

public record SendClaimRequest(@NotNull InteractionChannel channel) {}
