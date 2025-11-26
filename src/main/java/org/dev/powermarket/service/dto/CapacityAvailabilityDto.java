package org.dev.powermarket.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapacityAvailabilityDto {
    private LocalDate date;
    private Integer totalCapacity;
    private Integer availableCapacity;
    private Integer occupiedCapacity;
    private List<OccupiedSlot> occupiedSlots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupiedSlot {
        private LocalDate startDate;
        private LocalDate endDate;
        private String tenantName;
        private Integer capacityUnits;
    }
}
