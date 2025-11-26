package org.dev.powermarket.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dev.powermarket.domain.enums.RentalRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalDto {
    private UUID id;
    private UUID serviceId;
    private String serviceTitle;
    private UUID supplierId;
    private String supplierName;
    private UUID tenantId;
    private String tenantName;
    private Double capacityRented;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalPrice;
    private UUID chatId;
    private Boolean supplierConfirmed;
    private Boolean tenantConfirmed;
    private Instant supplierConfirmedAt;
    private Instant tenantConfirmedAt;
    private RentalRequestStatus status;
    private Boolean isActive;
    private Instant createdAt;
}
