package org.dev.powermarket.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class RentalStatsDto {
    private Long totalRentals;
    private Long activeRentals;
    private Long completedRentals;
    private BigDecimal totalRevenue;
    private BigDecimal averageRating;

    public RentalStatsDto() {
    }

    public RentalStatsDto(Long totalRentals, Long activeRentals, Long completedRentals,
                          BigDecimal totalRevenue, BigDecimal averageRating) {
        this.totalRentals = totalRentals;
        this.activeRentals = activeRentals;
        this.completedRentals = completedRentals;
        this.totalRevenue = totalRevenue;
        this.averageRating = averageRating;
    }

}
