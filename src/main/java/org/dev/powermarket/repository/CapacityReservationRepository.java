package org.dev.powermarket.repository;

import org.dev.powermarket.domain.CapacityReservation;
import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailabilityPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CapacityReservationRepository extends JpaRepository<CapacityReservation, UUID> {

    // Найти бронирования по rental
    List<CapacityReservation> findByRental(Rental rental);

    // Найти бронирования, пересекающиеся с периодом
    @Query("SELECT cr FROM CapacityReservation cr WHERE " +
            "cr.rental.service = :service AND " +
            "cr.startDate <= :endDate AND cr.endDate >= :startDate")
    List<CapacityReservation> findOverlappingReservations(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ✅ ДОБАВЛЯЕМ отсутствующий метод
    @Query("SELECT cr FROM CapacityReservation cr WHERE " +
            "cr.rental.service = :service AND " +
            "cr.startDate <= :endDate AND cr.endDate >= :startDate")
    List<CapacityReservation> findByServiceAndDateRange(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Найти сумму забронированной мощности на конкретную дату
    @Query("SELECT COALESCE(SUM(cr.reservedCapacity), 0) FROM CapacityReservation cr WHERE " +
            "cr.rental.service = :service AND " +
            ":date BETWEEN cr.startDate AND cr.endDate")
    BigDecimal getReservedCapacityForDate(
            @Param("service") Service service,
            @Param("date") LocalDate date);
}
