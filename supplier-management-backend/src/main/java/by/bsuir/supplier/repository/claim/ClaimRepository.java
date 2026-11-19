package by.bsuir.supplier.repository.claim;

import by.bsuir.supplier.entity.claim.Claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface ClaimRepository extends JpaRepository<Claim, UUID> {}
