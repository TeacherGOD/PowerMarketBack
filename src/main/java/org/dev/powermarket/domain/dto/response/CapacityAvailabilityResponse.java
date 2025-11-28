package org.dev.powermarket.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CapacityAvailabilityResponse(
        LocalDate date,
        BigDecimal totalCapacity,
        BigDecimal availableCapacity,
        BigDecimal reservedCapacity,
        List<OccupiedSlot> occupiedSlots
) {
    public record OccupiedSlot(
            LocalDate startDate,
            LocalDate endDate,
            String tenantName,
            BigDecimal reservedCapacity
    ) {}
}
