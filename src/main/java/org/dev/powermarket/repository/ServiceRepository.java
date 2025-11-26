package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Service;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.domain.enums.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}
