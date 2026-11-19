package by.bsuir.supplier.service.rating;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.VersionState;
import by.bsuir.supplier.entity.rating.ExpertAssessment;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.repository.rating.ExpertAssessmentRepository;
import by.bsuir.supplier.repository.supplier.SupplierRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ExpertAssessmentService {
    private final ActorAccess access;
    private final SupplierRepository suppliers;
    private final ExpertAssessmentRepository assessments;
    private final AuditTrail audit;

    public ExpertAssessmentService(
            ActorAccess access,
            SupplierRepository suppliers,
            ExpertAssessmentRepository assessments,
            AuditTrail audit) {
        this.access = access;
        this.suppliers = suppliers;
        this.assessments = assessments;
        this.audit = audit;
    }

    @Transactional
    public ExpertAssessment create(
            UUID actorId,
            UUID supplierId,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate applicableFrom,
            LocalDate applicableTo,
            int score,
            String justification) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier supplier =
                suppliers
                        .findById(supplierId)
                        .orElseThrow(() -> new BusinessException("Поставщик не найден"));
        Rules.dates(periodStart, periodEnd);
        Rules.dates(applicableFrom, applicableTo);
        Rules.require(
                !applicableFrom.plusDays(29).isBefore(applicableTo),
                "Оценка применима максимум 30 дней");
        Rules.require(
                score == 0 || score == 25 || score == 50 || score == 75 || score == 100,
                "Допускаются баллы 0, 25, 50, 75, 100");
        ExpertAssessment a = new ExpertAssessment();
        a.setSupplier(supplier);
        a.setPeriodStart(periodStart);
        a.setPeriodEnd(periodEnd);
        a.setApplicableFrom(applicableFrom);
        a.setApplicableTo(applicableTo);
        a.setScore(score);
        a.setJustification(Rules.text(justification, "Обоснование"));
        a.setCreatedBy(actor);
        a.setState(VersionState.DRAFT);
        a = assessments.saveAndFlush(a);
        audit.record(actor, "EXPERT_ASSESSMENT_CREATED", a, "ExpertAssessment", a.getId(), null);
        return a;
    }

    @Transactional
    public ExpertAssessment submit(UUID actorId, UUID assessmentId) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        ExpertAssessment a = assessment(assessmentId);
        Rules.require(a.getState() == VersionState.DRAFT, "Оценка уже отправлена");
        a.setState(VersionState.PENDING);
        audit.record(actor, "EXPERT_ASSESSMENT_SUBMITTED", a, "ExpertAssessment", a.getId(), null);
        return a;
    }

    @Transactional
    public ExpertAssessment decide(UUID managerId, UUID assessmentId, boolean approve) {
        User manager = access.requireRole(managerId, RoleCode.PROCUREMENT_MANAGER);
        ExpertAssessment a = assessment(assessmentId);
        Rules.require(a.getState() == VersionState.PENDING, "Оценка не ожидает решения");
        ActorAccess.independent(a.getCreatedBy(), manager);
        suppliers
                .findByIdForUpdate(a.getSupplier().getId())
                .orElseThrow(() -> new BusinessException("Поставщик не найден"));
        if (approve) {
            boolean overlap =
                    assessments.findBySupplier_Id(a.getSupplier().getId()).stream()
                            .anyMatch(
                                    x ->
                                            x.getState() == VersionState.APPROVED
                                                    && !x.getId().equals(a.getId())
                                                    && !x.getApplicableTo()
                                                            .isBefore(a.getApplicableFrom())
                                                    && !x.getApplicableFrom()
                                                            .isAfter(a.getApplicableTo()));
            Rules.require(!overlap, "Период экспертной оценки пересекает уже утверждённую оценку");
        }
        a.setState(approve ? VersionState.APPROVED : VersionState.REJECTED);
        if (approve) {
            a.setApprovedBy(manager);
            a.setApprovedAt(Instant.now());
        }
        audit.record(
                manager,
                approve ? "EXPERT_ASSESSMENT_APPROVED" : "EXPERT_ASSESSMENT_REJECTED",
                a,
                "ExpertAssessment",
                a.getId(),
                null);
        return a;
    }

    private ExpertAssessment assessment(UUID id) {
        return assessments
                .findById(id)
                .orElseThrow(() -> new BusinessException("Экспертная оценка не найдена"));
    }
}
