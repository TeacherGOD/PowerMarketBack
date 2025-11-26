package org.dev.powermarket.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateRentalRequestRequest {
    @NotNull(message = "Service ID is required")
    private UUID serviceId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Integer capacityNeeded;
}
