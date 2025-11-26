package org.dev.powermarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "service_availability")
public class ServiceAvailability {

    // Getters and setters
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Column(name = "is_reserved", nullable = false)
    private Boolean isReserved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by_rental_id")
    private Rental reservedByRental;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

}
