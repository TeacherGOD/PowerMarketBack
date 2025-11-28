package org.dev.powermarket.domain.dto.response;

import org.dev.powermarket.domain.enums.AvailabilityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ServiceAvailabilityResponse(
        UUID serviceId,
        String serviceTitle,
        AvailabilityStatus availabilityStatus,
        BigDecimal maxCapacity,
        List<AvailablePeriod> availablePeriods,
        List<ReservedPeriod> reservedPeriods
) {
    public record AvailablePeriod(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal availableCapacity
    ) {}

    public record ReservedPeriod(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal reservedCapacity,
            String tenantName
    ) {}
}