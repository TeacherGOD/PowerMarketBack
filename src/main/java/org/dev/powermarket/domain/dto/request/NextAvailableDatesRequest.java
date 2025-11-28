package org.dev.powermarket.domain.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NextAvailableDatesRequest(
        BigDecimal requiredCapacity,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        Integer limit
) {
    public NextAvailableDatesRequest {
        if (fromDate == null) fromDate = LocalDate.now();
        if (limit == null) limit = 10;
    }
}