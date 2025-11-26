package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceAvailabilityRepository extends JpaRepository<ServiceAvailability, UUID> {

    List<ServiceAvailability> findByServiceAndIsReservedFalse(Service service);

    List<ServiceAvailability> findByServiceAndAvailableDateBetweenAndIsReservedFalse(
            Service service, LocalDate startDate, LocalDate endDate);

    boolean existsByServiceAndAvailableDateAndIsReservedFalse(Service service, LocalDate date);

    List<ServiceAvailability> findByService(Service service);

    @Query("SELECT sa FROM ServiceAvailability sa WHERE sa.service = :service " +
            "AND sa.availableDate BETWEEN :startDate AND :endDate")
    List<ServiceAvailability> findByServiceAndDateRange(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(sa) = :expectedDays FROM ServiceAvailability sa " +
            "WHERE sa.service = :service " +
            "AND sa.availableDate BETWEEN :startDate AND :endDate " +
            "AND sa.isReserved = false")
    boolean isDateRangeAvailable(
            @Param("service") Service service,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("expectedDays") long expectedDays);

    void deleteByService(Service service);

    @Query("SELECT a FROM ServiceAvailability a " +
            "WHERE a.service = :service " +
            "AND a.availableDate BETWEEN :startDate AND :endDate " +
            "AND a.isReserved = false")
    List<ServiceAvailability> findOverlappingAvailabilities(@Param("service") Service service,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    @Query("SELECT sa FROM ServiceAvailability sa " +
            "WHERE sa.service = :service " +
            "AND sa.availableDate BETWEEN :startDate AND :endDate " +
            "AND sa.isReserved = true")
    List<ServiceAvailability> findReservedAvailabilities(@Param("service") Service service,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);
}
