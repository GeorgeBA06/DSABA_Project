package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.SupplierOfferView;
import by.bsuir.supplier.dto.response.SupplierStatusRequestView;
import by.bsuir.supplier.dto.response.SupplierView;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.entity.supplier.SupplierProduct;
import by.bsuir.supplier.entity.supplier.SupplierStatusRequest;

/** Maps persistence entities to immutable public response DTOs. */
public final class SupplierViewMapper {
    private SupplierViewMapper() {
    }
    public static SupplierOfferView toView(SupplierProduct e) {
        return new SupplierOfferView(
            e.getSupplier().getId(),
            e.getProduct().getId(),
            e.getProduct().getName(),
            e.getSupplierItemCode(),
            e.getActive()
        );
    }
    public static SupplierView toView(Supplier e) {
        return new SupplierView(
            e.getId(),
            e.getName(),
            e.getContactName(),
            e.getEmail(),
            e.getPhone(),
            e.getAddress(),
            e.getRegistrationNumber(),
            e.getStatus(),
            e.getCreatedBy() == null?null:e.getCreatedBy().getId(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
    public static SupplierStatusRequestView toView(SupplierStatusRequest e) {
        return new SupplierStatusRequestView(
            e.getId(),
            e.getSupplier() == null?null:e.getSupplier().getId(),
            e.getFromStatus(),
            e.getToStatus(),
            e.getReason(),
            e.getRequestedBy() == null?null:e.getRequestedBy().getId(),
            e.getRequestedAt(),
            e.getState(),
            e.getDecidedBy() == null?null:e.getDecidedBy().getId(),
            e.getDecidedAt(),
            e.getDecisionComment()
        );
    }
}
