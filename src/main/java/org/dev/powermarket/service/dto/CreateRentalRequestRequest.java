package org.dev.powermarket.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateRentalRequestRequest {
    @NotNull(message = "Service ID is required")
    private UUID serviceId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be in the future or present")
    private LocalDate startDate;


    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    @NotNull(message = "Capacity needed is required")
    @DecimalMin(value = "0.01", message = "Capacity must be at least 0.01")
    private BigDecimal capacityNeeded;
}
