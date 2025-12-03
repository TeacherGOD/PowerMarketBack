package org.dev.powermarket.domain.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChatListItemDto(
        UUID id,
        UUID rentalId,
        String rentalTitle,
        UUID counterpartId,
        String counterpartName,
        String counterpartRole,
        String lastMessagePreview,
        Instant lastMessageTime,
        int unreadCount,
        Instant createdAt
) {}