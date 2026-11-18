package by.bsuir.supplier.repository.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.auth.User;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByLogin(String login);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.login = :login")
    Optional<User> findByLoginForUpdate(@org.springframework.data.repository.query.Param("login") String login);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    java.util.Optional<by.bsuir.supplier.entity.auth.User> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
