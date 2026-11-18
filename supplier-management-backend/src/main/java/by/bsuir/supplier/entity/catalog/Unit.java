package by.bsuir.supplier.entity.catalog;

import jakarta.persistence.*;
import java.util.UUID;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import by.bsuir.supplier.entity.enums.*;

/** Persisted aggregate/data record for units. No business decisions belong here. */
@Entity
@Table(name = "units")
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public Unit() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public String getCode() { return code; }
    public void setCode(String value) { this.code = value; }

    public String getName() { return name; }
    public void setName(String value) { this.name = value; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { this.active = value; }

}
