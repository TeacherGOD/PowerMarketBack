package org.dev.powermarket.domain.dto.request;


import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AvailablePeriodSearchRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        BigDecimal requiredCapacity
) {
    public AvailablePeriodSearchRequest {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = startDate.plusMonths(1);
        if (requiredCapacity == null) requiredCapacity = BigDecimal.ONE;
    }
}