package org.dev.powermarket.domain.dto.response;


import org.dev.powermarket.domain.enums.AvailabilityStatus;
import org.dev.powermarket.domain.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AvailableServiceResponse(
        // Basic service info
        UUID id,
        String title,
        String description,
        ServiceCategory category,
        BigDecimal pricePerDay,
        BigDecimal maxCapacity,
        String location,
        String technicalSpecs,
        UUID supplierId,
        String supplierName,
        Boolean active,
        BigDecimal averageRating,
        Integer totalReviews,
        Instant createdAt,

        // Availability info
        AvailabilityStatus availabilityStatus,
        BigDecimal availableCapacity,
        Boolean isAvailableForDates
) {}
