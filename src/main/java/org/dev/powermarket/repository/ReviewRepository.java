package org.dev.powermarket.repository;

import org.dev.powermarket.domain.Review;
import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.security.entity.User;
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
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByRentalAndReviewer(Rental rental, User reviewer);

    boolean existsByRentalAndReviewer(Rental rental, User reviewer);

    List<Review> findByReviewedUser(User reviewedUser);

    Page<Review> findByReviewedUser(User reviewedUser, Pageable pageable);

    List<Review> findByService(Service service);

    Page<Review> findByService(Service service, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewedUser = :user")
    Double getAverageRatingForUser(@Param("user") User user);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.service = :service")
    Double getAverageRatingForService(@Param("service") Service service);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.service = :service")
    Long countReviewsForService(@Param("service") Service service);
}
