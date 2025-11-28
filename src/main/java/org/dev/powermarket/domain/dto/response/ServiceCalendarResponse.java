package org.dev.powermarket.domain.dto.response;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ServiceCalendarResponse(
        UUID serviceId,
        String serviceTitle,
        List<CalendarDay> calendarDays
) {
    public record CalendarDay(
            LocalDate date,
            BigDecimal totalCapacity,
            BigDecimal availableCapacity,
            BigDecimal reservedCapacity,
            Boolean isAvailable,
            List<BookingSlot> bookings
    ) {}

    public record BookingSlot(
            String tenantName,
            BigDecimal reservedCapacity,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}