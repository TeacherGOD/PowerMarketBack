package org.dev.powermarket.repository;

import jakarta.validation.constraints.NotNull;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.domain.enums.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface ServiceRepository extends JpaRepository<Service, UUID> {
    
    Page<Service> findBySupplierAndDeletedFalse(User supplier, Pageable pageable);
    
    Page<Service> findByIsActiveTrueAndDeletedFalse(Pageable pageable);
    
    Page<Service> findByIsActiveTrueAndCategoryAndDeletedFalse(ServiceCategory category, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.isActive = true AND s.deleted = false AND " +
            "(LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Service> searchServices(@Param("search") String search, Pageable pageable);
    
    List<Service> findBySupplierAndIsActiveTrueAndDeletedFalse(User supplier);

    List<Service> findByIdInAndIsActiveTrueAndDeletedFalse(List<UUID> ids);

    // Поиск сервисов с минимальной мощностью
    @Query("SELECT s FROM Service s WHERE " +
            "s.isActive = true AND s.deleted = false AND " +
            "(:minCapacity IS NULL OR s.maxCapacity >= :minCapacity)")
    Page<Service> findByMinCapacity(
            @Param("minCapacity") BigDecimal minCapacity,
            Pageable pageable);



    @Query("SELECT s FROM Service s WHERE " +
            "s.isActive = true AND s.deleted = false AND " +
            "(:category IS NULL OR s.category = :category) AND " +
            "s.maxCapacity >= :minCapacity")
    Page<Service> findByCategoryAndMinCapacity(
            @Param("category") ServiceCategory category,
            @Param("minCapacity") BigDecimal minCapacity,
            Pageable pageable);

    // Поиск по категории, минимальной мощности и местоположению
    @Query("SELECT s FROM Service s WHERE " +
            "s.isActive = true AND s.deleted = false AND " +
            "(:category IS NULL OR s.category = :category) AND " +
            "s.maxCapacity >= :minCapacity AND " +
            "(:location IS NULL OR LOWER(s.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    Page<Service> findByCategoryMinCapacityAndLocation(
            @Param("category") ServiceCategory category,
            @Param("minCapacity") BigDecimal minCapacity,
            @Param("location") String location,
            Pageable pageable);

    // Поиск сервисов, которые имеют периоды доступности в указанном диапазоне
    @Query("SELECT DISTINCT s FROM Service s " +
            "JOIN ServiceAvailabilityPeriod p ON p.service = s " +
            "WHERE s.isActive = true AND s.deleted = false AND " +
            "(:category IS NULL OR s.category = :category) AND " +
            "s.maxCapacity >= :minCapacity AND " +
            "p.startDate <= :availableTo AND p.endDate >= :availableFrom")
    Page<Service> findAvailableServicesByCategoryAndCapacity(
            @Param("category") ServiceCategory category,
            @Param("minCapacity") BigDecimal minCapacity,
            @Param("availableFrom") LocalDate availableFrom,
            @Param("availableTo") LocalDate availableTo,
            Pageable pageable);
}
