package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.AcceptanceDocumentView;
import by.bsuir.supplier.dto.response.AcceptanceLineView;
import by.bsuir.supplier.dto.response.DeliveryLineView;
import by.bsuir.supplier.dto.response.DeliveryView;
import by.bsuir.supplier.dto.response.FactCorrectionView;
import by.bsuir.supplier.dto.response.SupplierReturnLineView;
import by.bsuir.supplier.dto.response.SupplierReturnView;
import by.bsuir.supplier.entity.fulfillment.AcceptanceDocument;
import by.bsuir.supplier.entity.fulfillment.AcceptanceLine;
import by.bsuir.supplier.entity.fulfillment.Delivery;
import by.bsuir.supplier.entity.fulfillment.DeliveryLine;
import by.bsuir.supplier.entity.fulfillment.FactCorrection;
import by.bsuir.supplier.entity.fulfillment.SupplierReturn;
import by.bsuir.supplier.entity.fulfillment.SupplierReturnLine;

/** Maps persistence entities to immutable public response DTOs. */
public final class FulfillmentViewMapper {
    private FulfillmentViewMapper() {
    }
    public static SupplierReturnLineView toView(SupplierReturnLine e) {
        return new SupplierReturnLineView(
            e.getId(),
            e.getSupplierReturn() == null?null:e.getSupplierReturn().getId(),
            e.getAcceptanceLine() == null?null:e.getAcceptanceLine().getId(),
            e.getQuantity(),
            e.getDefectDescription(),
            e.getClaim() == null?null:e.getClaim().getId()
        );
    }
    public static SupplierReturnView toView(SupplierReturn e) {
        return new SupplierReturnView(
            e.getId(),
            e.getOrder() == null?null:e.getOrder().getId(),
            e.getReturnedOn(),
            e.getState(),
            e.getReason(),
            e.getCreatedBy() == null?null:e.getCreatedBy().getId(),
            e.getApprovedBy() == null?null:e.getApprovedBy().getId(),
            e.getApprovedAt(),
            e.getSupersedes() == null?null:e.getSupersedes().getId(),
            e.getCorrection() == null?null:e.getCorrection().getId()
        );
    }
    public static AcceptanceDocumentView toView(AcceptanceDocument e) {
        return new AcceptanceDocumentView(
            e.getId(),
            e.getDelivery() == null?null:e.getDelivery().getId(),
            e.getInspectedOn(),
            e.getRegisteredBy() == null?null:e.getRegisteredBy().getId(),
            e.getRegisteredAt(),
            e.getState(),
            e.getSupersedes() == null?null:e.getSupersedes().getId(),
            e.getCorrection() == null?null:e.getCorrection().getId()
        );
    }
    public static DeliveryView toView(Delivery e) {
        return new DeliveryView(
            e.getId(),
            e.getOrder() == null?null:e.getOrder().getId(),
            e.getArrivedOn(),
            e.getRegisteredAt(),
            e.getRegisteredBy() == null?null:e.getRegisteredBy().getId(),
            e.getState(),
            e.getExternalNumber(),
            e.getSupersedes() == null?null:e.getSupersedes().getId(),
            e.getCorrection() == null?null:e.getCorrection().getId()
        );
    }
    public static FactCorrectionView toView(FactCorrection e) {
        return new FactCorrectionView(
            e.getId(),
            e.getReason(),
            e.getState(),
            e.getEffectiveOn(),
            e.getCreatedBy() == null?null:e.getCreatedBy().getId(),
            e.getCreatedAt(),
            e.getDecidedBy() == null?null:e.getDecidedBy().getId(),
            e.getDecidedAt(),
            e.getDecisionComment()
        );
    }
    public static DeliveryLineView toView(DeliveryLine e) {
        return new DeliveryLineView(
            e.getId(),
            e.getDelivery() == null?null:e.getDelivery().getId(),
            e.getOrderVersionLine() == null?null:e.getOrderVersionLine().getId(),
            e.getQuantity(),
            e.getReplacementForLine() == null?null:e.getReplacementForLine().getId(),
            e.getClaim() == null?null:e.getClaim().getId()
        );
    }
    public static AcceptanceLineView toView(AcceptanceLine e) {
        return new AcceptanceLineView(
            e.getId(),
            e.getAcceptanceDocument() == null?null:e.getAcceptanceDocument().getId(),
            e.getDeliveryLine() == null?null:e.getDeliveryLine().getId(),
            e.getAcceptedQty(),
            e.getQualityRejectedQty(),
            e.getOtherRejectedQty(),
            e.getOtherReason()
        );
    }
}
