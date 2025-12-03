package org.dev.powermarket.repository.projection;

import java.util.UUID;

public interface UnreadCountProjection {
    UUID getChatId();
    Long getCount();
}
