package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.ExpertAssessmentView;
import by.bsuir.supplier.dto.response.RatingLineInputView;
import by.bsuir.supplier.dto.response.SupplierRatingView;
import by.bsuir.supplier.entity.rating.ExpertAssessment;
import by.bsuir.supplier.entity.rating.RatingLineInput;
import by.bsuir.supplier.entity.rating.SupplierRating;

/** Maps persistence entities to immutable public response DTOs. */
public final class RatingViewMapper {
    private RatingViewMapper() {}

    public static RatingLineInputView toView(RatingLineInput e) {
        return new RatingLineInputView(
                e.getId(),
                e.getRating() == null ? null : e.getRating().getId(),
                e.getOrderVersionLine() == null ? null : e.getOrderVersionLine().getId(),
                e.getSelectionReason(),
                e.getOrderedQty(),
                e.getDeliveredOnTimeQty(),
                e.getFulfilledQty(),
                e.getQualityCheckedQty(),
                e.getQualityGoodQty(),
                e.getTimelinessScore(),
                e.getCompletenessScore(),
                e.getQualityScore(),
                e.getSourceCutoffAt());
    }

    public static ExpertAssessmentView toView(ExpertAssessment e) {
        return new ExpertAssessmentView(
                e.getId(),
                e.getSupplier() == null ? null : e.getSupplier().getId(),
                e.getPeriodStart(),
                e.getPeriodEnd(),
                e.getApplicableFrom(),
                e.getApplicableTo(),
                e.getScore(),
                e.getJustification(),
                e.getState(),
                e.getCreatedBy() == null ? null : e.getCreatedBy().getId(),
                e.getCreatedAt(),
                e.getApprovedBy() == null ? null : e.getApprovedBy().getId(),
                e.getApprovedAt(),
                e.getSupersedes() == null ? null : e.getSupersedes().getId());
    }

    public static SupplierRatingView toView(SupplierRating e) {
        return new SupplierRatingView(
                e.getId(),
                e.getSupplier() == null ? null : e.getSupplier().getId(),
                e.getScope(),
                e.getCategory() == null ? null : e.getCategory().getId(),
                e.getPeriodStart(),
                e.getPeriodEnd(),
                e.getAsOfAt(),
                e.getMethodVersion(),
                e.getExpertAssessment() == null ? null : e.getExpertAssessment().getId(),
                e.getTimelinessScore(),
                e.getCompletenessScore(),
                e.getQualityScore(),
                e.getInteractionScore(),
                e.getTotalScore(),
                e.getBand(),
                e.getState(),
                e.getOrderCount(),
                e.getLineCount(),
                e.getCreatedAt(),
                e.getSupersedes() == null ? null : e.getSupersedes().getId());
    }
}
