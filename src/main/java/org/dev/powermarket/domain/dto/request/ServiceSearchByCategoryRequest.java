package org.dev.powermarket.domain.dto.request;


import org.dev.powermarket.domain.enums.ServiceCategory;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceSearchByCategoryRequest(
        ServiceCategory category,
        BigDecimal minCapacity,

        // Опционально: фильтр по датам доступности
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate availableFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate availableTo,

        // Пагинация
        Integer page,
        Integer size
) {
    public ServiceSearchByCategoryRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (minCapacity == null) minCapacity = BigDecimal.ZERO;
    }
}