package org.dev.powermarket.service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dev.powermarket.domain.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto {
    private UUID id;
    private String title;
    private String description;
    private ServiceCategory category;
    private BigDecimal pricePerDay;
    private String location;
    private String totalCapacity;
    private String availableCapacity;
    private Integer totalCapacityUnits;
    private Integer availableCapacityUnits;
    private String technicalSpecs;
    private UUID supplierId;
    private String supplierName;
    private Boolean active;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Instant createdAt;
    private List<ServiceAvailabilityDto> availabilities;
}
