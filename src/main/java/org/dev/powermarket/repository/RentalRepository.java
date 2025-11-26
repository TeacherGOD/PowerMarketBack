package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.RentalRequest;
import org.dev.powermarket.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
