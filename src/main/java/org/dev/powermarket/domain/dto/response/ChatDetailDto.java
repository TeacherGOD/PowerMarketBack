package org.dev.powermarket.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dev.powermarket.service.dto.ChatMessageDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Детальная информация о чате")
public record ChatDetailDto(
        @Schema(description = "ID чата", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "ID аренды", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID rentalId,

        @Schema(description = "Название аренды", example = "Аренда помещения под офис")
        String rentalTitle,

        @Schema(description = "ID поставщика", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID supplierId,

        @Schema(description = "Имя поставщика", example = "ООО 'Ромашка'")
        String supplierName,

        @Schema(description = "ID арендатора", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID tenantId,

        @Schema(description = "Имя арендатора", example = "Иван Петров")
        String tenantName,

        @Schema(description = "Время создания чата")
        Instant createdAt,

        @Schema(description = "Время последнего обновления чата")
        Instant updatedAt,

        @Schema(description = "Последние сообщения в чате")
        List<ChatMessageDto> recentMessages,

        @Schema(description = "Последнее сообщение в чате")
        ChatMessageDto lastMessage,

        @Schema(description = "Количество непрочитанных сообщений", example = "5")
        int unreadMessagesCount,

        @Schema(description = "ID собеседника", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID counterpartId,

        @Schema(description = "Имя собеседника", example = "ООО 'Ромашка'")
        String counterpartName,

        @Schema(description = "Роль собеседника", example = "Арендодатель")
        String counterpartRole
) {}
