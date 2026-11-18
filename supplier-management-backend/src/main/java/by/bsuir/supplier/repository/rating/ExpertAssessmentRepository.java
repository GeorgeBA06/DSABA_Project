package by.bsuir.supplier.repository.rating;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.rating.ExpertAssessment;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface ExpertAssessmentRepository extends JpaRepository<ExpertAssessment, UUID> {
    java.util.List<by.bsuir.supplier.entity.rating.ExpertAssessment> findBySupplier_Id(java.util.UUID supplierId);
}
