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

    // Найти периоды с достаточной доступной мощностью
    @Query("SELECT p FROM ServiceAvailabilityPeriod p WHERE " +
            "p.service = :service AND " +
            "p.startDate <= :endDate AND p.endDate >= :startDate AND " +
            "p.availableCapacity >= :requiredCapacity")
    List<ServiceAvailabilityPeriod> findAvailablePeriodsWithCapacity(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("requiredCapacity") BigDecimal requiredCapacity);

    // Найти все пересекающиеся периоды
    @Query("SELECT p FROM ServiceAvailabilityPeriod p WHERE " +
            "p.service = :service AND " +
            "p.startDate <= :endDate AND p.endDate >= :startDate")
    List<ServiceAvailabilityPeriod> findOverlappingPeriods(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    // Найти период по сервису и конечной дате
    Optional<ServiceAvailabilityPeriod> findByServiceAndEndDate(Service service, LocalDate endDate);

    // Найти период по сервису и начальной дате
    Optional<ServiceAvailabilityPeriod> findByServiceAndStartDate(Service service, LocalDate startDate);

    // Найти период по сервису, начальной и конечной дате
    Optional<ServiceAvailabilityPeriod> findByServiceAndStartDateAndEndDate(
            Service service, LocalDate startDate, LocalDate endDate);

    // Найти все периоды для сервиса в диапазоне дат (для отладки)
    List<ServiceAvailabilityPeriod> findByServiceAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Service service, LocalDate endDate, LocalDate startDate);
}
