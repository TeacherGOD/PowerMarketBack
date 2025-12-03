package org.dev.powermarket.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dev.powermarket.service.dto.ChatMessageDto;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Краткая информация о чате")
public record ChatSummaryDto(
        @Schema(description = "ID чата", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "ID аренды", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID rentalId,

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

        @Schema(description = "Последнее сообщение в чате")
        ChatMessageDto lastMessage
) {}
