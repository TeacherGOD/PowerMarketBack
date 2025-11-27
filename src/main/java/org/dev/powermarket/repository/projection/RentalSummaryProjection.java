package org.dev.powermarket.repository.projection;

import java.math.BigDecimal;

public interface RentalSummaryProjection {
    Long getCount();
    BigDecimal getTotalAmount();
}
