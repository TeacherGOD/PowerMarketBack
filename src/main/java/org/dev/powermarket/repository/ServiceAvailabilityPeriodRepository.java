package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailabilityPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceAvailabilityPeriodRepository extends JpaRepository<ServiceAvailabilityPeriod, UUID> {

    @Query("SELECT p FROM ServiceAvailabilityPeriod p WHERE " +
            "p.service = :service AND " +
            "p.startDate <= :date AND p.endDate >= :date")
    Optional<ServiceAvailabilityPeriod> findByServiceAndDate(
            @Param("service") Service service,
            @Param("date") LocalDate date);

    @Query("SELECT p FROM ServiceAvailabilityPeriod p WHERE " +
            "p.service = :service AND " +
            "p.startDate <= :endDate AND p.endDate >= :startDate")
    List<ServiceAvailabilityPeriod> findOverlappingPeriods(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ✅ ДОБАВЛЯЕМ метод для поиска периодов в диапазоне
    @Query("SELECT p FROM ServiceAvailabilityPeriod p WHERE " +
            "p.service = :service AND " +
            "p.startDate <= :endDate AND p.endDate >= :startDate " +
            "ORDER BY p.startDate")
    List<ServiceAvailabilityPeriod> findPeriodsInRange(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
