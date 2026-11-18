package by.bsuir.supplier.repository.rating;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.rating.RatingLineInput;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface RatingLineInputRepository extends JpaRepository<RatingLineInput, UUID> {
    java.util.List<by.bsuir.supplier.entity.rating.RatingLineInput> findByRating_Id(java.util.UUID ratingId);
}
