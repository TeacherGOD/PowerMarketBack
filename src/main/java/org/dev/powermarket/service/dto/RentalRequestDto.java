package org.dev.powermarket.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dev.powermarket.domain.enums.RentalRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalRequestDto {
    private UUID id;
    private UUID serviceId;
    private String serviceTitle;
    private UUID tenantId;
    private String tenantName;
    private String tenantInn;
    private String tenantEmail;
    private String tenantPhone;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalPrice;
    private String message;
    private RentalRequestStatus status;
    private String rejectionReason;
    private Instant createdAt;
    private Instant respondedAt;
    private Integer capacityNeeded;
}
