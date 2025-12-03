package org.dev.powermarket.domain.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.Chat;
import org.dev.powermarket.domain.ChatMessage;
import org.dev.powermarket.domain.dto.response.ChatDetailDto;
import org.dev.powermarket.domain.dto.response.ChatListItemDto;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.service.dto.ChatMessageDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    public ChatListItemDto toListItemDto(Chat chat, User currentUser, ChatMessage lastMessage, long unreadCount) {
        // Определяем контрагента (с кем ведется диалог)
        User counterpart = getCounterpart(chat, currentUser);
        String counterpartRole = getCounterpartRole(chat, currentUser);

        String lastMessagePreview = lastMessage != null
                ? truncateMessage(lastMessage.getContent(), 50)
                : null;

        Instant lastMessageTime = lastMessage != null
                ? lastMessage.getCreatedAt()
                : chat.getCreatedAt();

        String rentalTitle = chat.getRental().getService().getTitle();

        return new ChatListItemDto(
                chat.getId(),
                chat.getRental().getId(),
                rentalTitle,
                counterpart.getId(),
                counterpart.getFullName(),
                counterpartRole,
                lastMessagePreview,
                lastMessageTime,
                (int) unreadCount, // осторожно с большими числами
                chat.getCreatedAt()
        );
    }

    public ChatDetailDto toDetailDto(Chat chat, User currentUser,
                                     List<ChatMessage> recentMessages,
                                     ChatMessage lastMessage,
                                     int unreadCount) {
        return new ChatDetailDto(
                chat.getId(),
                chat.getRental().getId(),
                chat.getRental().getService() != null ? chat.getRental().getService().getTitle() : "Аренда",
                chat.getRental().getSupplier().getId(),
                chat.getRental().getSupplier().getFullName(),
                chat.getRental().getTenant().getId(),
                chat.getRental().getTenant().getFullName(),
                chat.getCreatedAt(),
                chat.getUpdatedAt(),
                recentMessages.stream().map(this::toMessageDto).toList(),
                lastMessage != null ? toMessageDto(lastMessage) : null,
                unreadCount,
                getCounterpart(chat, currentUser).getId(),
                getCounterpart(chat, currentUser).getFullName(),
                getCounterpartRole(chat, currentUser)
        );
    }

    public ChatMessageDto toMessageDto(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getChat().getId(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getContent(),
                message.getCreatedAt(),
                message.getReadAt(),
                message.isEdited(),
                message.getEditedAt()
        );
    }

    private User getCounterpart(Chat chat, User currentUser) {
        if (chat.getRental().getSupplier().getId().equals(currentUser.getId())) {
            return chat.getRental().getTenant();
        } else {
            return chat.getRental().getSupplier();
        }
    }

    private String getCounterpartRole(Chat chat, User currentUser) {
        if (chat.getRental().getSupplier().getId().equals(currentUser.getId())) {
            return "Арендатор";
        } else {
            return "Арендодатель";
        }
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null) return null;
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength) + "...";
    }
}
