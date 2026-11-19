package by.bsuir.supplier.entity.enums;

/** Fixed codes persisted as strings (never ordinal numbers). */
public enum OrderState {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
