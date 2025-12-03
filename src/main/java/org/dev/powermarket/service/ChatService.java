package org.dev.powermarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dev.powermarket.domain.Chat;
import org.dev.powermarket.domain.ChatMessage;
import org.dev.powermarket.domain.dto.mapper.ChatMapper;
import org.dev.powermarket.domain.dto.request.EditMessageRequest;
import org.dev.powermarket.domain.dto.response.ChatDetailDto;
import org.dev.powermarket.domain.dto.response.ChatListItemDto;
import org.dev.powermarket.repository.projection.UnreadCountProjection;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.repository.ChatMessageRepository;
import org.dev.powermarket.repository.ChatRepository;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.dev.powermarket.service.dto.ChatMessageDto;
import org.dev.powermarket.service.dto.SendMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final AuthorizedUserRepository userRepository;
    private final ChatMapper chatMapper;


    //todo убрать их в default части?
    private static final int RECENT_MESSAGES_LIMIT = 10;
    private static final int MESSAGE_EDIT_TIMEOUT_MINUTES = 60;
    private static final int LAST_MESSAGE_PREVIEW_LENGTH = 50;

    @Transactional(readOnly = true)
    public Page<ChatListItemDto> getMyChats(String email, Pageable pageable) {
        User user = getUserByEmail(email);

        Page<Chat> chatsPage = chatRepository.findChatsByUserId(user.getId(), pageable);
        List<UUID> chatIds = chatsPage.getContent().stream()
                .map(Chat::getId)
                .toList();

        // Получаем последние сообщения для всех чатов одним запросом
        Map<UUID, ChatMessage> lastMessagesMap = messageRepository
                .findLastMessagesForChats(chatIds)
                .stream()
                .filter(message -> !message.isDeletedForEveryone())
                .collect(Collectors.toMap(
                        message -> message.getChat().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // Получаем количество непрочитанных сообщений
        List<UnreadCountProjection> projections = messageRepository.countUnreadMessagesByChat(user.getId());
        Map<UUID, Long> unreadCountsMap = projections.stream()
                .collect(Collectors.toMap(UnreadCountProjection::getChatId, UnreadCountProjection::getCount));

        // Маппим в DTO
        List<ChatListItemDto> chatItems = chatsPage.getContent().stream()
                .map(chat -> {
                    ChatMessage lastMessage = lastMessagesMap.get(chat.getId());
                    long unreadCount = unreadCountsMap.getOrDefault(chat.getId(), 0L);
                    return chatMapper.toListItemDto(chat, user, lastMessage, (int) unreadCount);
                })
                .toList();

        return new PageImpl<>(chatItems, pageable, chatsPage.getTotalElements());
    }

    @PreAuthorize("@chatAccessService.hasAccessToChat(@userService.getUserIdByEmail(#email), #chatId)")
    public Page<ChatMessageDto> getChatMessages(String email, UUID chatId, Pageable pageable) {
        Chat chat = getChatById(chatId);
        User user = getUserByEmail(email);

        // Помечаем сообщения как прочитанные
        markMessagesAsRead(chat, user);

        return messageRepository.findVisibleMessagesByChat(chat, user.getId(), pageable)
                .map(chatMapper::toMessageDto);
    }


    @Transactional
    @PreAuthorize("@chatAccessService.hasAccessToChat(@userService.getUserIdByEmail(#email), #chatId)")
    public ChatMessageDto sendMessage(String email, UUID chatId, SendMessageRequest request) {
        Chat chat = getChatById(chatId);
        User user = getUserByEmail(email);

        ChatMessage message = ChatMessage.builder()
                .chat(chat)
                .sender(user)
                .content(request.content())
                .isEdited(false)
                .deletedForEveryone(false)
                .deletedForSender(false)
                .createdAt(Instant.now())
                .build();


        markMessagesAsRead(chat, user);
        ChatMessage saved = messageRepository.save(message);

        // Обновляем информацию о последнем сообщении в чате
        updateChatLastMessage(chat, saved, user);

        log.info("Message sent: chatId={}, senderId={}, messageId={}",
                chatId, user.getId(), saved.getId());

        return chatMapper.toMessageDto(saved);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ChatMessageDto editMessage(String email, UUID messageId, EditMessageRequest request) {
        ChatMessage message = getMessageById(messageId);
        User user = getUserByEmail(email);

        // Проверяем, что пользователь является отправителем
        if (!message.getSender().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only edit your own messages");
        }

        // Проверяем, что сообщение не удалено
        if (message.isDeletedForEveryone() || message.isDeletedForSender()) {
            throw new IllegalArgumentException("Cannot edit deleted message");
        }

        // Проверяем, что сообщение не старше MESSAGE_EDIT_TIMEOUT_MINUTES минут
        Instant editDeadline = message.getCreatedAt().plusSeconds(MESSAGE_EDIT_TIMEOUT_MINUTES * 60L);
        if (Instant.now().isAfter(editDeadline)) {
            throw new IllegalArgumentException(
                    String.format("Message can only be edited within %d minutes of sending",
                            MESSAGE_EDIT_TIMEOUT_MINUTES));
        }

        message.setContent(request.content());
        message.setEdited(true);
        message.setEditedAt(Instant.now());

        ChatMessage saved = messageRepository.save(message);

        // Если это последнее сообщение в чате, обновляем preview
        updateChatIfLastMessage(message.getChat(), saved);

        log.info("Message edited: messageId={}, editorId={}", messageId, user.getId());

        return chatMapper.toMessageDto(saved);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void deleteMessage(String email, UUID messageId, boolean deleteForEveryone) {
        ChatMessage message = getMessageById(messageId);
        User user = getUserByEmail(email);

        // Проверяем права
        if (deleteForEveryone) {
            // Только отправитель может удалить для всех
            if (!message.getSender().getId().equals(user.getId())) {
                throw new AccessDeniedException("You don't have permission to delete this message for everyone");
            }
            message.setDeletedForEveryone(true);
            messageRepository.save(message);

            // Если это последнее сообщение, обновляем чат
            updateChatIfLastMessageDeleted(message.getChat());

            log.info("Message deleted for everyone: messageId={}, deleterId={}", messageId, user.getId());
        } else {
            // Только отправитель может удалить для себя
            if (!message.getSender().getId().equals(user.getId())) {
                throw new AccessDeniedException("You can only delete your own messages");
            }
            message.setDeletedForSender(true);
            messageRepository.save(message);

            log.info("Message deleted for sender: messageId={}, senderId={}", messageId, user.getId());
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public long getUnreadMessagesCount(String email) {
        User user = getUserByEmail(email);
        return messageRepository.countUnreadMessages(user.getId());
    }

    @Transactional
    @PreAuthorize("@chatAccessService.hasAccessToChat(@userService.getUserIdByEmail(#email), #chatId)")
    public void markMessagesAsRead(String email, UUID chatId) {
        Chat chat = getChatById(chatId);
        User user = getUserByEmail(email);
        markMessagesAsRead(chat, user);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void markAllMessagesAsRead(String email) {
        User user = getUserByEmail(email);

        // Получаем все непрочитанные сообщения пользователя
        List<ChatMessage> unreadMessages = messageRepository.findAllUnreadMessages(user.getId());

        if (!unreadMessages.isEmpty()) {
            Instant now = Instant.now();
            unreadMessages.forEach(msg -> msg.setReadAt(now));
            messageRepository.saveAll(unreadMessages);

            // Обновляем флаги hasUnread в чатах
            unreadMessages.stream()
                    .map(ChatMessage::getChat)
                    .distinct()
                    .forEach(chat -> {
                        chat.setHasUnread(false);
                        chatRepository.save(chat);
                    });
        }
    }

    private Chat getChatById(UUID chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
    }

    private ChatMessage getMessageById(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }


    private void markMessagesAsRead(Chat chat, User user) {
        List<ChatMessage> unreadMessages = messageRepository.findUnreadMessages(
                chat.getId(),
                user.getId()
        );

        if (!unreadMessages.isEmpty()) {
            Instant now = Instant.now();
            unreadMessages.forEach(msg -> msg.setReadAt(now));
            messageRepository.saveAll(unreadMessages);

            // Обновляем флаг hasUnread в чате
            chat.setHasUnread(false);
            chatRepository.save(chat);
        }
    }

    private void updateChatLastMessage(Chat chat, ChatMessage message, User sender) {
        // Определяем получателя (другого участника чата)
        User receiver = getOtherParticipant(chat, sender);

        chat.setLastMessagePreview(truncateMessage(message.getContent(), LAST_MESSAGE_PREVIEW_LENGTH));
        chat.setLastSenderId(sender.getId());
        chat.setUpdatedAt(Instant.now());

        // Устанавливаем флаг hasUnread для получателя
        if (!sender.getId().equals(receiver.getId())) {
            chat.setHasUnread(true);
        }

        chatRepository.save(chat);
    }

    private void updateChatIfLastMessage(Chat chat, ChatMessage message) {
        ChatMessage lastVisibleMessage = messageRepository.findFirstVisibleByChat(chat, null)
                .orElse(null);

        if (lastVisibleMessage != null && lastVisibleMessage.getId().equals(message.getId())) {
            chat.setLastMessagePreview(truncateMessage(message.getContent(), LAST_MESSAGE_PREVIEW_LENGTH));
            chat.setUpdatedAt(Instant.now());
            chatRepository.save(chat);
        }
    }

    private void updateChatIfLastMessageDeleted(Chat chat) {
        // Находим предыдущее видимое сообщение
        ChatMessage previousMessage = messageRepository.findFirstVisibleByChat(chat, null)
                .orElse(null);

        if (previousMessage != null) {
            chat.setLastMessagePreview(truncateMessage(previousMessage.getContent(), LAST_MESSAGE_PREVIEW_LENGTH));
            chat.setLastSenderId(previousMessage.getSender().getId());
        } else {
            chat.setLastMessagePreview(null);
            chat.setLastSenderId(null);
        }

        chat.setUpdatedAt(Instant.now());
        chatRepository.save(chat);
    }

    private User getOtherParticipant(Chat chat, User currentUser) {
        if (chat.getRental().getSupplier().getId().equals(currentUser.getId())) {
            return chat.getRental().getTenant();
        } else {
            return chat.getRental().getSupplier();
        }
    }

    private String truncateMessage(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }


    @Transactional(readOnly = true)
    @PreAuthorize("@chatAccessService.hasAccessToChat(@userService.getUserIdByEmail(#email), #chatId)")
    public ChatDetailDto getChatDetail(String email, UUID chatId) {
        Chat chat = getChatById(chatId);
        User user = getUserByEmail(email);

        // Получаем последние сообщения (только видимые)
        Page<ChatMessage> recentMessagesPage = messageRepository.findVisibleMessagesByChat(
                chat,
                user.getId(),
                Pageable.ofSize(RECENT_MESSAGES_LIMIT)
        );

        // Получаем последнее видимое сообщение
        ChatMessage lastMessage = messageRepository.findFirstVisibleByChat(chat, user.getId())
                .orElse(null);

        // Получаем количество непрочитанных
        List<ChatMessage> unreadMessages = messageRepository.findUnreadMessages(chat.getId(), user.getId());
        int unreadCount = (int) unreadMessages.stream()
                .filter(msg -> !msg.isDeletedForEveryone() &&
                        (!msg.getSender().getId().equals(user.getId()) || !msg.isDeletedForSender()))
                .count();

        return chatMapper.toDetailDto(
                chat,
                user,
                recentMessagesPage.getContent(),
                lastMessage,
                unreadCount
        );
    }
}
