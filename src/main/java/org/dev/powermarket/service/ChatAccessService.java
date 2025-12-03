package org.dev.powermarket.service;

import lombok.RequiredArgsConstructor;
import org.dev.powermarket.repository.ChatRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatAccessService {

    private final ChatRepository chatRepository;
    private final UserService userService;

    public boolean hasAccessToChat(String email, UUID chatId) {
        return hasAccessToChat(userService.getUserIdByEmail(email), chatId);
    }

    public boolean hasAccessToChat(UUID userId, UUID chatId) {
        return chatRepository.userHasAccessToChat(userId, chatId);
    }
}
