package org.dev.powermarket.domain.dto.response;

import org.dev.powermarket.service.dto.ChatMessageDto;

import java.time.Instant;
import java.util.UUID;

public record ChatSummaryDto(
        UUID id,
        UUID rentalId,
        UUID supplierId,
        String supplierName,
        UUID tenantId,
        String tenantName,
        Instant createdAt,
        ChatMessageDto lastMessage
) {}
