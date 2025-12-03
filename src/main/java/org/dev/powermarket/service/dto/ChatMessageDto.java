package org.dev.powermarket.service.dto;


import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "DTO сообщения чата")
public record ChatMessageDto(
        @Schema(description = "ID сообщения", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "ID чата", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID chatId,

        @Schema(description = "ID отправителя", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID senderId,

        @Schema(description = "Имя отправителя", example = "Иван Иванов")
        String senderName,

        @Schema(description = "Текст сообщения", example = "Здравствуйте!")
        String content,

        @Schema(description = "Время отправки")
        Instant sentAt,

        @Schema(description = "Время прочтения (null если не прочитано)")
        Instant readAt,

        @Schema(description = "Было ли сообщение отредактировано")
        boolean edited,

        @Schema(description = "Время последнего редактирования")
        Instant editedAt
) {}
