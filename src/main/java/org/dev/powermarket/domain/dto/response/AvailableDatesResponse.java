package org.dev.powermarket.domain.dto.response;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailableDatesResponse(
        UUID serviceId,
        String serviceTitle,
        BigDecimal requiredCapacity,
        List<LocalDate> availableDates
) {}