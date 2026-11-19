package by.bsuir.supplier.repository.auth;

import by.bsuir.supplier.entity.auth.UserRoleAssignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
    java.util.List<by.bsuir.supplier.entity.auth.UserRoleAssignment>
            findByUser_IdAndRevokedAtIsNull(java.util.UUID userId);
}
