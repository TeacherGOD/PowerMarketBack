package org.dev.powermarket.repository;

import org.dev.powermarket.domain.RentalRequest;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.domain.enums.RentalRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RentalRequestRepository extends JpaRepository<RentalRequest, UUID> {
    
    Page<RentalRequest> findByTenant(User tenant, Pageable pageable);
    
    @Query("SELECT rr FROM RentalRequest rr WHERE rr.service.supplier = :supplier")
    Page<RentalRequest> findBySupplier(@Param("supplier") User supplier, Pageable pageable);
    
    @Query("SELECT rr FROM RentalRequest rr WHERE rr.service.supplier = :supplier AND rr.status = :status")
    Page<RentalRequest> findBySupplierAndStatus(
        @Param("supplier") User supplier,
        @Param("status") RentalRequestStatus status,
        Pageable pageable);
    
    List<RentalRequest> findByServiceAndStatus(Service service, RentalRequestStatus status);
    
    long countByServiceSupplierAndStatus(User supplier, RentalRequestStatus status);
}
