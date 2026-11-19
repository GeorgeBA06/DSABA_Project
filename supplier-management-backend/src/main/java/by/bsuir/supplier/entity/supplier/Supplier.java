package by.bsuir.supplier.entity.supplier;

import by.bsuir.supplier.entity.auth.*;
import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Persisted aggregate/data record for suppliers. No business decisions belong here. */
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "contact_name", nullable = true, length = 150)
    private String contactName;

    @Column(name = "email", nullable = true, length = 250)
    private String email;

    @Column(name = "phone", nullable = true, length = 60)
    private String phone;

    @Column(name = "address", nullable = true, columnDefinition = "text")
    private String address;

    @Column(name = "registration_number", nullable = true, length = 80)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private SupplierStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    public Supplier() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String value) {
        this.contactName = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String value) {
        this.phone = value;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String value) {
        this.address = value;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String value) {
        this.registrationNumber = value;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierStatus value) {
        this.status = value;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User value) {
        this.createdBy = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant value) {
        this.updatedAt = value;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Long value) {
        this.lockVersion = value;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
