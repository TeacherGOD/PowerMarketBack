package org.dev.powermarket.repository.projection;

import java.math.BigDecimal;

public interface IncomeChartProjection {
    Double getYear();
    Double getMonth();
    BigDecimal getTotalAmount();
    Long getCompletedOrders();
}
