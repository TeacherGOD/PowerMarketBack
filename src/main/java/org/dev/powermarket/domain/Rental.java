package org.dev.powermarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.dev.powermarket.security.entity.User;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "rentals")
public class Rental {

    // Getters and setters
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;



    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_request_id", nullable = false)
    private RentalRequest rentalRequest;

    @OneToMany(mappedBy = "rental", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CapacityReservation> capacityReservations = new ArrayList<>();

    // Запрашиваемая мощность
    @Column(name = "capacity_needed", precision = 10, scale = 2, nullable = false)
    private BigDecimal capacityNeeded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private User supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "supplier_confirmed", nullable = false)
    private Boolean supplierConfirmed = false;

    @Column(name = "tenant_confirmed", nullable = false)
    private Boolean tenantConfirmed = false;

    @Column(name = "supplier_confirmed_at")
    private Instant supplierConfirmedAt;

    @Column(name = "tenant_confirmed_at")
    private Instant tenantConfirmedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToOne(mappedBy = "rental", cascade = CascadeType.ALL)
    private Chat chat;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

}
