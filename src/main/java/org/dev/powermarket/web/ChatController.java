package org.dev.powermarket.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@Tag(name = "Chats", description = "API для управления чатами и сообщениями")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "Получить список чатов пользователя",
            description = "Возвращает пагинированный список чатов пользователя с краткой информацией"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный запрос"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ChatListItemDto>> getMyChats(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "Параметры пагинации и сортировки")
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chatService.getMyChats(principal.getUsername(), pageable));
    }

    @Operation(
            summary = "Получить детальную информацию о чате",
            description = "Возвращает полную информацию о чате, включая последние сообщения"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный запрос"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ к чату запрещен"),
            @ApiResponse(responseCode = "404", description = "Чат не найден")
    })
    @GetMapping(value = "/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatDetailDto> getChatDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "ID чата", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatDetail(principal.getUsername(), chatId));
    }

    @Operation(
            summary = "Получить сообщения чата",
            description = "Возвращает пагинированный список сообщений чата. При получении сообщения помечаются как прочитанные."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный запрос"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ к чату запрещен"),
            @ApiResponse(responseCode = "404", description = "Чат не найден")
    })
    @GetMapping(value = "/{chatId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ChatMessageDto>> getChatMessages(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "ID чата", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID chatId,
            @Parameter(description = "Параметры пагинации и сортировки")
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chatService.getChatMessages(principal.getUsername(), chatId, pageable));
    }

    @Operation(
            summary = "Отправить сообщение в чат",
            description = "Отправляет новое сообщение в указанный чат"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сообщение успешно отправлено"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ к чату запрещен"),
            @ApiResponse(responseCode = "404", description = "Чат не найден")
    })
    @PostMapping(
            value = "/{chatId}/messages",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ChatMessageDto> sendMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "ID чата", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID chatId,
            @Parameter(description = "Данные сообщения", required = true)
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(principal.getUsername(), chatId, request));
    }

    @Operation(
            summary = "Редактировать сообщение",
            description = "Редактирует существующее сообщение (доступно только в течение 60 минут после отправки)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сообщение успешно отредактировано"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные или время редактирования истекло"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ к сообщению запрещен"),
            @ApiResponse(responseCode = "404", description = "Сообщение не найдено")
    })
    @PatchMapping(
            value = "/messages/{messageId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ChatMessageDto> editMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "ID сообщения", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID messageId,
            @Parameter(description = "Новое содержание сообщения", required = true)
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(chatService.editMessage(principal.getUsername(), messageId, request));
    }

    @Operation(
            summary = "Удалить сообщение",
            description = "Удаляет сообщение. Можно удалить только для себя или для всех участников чата."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Сообщение успешно удалено"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ к сообщению запрещен"),
            @ApiResponse(responseCode = "404", description = "Сообщение не найдено")
    })
    @DeleteMapping(value = "/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "ID сообщения", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID messageId,
            @Parameter(
                    description = "Удалить для всех участников чата",
                    required = false,
                    example = "false"
            )
            @RequestParam(defaultValue = "false") boolean forEveryone) {
        chatService.deleteMessage(principal.getUsername(), messageId, forEveryone);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Получить количество непрочитанных сообщений",
            description = "Возвращает общее количество непрочитанных сообщений во всех чатах пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный запрос"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping(value = "/unread/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> getUnreadMessagesCount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(chatService.getUnreadMessagesCount(principal.getUsername()));
    }

    @Operation(
            summary = "Пометить сообщения чата как прочитанные",
            description = "Помечает все сообщения в указанном чате как прочитанные для текущего пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сообщения помечены как прочитанные"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ к чату запрещен"),
            @ApiResponse(responseCode = "404", description = "Чат не найден")
    })
    @PostMapping(value = "/{chatId}/mark-read")
    public ResponseEntity<Void> markMessagesAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "ID чата", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID chatId) {
        chatService.markMessagesAsRead(principal.getUsername(), chatId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Пометить все сообщения как прочитанные",
            description = "Помечает все сообщения во всех чатах пользователя как прочитанные"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Все сообщения помечены как прочитанные"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllMessagesAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        chatService.markAllMessagesAsRead(principal.getUsername());
        return ResponseEntity.ok().build();
    }
}