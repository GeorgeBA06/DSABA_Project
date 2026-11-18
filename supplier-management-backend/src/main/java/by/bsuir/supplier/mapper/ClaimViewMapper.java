package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.ClaimView;
import by.bsuir.supplier.dto.response.InteractionView;
import by.bsuir.supplier.entity.claim.Claim;
import by.bsuir.supplier.entity.claim.Interaction;

/** Maps persistence entities to immutable public response DTOs. */
public final class ClaimViewMapper {
    private ClaimViewMapper() {
    }
    public static ClaimView toView(Claim e) {
        return new ClaimView(
            e.getId(),
            e.getSupplier() == null?null:e.getSupplier().getId(),
            e.getNumber(),
            e.getOrderLine() == null?null:e.getOrderLine().getId(),
            e.getDeliveryLine() == null?null:e.getDeliveryLine().getId(),
            e.getViolationType(),
            e.getState(),
            e.getDescription(),
            e.getRequirements(),
            e.getResponseDueOn(),
            e.getSentAt(),
            e.getSendChannel(),
            e.getCreatedBy() == null?null:e.getCreatedBy().getId(),
            e.getCreatedAt(),
            e.getResult(),
            e.getRemedyType(),
            e.getResolutionDescription(),
            e.getResolvedAt(),
            e.getApprovedBy() == null?null:e.getApprovedBy().getId()
        );
    }
    public static InteractionView toView(Interaction e) {
        return new InteractionView(
            e.getId(),
            e.getSupplier() == null?null:e.getSupplier().getId(),
            e.getEmployee() == null?null:e.getEmployee().getId(),
            e.getOccurredAt(),
            e.getChannel(),
            e.getSubject(),
            e.getContent(),
            e.getOutcome(),
            e.getClaim() == null?null:e.getClaim().getId()
        );
    }
}
