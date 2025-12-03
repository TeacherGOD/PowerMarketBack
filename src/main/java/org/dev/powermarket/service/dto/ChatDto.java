package org.dev.powermarket.service.dto;


import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Основная информация о чате")
public record ChatDto(
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
        ChatMessageDto lastMessage
) {}