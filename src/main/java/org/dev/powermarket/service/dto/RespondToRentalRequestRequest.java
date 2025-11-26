package org.dev.powermarket.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespondToRentalRequestRequest {
    @NotNull(message = "Approved status is required")
    private Boolean approved;

    private String rejectionReason;
}
