package org.dev.powermarket.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardDto(
        String role,
        Period period,
        Summary summary,
        Charts charts
) {
    public record Period(
            LocalDate from,
            LocalDate to
    ) {}

    public record Summary(
            long activeOrders,
            long completedOrders,
            BigDecimal totalAmount,
            long pendingRequests
    ) {}

    public record Charts(
            StatusesChart statusesChart,
            IncomeChart incomeChart
    ) {}

    public record StatusesChart(
            long totalOrders,
            List<StatusItem> items
    ) {}

    public record StatusItem(
            String status,
            String label,
            long count
    ) {}

    public record IncomeChart(
            String currency,
            String groupBy,
            List<IncomePoint> points
    ) {}

    public record IncomePoint(
            String periodLabel,
            LocalDate periodStart,
            BigDecimal totalAmount,
            long completedOrders
    ) {}
}
