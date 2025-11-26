package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Chat;
import org.dev.powermarket.domain.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    Optional<Chat> findByRental(Rental rental);

    @Query("SELECT c FROM Chat c WHERE c.rental.supplier.id = :userId OR c.rental.tenant.id = :userId")
    List<Chat> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Chat c WHERE c.rental.rentalRequest.id = :rentalRequestId")
    Optional<Chat> findByRentalRequestId(@Param("rentalRequestId") UUID rentalRequestId);
}
