package by.bsuir.supplier.controller;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.request.AssessmentRequest;
import by.bsuir.supplier.dto.request.DecisionRequest;
import by.bsuir.supplier.dto.request.RatingRequest;
import by.bsuir.supplier.dto.response.ExpertAssessmentView;
import by.bsuir.supplier.dto.response.RatingLineInputView;
import by.bsuir.supplier.dto.response.SupplierRatingView;
import by.bsuir.supplier.mapper.RatingViewMapper;
import by.bsuir.supplier.service.query.RatingQueries;
import by.bsuir.supplier.service.rating.*;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
@PreAuthorize("hasAnyRole('PROCUREMENT_SPECIALIST','PROCUREMENT_MANAGER')")
public class RatingsController {
    private final RatingService service;
    private final ExpertAssessmentService assessments;
    private final RatingQueries read;

    public RatingsController(
            RatingService service, ExpertAssessmentService assessments, RatingQueries read) {
        this.service = service;
        this.assessments = assessments;
        this.read = read;
    }

    @GetMapping
    public PageResponse<SupplierRatingView> list(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listSupplierRating(ApiActor.id(a), page, size);
    }

    @GetMapping("/{id}")
    public SupplierRatingView detail(Authentication a, @PathVariable UUID id) {
        return read.getSupplierRating(ApiActor.id(a), id);
    }

    @GetMapping("/{id}/inputs")
    public List<RatingLineInputView> inputs(Authentication a, @PathVariable UUID id) {
        return read.lineInputs(ApiActor.id(a), id);
    }

    @GetMapping("/expert-assessments/{id}")
    public ExpertAssessmentView assessment(Authentication a, @PathVariable UUID id) {
        return read.getExpertAssessment(ApiActor.id(a), id);
    }

    @GetMapping("/supplier/{id}")
    public List<SupplierRatingView> supplier(Authentication a, @PathVariable UUID id) {
        return read.ratingsForSupplier(ApiActor.id(a), id);
    }

    @GetMapping("/expert-assessments")
    public PageResponse<ExpertAssessmentView> experts(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listExpertAssessment(ApiActor.id(a), page, size);
    }

    @PostMapping("/calculate")
    public SupplierRatingView calculate(Authentication a, @Valid @RequestBody RatingRequest d) {
        return RatingViewMapper.toView(
                service.calculate(ApiActor.id(a), d.supplierId(), d.scope(), d.categoryId()));
    }

    @PostMapping("/expert-assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpertAssessmentView assessment(
            Authentication a, @Valid @RequestBody AssessmentRequest d) {
        return RatingViewMapper.toView(
                assessments.create(
                        ApiActor.id(a),
                        d.supplierId(),
                        d.periodStart(),
                        d.periodEnd(),
                        d.applicableFrom(),
                        d.applicableTo(),
                        d.score(),
                        d.justification()));
    }

    @PostMapping("/expert-assessments/{id}/submit")
    public ExpertAssessmentView submit(Authentication a, @PathVariable UUID id) {
        return RatingViewMapper.toView(assessments.submit(ApiActor.id(a), id));
    }

    @PostMapping("/expert-assessments/{id}/decision")
    public ExpertAssessmentView decide(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody DecisionRequest d) {
        return RatingViewMapper.toView(assessments.decide(ApiActor.id(a), id, d.approve()));
    }
}
