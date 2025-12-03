package org.dev.powermarket.service.dto;


import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        UUID chatId,
        UUID senderId,
        String senderName,
        String content,
        Instant sentAt,
        Instant readAt,
        boolean edited,
        Instant editedAt
) {}
