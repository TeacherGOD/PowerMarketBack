package org.dev.powermarket.web;

import org.dev.powermarket.service.ChatService;
import org.dev.powermarket.service.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/chats","/api/chats"})
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/my")
    public ResponseEntity<List<ChatDto>> getMyChats(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(chatService.getMyChats(principal.getUsername()));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDto> getChat(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChat(principal.getUsername(), chatId));
    }

    @GetMapping("/rental-request/{requestId}")
    public ResponseEntity<ChatDto> getChatByRentalRequest(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(chatService.getChatByRentalRequestId(principal.getUsername(), requestId));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getChatMessages(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatMessages(principal.getUsername(), chatId));
    }

    @GetMapping("/rental-request/{requestId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getChatMessagesByRentalRequest(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(chatService.getChatMessagesByRentalRequestId(principal.getUsername(), requestId));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID chatId,
            @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(principal.getUsername(), chatId, request));
    }

    @PostMapping("/rental-request/{requestId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessageByRentalRequest(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID requestId,
            @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessageByRentalRequestId(principal.getUsername(), requestId, request));
    }
}
