package by.bsuir.supplier.repository.auth;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import by.bsuir.supplier.entity.auth.AuthSession;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AuthSession e where e.id = :id")
    Optional<AuthSession> findByIdForUpdate(@Param("id") UUID id);
    java.util.List<by.bsuir.supplier.entity.auth.AuthSession> findByUser_Id(java.util.UUID userId);
}
