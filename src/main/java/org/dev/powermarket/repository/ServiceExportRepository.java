package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceExportRepository extends JpaRepository<Service, UUID> {
    @Query("SELECT l FROM Service l WHERE l.supplier.id = :supplierId AND l.createdAt BETWEEN :from AND :to")
    List<Service> getLotsByCreatedAt(@Param("supplierId") UUID supplierId,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);
}
