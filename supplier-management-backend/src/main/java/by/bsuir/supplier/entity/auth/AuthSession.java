package by.bsuir.supplier.entity.auth;

import jakarta.persistence.*;
import java.util.UUID;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import by.bsuir.supplier.entity.enums.*;

/** Persisted aggregate/data record for auth_sessions. No business decisions belong here. */
@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "token_generation", nullable = false)
    private Long tokenGeneration;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = true)
    private Instant revokedAt;

    @Column(name = "rotated_at", nullable = true)
    private Instant rotatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    public AuthSession() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public User getUser() { return user; }
    public void setUser(User value) { this.user = value; }

    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String value) { this.refreshTokenHash = value; }

    public Long getTokenGeneration() { return tokenGeneration; }
    public void setTokenGeneration(Long value) { this.tokenGeneration = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant value) { this.revokedAt = value; }

    public Instant getRotatedAt() { return rotatedAt; }
    public void setRotatedAt(Instant value) { this.rotatedAt = value; }

    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long value) { this.lockVersion = value; }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

}
