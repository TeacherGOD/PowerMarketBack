package org.dev.powermarket.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на отправку сообщения в чат")
public record SendMessageRequest(
        @Schema(
                description = "Текст сообщения",
                example = "Здравствуйте! Интересует ваше предложение по аренде",
                minLength = 1,
                maxLength = 2000
        )
        @NotBlank(message = "Message content is required")
        @Size(max = 2000)
        String content
) {}
