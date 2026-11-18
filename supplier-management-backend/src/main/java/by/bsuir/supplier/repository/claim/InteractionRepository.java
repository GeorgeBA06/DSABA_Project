package by.bsuir.supplier.repository.claim;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.claim.Interaction;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
}
