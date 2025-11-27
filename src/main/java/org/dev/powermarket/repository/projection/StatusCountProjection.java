package org.dev.powermarket.repository.projection;

import org.dev.powermarket.domain.enums.RentalRequestStatus;

public interface StatusCountProjection {
    RentalRequestStatus getStatus();
    Long getCount();
}
