package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Chat;
import org.dev.powermarket.domain.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    // Пагинированный список чатов пользователя
    @Query("SELECT c FROM Chat c " +
            "WHERE c.rental.supplier.id = :userId OR c.rental.tenant.id = :userId " +
            "ORDER BY c.updatedAt DESC NULLS LAST")
    Page<Chat> findChatsByUserId(@Param("userId") UUID userId, Pageable pageable);


    // Проверка доступа пользователя к чату
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Chat c WHERE c.id = :chatId AND " +
            "(c.rental.supplier.id = :userId OR c.rental.tenant.id = :userId)")
    boolean userHasAccessToChat(@Param("userId") UUID userId, @Param("chatId") UUID chatId);


    Optional<Chat> findByRental(Rental savedRental);
}
