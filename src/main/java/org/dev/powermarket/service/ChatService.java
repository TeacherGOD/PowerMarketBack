package org.dev.powermarket.service;

import org.dev.powermarket.domain.Chat;
import org.dev.powermarket.domain.ChatMessage;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.repository.ChatMessageRepository;
import org.dev.powermarket.repository.ChatRepository;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.dev.powermarket.service.dto.ChatDto;
import org.dev.powermarket.service.dto.ChatMessageDto;
import org.dev.powermarket.service.dto.SendMessageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final AuthorizedUserRepository userRepository;

    public ChatService(ChatRepository chatRepository,
                       ChatMessageRepository messageRepository,
                       AuthorizedUserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatDto> getMyChats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Chat> chats = chatRepository.findByUserId(user.getId());
        return chats.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatDto getChat(String email, UUID chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = chat.getRental().getSupplier().getId().equals(user.getId()) ||
                chat.getRental().getTenant().getId().equals(user.getId());

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have access to this chat");
        }

        return toDto(chat);
    }

    @Transactional(readOnly = true)
    public ChatDto getChatByRentalRequestId(String email, UUID rentalRequestId) {
        Chat chat = chatRepository.findByRentalRequestId(rentalRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = chat.getRental().getSupplier().getId().equals(user.getId()) ||
                chat.getRental().getTenant().getId().equals(user.getId());

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have access to this chat");
        }

        return toDto(chat);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatMessages(String email, UUID chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = chat.getRental().getSupplier().getId().equals(user.getId()) ||
                chat.getRental().getTenant().getId().equals(user.getId());

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have access to this chat");
        }

        return messageRepository.findByChatOrderByCreatedAtAsc(chat).stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatMessagesByRentalRequestId(String email, UUID rentalRequestId) {
        Chat chat = chatRepository.findByRentalRequestId(rentalRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = chat.getRental().getSupplier().getId().equals(user.getId()) ||
                chat.getRental().getTenant().getId().equals(user.getId());

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have access to this chat");
        }

        return messageRepository.findByChatOrderByCreatedAtAsc(chat).stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto sendMessage(String email, UUID chatId, SendMessageRequest request) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = chat.getRental().getSupplier().getId().equals(sender.getId()) ||
                chat.getRental().getTenant().getId().equals(sender.getId());

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have access to this chat");
        }

        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setCreatedAt(Instant.now());

        ChatMessage saved = messageRepository.save(message);
        return toMessageDto(saved);
    }

    @Transactional
    public ChatMessageDto sendMessageByRentalRequestId(String email, UUID rentalRequestId, SendMessageRequest request) {
        Chat chat = chatRepository.findByRentalRequestId(rentalRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = chat.getRental().getSupplier().getId().equals(sender.getId()) ||
                chat.getRental().getTenant().getId().equals(sender.getId());

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have access to this chat");
        }

        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setCreatedAt(Instant.now());

        ChatMessage saved = messageRepository.save(message);
        return toMessageDto(saved);
    }

    private ChatDto toDto(Chat chat) {
        ChatDto dto = new ChatDto();
        dto.setId(chat.getId());
        dto.setRentalId(chat.getRental().getId());
        dto.setSupplierId(chat.getRental().getSupplier().getId());
        dto.setSupplierName(chat.getRental().getSupplier().getFullName());
        dto.setTenantId(chat.getRental().getTenant().getId());
        dto.setTenantName(chat.getRental().getTenant().getFullName());
        dto.setCreatedAt(chat.getCreatedAt());

        // Get recent messages (last 10)
        List<ChatMessageDto> recentMessages = messageRepository
                .findByChatOrderByCreatedAtDesc(chat, PageRequest.of(0, 10))
                .stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
        dto.setRecentMessages(recentMessages);

        return dto;
    }

    private ChatMessageDto toMessageDto(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getChat().getId(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
