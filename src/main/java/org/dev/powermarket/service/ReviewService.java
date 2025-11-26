package org.dev.powermarket.service;

import org.dev.powermarket.domain.*;
import org.dev.powermarket.domain.enums.NotificationType;
import org.dev.powermarket.domain.enums.RentalRequestStatus;
import org.dev.powermarket.repository.*;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.dev.powermarket.service.dto.CreateReviewRequest;
import org.dev.powermarket.service.dto.ReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RentalRepository rentalRepository;
    private final ServiceRepository serviceRepository;
    private final AuthorizedUserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         RentalRepository rentalRepository,
                         ServiceRepository serviceRepository,
                         AuthorizedUserRepository userRepository,
                         NotificationRepository notificationRepository) {
        this.reviewRepository = reviewRepository;
        this.rentalRepository = rentalRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public ReviewDto createReview(String email, CreateReviewRequest request) {
        User reviewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Rental rental = rentalRepository.findById(request.getRentalId())
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));

        // Check if user is part of this rental
        boolean isParticipant = rental.getSupplier().getId().equals(reviewer.getId()) ||
                rental.getTenant().getId().equals(reviewer.getId());

        if (!isParticipant) {
            throw new AccessDeniedException("You can only review rentals you participated in");
        }

        // Check if rental is completed
        if (rental.getRentalRequest().getStatus() != RentalRequestStatus.COMPLETED) {
            throw new IllegalArgumentException("You can only review completed rentals");
        }

        // Check if already reviewed
        if (reviewRepository.existsByRentalAndReviewer(rental, reviewer)) {
            throw new IllegalArgumentException("You have already reviewed this rental");
        }

        // Determine who is being reviewed
        User reviewedUser = rental.getSupplier().getId().equals(reviewer.getId())
                ? rental.getTenant()
                : rental.getSupplier();

        Review review = new Review();
        review.setRental(rental);
        review.setReviewer(reviewer);
        review.setReviewedUser(reviewedUser);
        review.setService(rental.getService());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);

        // Update service average rating
        updateServiceRating(rental.getService());

        updateUserRating(reviewedUser);

        // Create notification
        createNotification(
                reviewedUser,
                NotificationType.NEW_RENTAL_REQUEST,
                "Новый отзыв",
                String.format("%s оставил отзыв о сотрудничестве", reviewer.getFullName()),
                saved.getId()
        );

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> getServiceReviews(UUID serviceId, Pageable pageable) {
        org.dev.powermarket.domain.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        return reviewRepository.findByService(service, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> getUserReviews(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return reviewRepository.findByReviewedUser(user, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ReviewDto getReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        return toDto(review);
    }

    private void updateServiceRating(org.dev.powermarket.domain.Service service) {
        Double avgRating = reviewRepository.getAverageRatingForService(service);
        Long totalReviews = reviewRepository.countReviewsForService(service);

        if (avgRating != null) {
            service.setAverageRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        }
        service.setTotalReviews(totalReviews != null ? totalReviews.intValue() : 0);

        serviceRepository.save(service);
    }

    private void updateUserRating(User user) {
        Double avgRating = reviewRepository.getAverageRatingForUser(user);
        Integer totalReviews = reviewRepository.findByReviewedUser(user).size();

        if (avgRating != null) {
            user.setAverageRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        }
        user.setTotalReviews(totalReviews != null ? totalReviews : 0);

        userRepository.save(user);
    }

    private void createNotification(User user, NotificationType type, String title,
                                    String message, UUID relatedEntityId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private ReviewDto toDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRentalId(review.getRental().getId());
        dto.setReviewerId(review.getReviewer().getId());
        dto.setReviewerName(review.getReviewer().getFullName());
        dto.setReviewedUserId(review.getReviewedUser().getId());
        dto.setReviewedUserName(review.getReviewedUser().getFullName());
        dto.setServiceId(review.getService().getId());
        dto.setServiceTitle(review.getService().getTitle());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
