package org.dev.powermarket.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Элемент списка чатов")
public record ChatListItemDto(
        @Schema(description = "ID чата", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "ID аренды", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID rentalId,

        @Schema(description = "Название аренды", example = "Аренда помещения под офис")
        String rentalTitle,

        @Schema(description = "ID собеседника", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID counterpartId,

        @Schema(description = "Имя собеседника", example = "ООО 'Ромашка'")
        String counterpartName,

        @Schema(description = "Роль собеседника", example = "Арендодатель")
        String counterpartRole,

        @Schema(description = "Превью последнего сообщения", example = "Добрый день! Интересует ваше предложение...")
        String lastMessagePreview,

        @Schema(description = "Время последнего сообщения")
        Instant lastMessageTime,

        @Schema(description = "Количество непрочитанных сообщений", example = "3")
        int unreadCount,

        @Schema(description = "Время создания чата")
        Instant createdAt
) {}