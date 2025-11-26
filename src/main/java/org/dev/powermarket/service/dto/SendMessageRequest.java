package org.dev.powermarket.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotBlank(message = "Message content is required")
    @Size(max = 2000)
    private String content;
}
