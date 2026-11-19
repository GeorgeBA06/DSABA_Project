package by.bsuir.supplier.entity.auth;

import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Persisted aggregate/data record for user_role_assignments. No business decisions belong here. */
@Entity
@Table(name = "user_role_assignments")
public class UserRoleAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 40)
    private RoleCode roleCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "revoked_at", nullable = true)
    private Instant revokedAt;

    public UserRoleAssignment() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User value) {
        this.user = value;
    }

    public RoleCode getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(RoleCode value) {
        this.roleCode = value;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User value) {
        this.assignedBy = value;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant value) {
        this.assignedAt = value;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant value) {
        this.revokedAt = value;
    }
}
