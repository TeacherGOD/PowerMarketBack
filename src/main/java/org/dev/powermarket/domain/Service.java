package org.dev.powermarket.domain;

import lombok.Getter;
import lombok.Setter;
import org.dev.powermarket.domain.enums.ServiceCategory;
import jakarta.persistence.*;
import org.dev.powermarket.security.entity.User;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "services")
public class Service {

    // Getters and setters
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private User supplier;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory category;

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "capacity")
    private String capacity; // Мощность/производительность

    @Column(name = "available_capacity")
    private String availableCapacity;

    @Column(name = "total_capacity_units")
    private Integer totalCapacityUnits = 1; // Общее количество единиц мощности

    @Column(name = "location")
    private String location;

    @Column(name = "technical_specs", columnDefinition = "TEXT")
    private String technicalSpecs;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

}
