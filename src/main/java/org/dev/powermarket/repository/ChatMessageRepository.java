package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Chat;
import org.dev.powermarket.domain.ChatMessage;
import org.dev.powermarket.repository.projection.UnreadCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {


    // Видимые сообщения (с учетом soft delete)
    @Query("SELECT m FROM ChatMessage m WHERE m.chat = :chat " +
            "AND m.deletedForEveryone = false " +
            "AND (m.deletedForSender = false OR m.sender.id != :userId) " +
            "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findVisibleMessagesByChat(
            @Param("chat") Chat chat,
            @Param("userId") UUID userId,
            Pageable pageable);

    // Последнее видимое сообщение в чате
    @Query("SELECT m FROM ChatMessage m WHERE m.chat = :chat " +
            "AND m.deletedForEveryone = false " +
            "AND (m.deletedForSender = false OR m.sender.id != :userId) " +
            "ORDER BY m.createdAt DESC " +
            "LIMIT 1")
    Optional<ChatMessage> findFirstVisibleByChat(
            @Param("chat") Chat chat,
            @Param("userId") UUID userId);

    // Непрочитанные сообщения для пользователя
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.chat.id = :chatId AND m.sender.id != :userId " +
            "AND m.readAt IS NULL AND m.deletedForEveryone = false " +
            "ORDER BY m.createdAt DESC")
    List<ChatMessage> findUnreadMessages(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    // Количество непрочитанных сообщений во всех чатах
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "JOIN Chat c ON m.chat = c " +
            "WHERE (c.rental.supplier.id = :userId OR c.rental.tenant.id = :userId) " +
            "AND m.sender.id != :userId AND m.readAt IS NULL " +
            "AND m.deletedForEveryone = false")
    long countUnreadMessages(@Param("userId") UUID userId);

    // Последние сообщения для нескольких чатов (для списка)
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.chat.id IN :chatIds " +
            "AND m.deletedForEveryone = false " +
            "AND m.createdAt = " +
            "(SELECT MAX(m2.createdAt) FROM ChatMessage m2 WHERE m2.chat.id = m.chat.id " +
            "AND m2.deletedForEveryone = false)")
    List<ChatMessage> findLastMessagesForChats(@Param("chatIds") List<UUID> chatIds);

    @Query("SELECT m FROM ChatMessage m " +
            "JOIN Chat c ON m.chat = c " +
            "WHERE (c.rental.supplier.id = :userId OR c.rental.tenant.id = :userId) " +
            "AND m.sender.id != :userId AND m.readAt IS NULL " +
            "AND m.deletedForEveryone = false " +
            "ORDER BY m.createdAt DESC")
    List<ChatMessage> findAllUnreadMessages(@Param("userId") UUID userId);

    // Количество непрочитанных по каждому чату
    @Query("SELECT m.chat.id as chatId, COUNT(m) as count FROM ChatMessage m " +
            "WHERE (m.chat.rental.supplier.id = :userId OR m.chat.rental.tenant.id = :userId) " +
            "AND m.sender.id != :userId AND m.readAt IS NULL AND m.deletedForEveryone = false " +
            "GROUP BY m.chat.id")
    List<UnreadCountProjection> countUnreadMessagesByChat(@Param("userId") UUID userId);

}
