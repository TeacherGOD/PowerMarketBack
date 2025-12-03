package org.dev.powermarket.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Schema(description = "Запрос на редактирование сообщения")
public record EditMessageRequest(
        @Schema(
                description = "Новый текст сообщения",
                example = "Измененный текст сообщения",
                minLength = 1,
                maxLength = 2000
        )
        @NotBlank
        @Size(max = 2000)
        String content
) {}
