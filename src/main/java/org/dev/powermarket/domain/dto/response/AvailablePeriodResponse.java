package org.dev.powermarket.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AvailablePeriodResponse(
        UUID serviceId,
        String serviceTitle,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal availableCapacity,
        BigDecimal requiredCapacity,
        Boolean isFullyAvailable
) {}
