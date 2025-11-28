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

    // Найти бронирования для rental
    List<CapacityReservation> findByRental(Rental rental);

    // Найти бронирования по дате и сервису
    @Query("SELECT cr FROM CapacityReservation cr WHERE " +
            "cr.availabilityPeriod.service = :service AND " +
            "cr.reservationDate BETWEEN :startDate AND :endDate")
    List<CapacityReservation> findByServiceAndDateRange(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Получить сумму забронированной мощности на конкретную дату
    @Query("SELECT COALESCE(SUM(cr.reservedCapacity), 0) FROM CapacityReservation cr WHERE " +
            "cr.availabilityPeriod.service = :service AND " +
            "cr.reservationDate = :date")
    BigDecimal getReservedCapacityForDate(
            @Param("service") Service service,
            @Param("date") LocalDate date);


    // Получить сумму забронированной мощности на дату в рамках периода
    @Query("SELECT COALESCE(SUM(cr.reservedCapacity), 0) FROM CapacityReservation cr WHERE " +
            "cr.availabilityPeriod = :period AND " +
            "cr.reservationDate = :date")
    BigDecimal getReservedCapacityForPeriodAndDate(
            @Param("period") ServiceAvailabilityPeriod period,
            @Param("date") LocalDate date);
}
