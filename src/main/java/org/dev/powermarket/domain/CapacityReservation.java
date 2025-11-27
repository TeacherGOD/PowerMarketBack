package org.dev.powermarket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "capacity_reservations")
@Getter
@Setter
public class CapacityReservation {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_period_id", nullable = false)
    private ServiceAvailabilityPeriod availabilityPeriod;

    // Конкретная дата бронирования (в рамках периода)
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    // Сколько мощности забронировано на эту дату
    @Column(name = "reserved_capacity", precision = 10, scale = 2, nullable = false)
    private BigDecimal reservedCapacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

}
