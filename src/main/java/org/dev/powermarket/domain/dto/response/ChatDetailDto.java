package org.dev.powermarket.domain.dto.response;

import org.dev.powermarket.service.dto.ChatMessageDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatDetailDto(
        UUID id,
        UUID rentalId,
        String rentalTitle,
        UUID supplierId,
        String supplierName,
        UUID tenantId,
        String tenantName,
        Instant createdAt,
        Instant updatedAt,
        List<ChatMessageDto> recentMessages,
        ChatMessageDto lastMessage,
        int unreadMessagesCount,
        UUID counterpartId,
        String counterpartName,
        String counterpartRole
) {}
