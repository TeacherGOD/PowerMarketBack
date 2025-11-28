package org.dev.powermarket.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapacityAvailabilityDto {
    private LocalDate date;
    private BigDecimal totalCapacity;
    private BigDecimal availableCapacity;
    private BigDecimal reservedCapacity;
    private List<OccupiedSlot> occupiedSlots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupiedSlot {
        private LocalDate startDate;
        private LocalDate endDate;
        private String tenantName;
        private BigDecimal reservedCapacity;
    }
}
