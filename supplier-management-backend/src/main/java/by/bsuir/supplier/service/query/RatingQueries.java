package by.bsuir.supplier.service.query;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.ExpertAssessmentView;
import by.bsuir.supplier.dto.response.RatingLineInputView;
import by.bsuir.supplier.dto.response.SupplierRatingView;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.rating.ExpertAssessment;
import by.bsuir.supplier.entity.rating.RatingLineInput;
import by.bsuir.supplier.entity.rating.SupplierRating;
import by.bsuir.supplier.mapper.RatingViewMapper;
import by.bsuir.supplier.repository.rating.ExpertAssessmentRepository;
import by.bsuir.supplier.repository.rating.RatingLineInputRepository;
import by.bsuir.supplier.repository.rating.SupplierRatingRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.BusinessException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RatingQueries {
    private final ActorAccess access;
    private final SupplierRatingRepository supplierRatingRepo;
    private final ExpertAssessmentRepository expertAssessmentRepo;
    private final RatingLineInputRepository ratingLineInputRepo;

    public RatingQueries(
            ActorAccess access,
            SupplierRatingRepository supplierRatingRepo,
            ExpertAssessmentRepository expertAssessmentRepo,
            RatingLineInputRepository ratingLineInputRepo) {
        this.access = access;
        this.supplierRatingRepo = supplierRatingRepo;
        this.expertAssessmentRepo = expertAssessmentRepo;
        this.ratingLineInputRepo = ratingLineInputRepo;
    }

    @Transactional(readOnly = true)
    public List<RatingLineInputView> lineInputs(UUID actorId, UUID ratingId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        supplierRatingRepo
                .findById(ratingId)
                .orElseThrow(() -> new BusinessException("Рейтинг не найден"));
        return ratingLineInputRepo.findByRating_Id(ratingId).stream()
                .map(RatingViewMapper::toView)
                .toList();
    }

    private static Pageable pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public SupplierRatingView getSupplierRating(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return RatingViewMapper.toView(
                supplierRatingRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierRatingView> listSupplierRating(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<SupplierRating> result = supplierRatingRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(RatingViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public ExpertAssessmentView getExpertAssessment(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return RatingViewMapper.toView(
                expertAssessmentRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpertAssessmentView> listExpertAssessment(
            UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<ExpertAssessment> result = expertAssessmentRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(RatingViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public RatingLineInputView getRatingLineInput(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return RatingViewMapper.toView(
                ratingLineInputRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<RatingLineInputView> listRatingLineInput(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<RatingLineInput> result = ratingLineInputRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(RatingViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public List<SupplierRatingView> ratingsForSupplier(UUID actorId, UUID supplierId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return supplierRatingRepo.findBySupplier_Id(supplierId).stream()
                .map(RatingViewMapper::toView)
                .toList();
    }
}
