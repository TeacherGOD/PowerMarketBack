package org.dev.powermarket.domain.dto.request;

import org.dev.powermarket.domain.enums.ServiceCategory;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceSearchRequest(
        ServiceCategory category,
        String keyword,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        BigDecimal requiredCapacity,
        String location,

        // Пагинация
        Integer page,
        Integer size
) {
    public ServiceSearchRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
    }
}