package org.dev.powermarket.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto {
    private UUID id;
    private UUID rentalId;
    private UUID supplierId;
    private String supplierName;
    private UUID tenantId;
    private String tenantName;
    private Instant createdAt;
    private List<ChatMessageDto> recentMessages;
}
