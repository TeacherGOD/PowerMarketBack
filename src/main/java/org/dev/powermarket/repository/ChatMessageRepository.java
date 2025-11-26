package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Chat;
import org.dev.powermarket.domain.ChatMessage;
import org.dev.powermarket.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    Page<ChatMessage> findByChatOrderByCreatedAtAsc(Chat chat, Pageable pageable);
    
    List<ChatMessage> findByChatOrderByCreatedAtAsc(Chat chat);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chat = :chat AND cm.sender != :user AND cm.isRead = false")
    long countUnreadMessages(@Param("chat") Chat chat, @Param("user") User user);

    List<ChatMessage> findByChatOrderByCreatedAtDesc(Chat chat, PageRequest of);
}
