package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.RentalRequest;
import org.dev.powermarket.repository.projection.IncomeChartProjection;
import org.dev.powermarket.repository.projection.RentalSummaryProjection;
import org.dev.powermarket.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalRepository extends JpaRepository<Rental, UUID> {
    
    Page<Rental> findByTenant(User tenant, Pageable pageable);
    
    Page<Rental> findBySupplier(User supplier, Pageable pageable);
    
    @Query("SELECT r FROM Rental r WHERE r.supplier = :user OR r.tenant = :user")
    Page<Rental> findByUser(@Param("user") User user, Pageable pageable);
    
    Optional<Rental> findByRentalRequestId(UUID rentalRequestId);

    List<Rental> findBySupplierOrTenant(User user, User user1);

    Optional<Rental> findByRentalRequest(RentalRequest request);


    @Query("SELECT COUNT(r) FROM Rental r WHERE r.supplier = :supplier " +
            "AND r.startDate <= :to AND r.endDate >= :from " +
            "AND r.isActive = true " +
            "AND r.rentalRequest.status IN (org.dev.powermarket.domain.enums.RentalRequestStatus.IN_RENT, " +
            "org.dev.powermarket.domain.enums.RentalRequestStatus.CONFIRMED)")
    long countActiveRentalsBySupplierAndPeriod(@Param("supplier") User supplier,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    @Query("SELECT COUNT(r) as count, COALESCE(SUM(r.totalPrice), 0) as totalAmount FROM Rental r WHERE r.supplier = :supplier " +
            "AND r.rentalRequest.status = org.dev.powermarket.domain.enums.RentalRequestStatus.COMPLETED " +
            "AND r.endDate BETWEEN :from AND :to")
    RentalSummaryProjection countCompletedRentalsAndTotalAmountBySupplierAndPeriod(@Param("supplier") User supplier,
                                                                                   @Param("from") LocalDate from,
                                                                                   @Param("to") LocalDate to);

    @Query("SELECT EXTRACT(YEAR FROM r.endDate) as year, EXTRACT(MONTH FROM r.endDate) as month, " +
            "COALESCE(SUM(r.totalPrice), 0) as totalAmount, COUNT(r) as completedOrders " +
            "FROM Rental r WHERE r.supplier = :supplier " +
            "AND r.rentalRequest.status = org.dev.powermarket.domain.enums.RentalRequestStatus.COMPLETED " +
            "AND r.endDate BETWEEN :from AND :to " +
            "GROUP BY EXTRACT(YEAR FROM r.endDate), EXTRACT(MONTH FROM r.endDate)")
    List<IncomeChartProjection> getIncomeChartDataBySupplierAndPeriod(@Param("supplier") User supplier,
                                                                      @Param("from") LocalDate from,
                                                                      @Param("to") LocalDate to);
}
