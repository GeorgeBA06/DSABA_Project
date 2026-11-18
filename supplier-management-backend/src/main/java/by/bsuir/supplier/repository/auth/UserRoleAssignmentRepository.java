package by.bsuir.supplier.repository.auth;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.auth.UserRoleAssignment;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
    java.util.List<by.bsuir.supplier.entity.auth.UserRoleAssignment> findByUser_IdAndRevokedAtIsNull(java.util.UUID userId);
}
