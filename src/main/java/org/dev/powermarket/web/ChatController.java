package org.dev.powermarket.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.dto.request.EditMessageRequest;
import org.dev.powermarket.domain.dto.response.ChatDetailDto;
import org.dev.powermarket.domain.dto.response.ChatListItemDto;
import org.dev.powermarket.service.ChatService;
import org.dev.powermarket.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<Page<ChatListItemDto>> getMyChats(
            @AuthenticationPrincipal UserDetails principal,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chatService.getMyChats(principal.getUsername(), pageable));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDetailDto> getChatDetail(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatDetail(principal.getUsername(), chatId));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<ChatMessageDto>> getChatMessages(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID chatId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chatService.getChatMessages(principal.getUsername(), chatId, pageable));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID chatId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(principal.getUsername(), chatId, request));
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessageDto> editMessage(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(chatService.editMessage(principal.getUsername(), messageId, request));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "false") boolean forEveryone) {
        chatService.deleteMessage(principal.getUsername(), messageId, forEveryone);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadMessagesCount(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(chatService.getUnreadMessagesCount(principal.getUsername()));
    }

    @PostMapping("/{chatId}/mark-read")
    public ResponseEntity<Void> markMessagesAsRead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID chatId) {
        chatService.markMessagesAsRead(principal.getUsername(), chatId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllMessagesAsRead(
            @AuthenticationPrincipal UserDetails principal) {
        chatService.markAllMessagesAsRead(principal.getUsername());
        return ResponseEntity.ok().build();
    }
}