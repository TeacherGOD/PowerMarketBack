package org.dev.powermarket.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.dev.powermarket.service.ReviewService;
import org.dev.powermarket.service.dto.CreateReviewRequest;
import org.dev.powermarket.service.dto.ReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Review management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    private final AuthorizedUserRepository userRepository;

    public ReviewController(ReviewService reviewService, AuthorizedUserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create a review", description = "Create a review for a completed rental")
    public ResponseEntity<ReviewDto> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        ReviewDto review = reviewService.createReview(email, request);
        return ResponseEntity.ok(review);
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get service reviews", description = "Get all reviews for a specific service")
    public ResponseEntity<Page<ReviewDto>> getServiceReviews(
            @PathVariable UUID serviceId,
            Pageable pageable) {
        Page<ReviewDto> reviews = reviewService.getServiceReviews(serviceId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<ReviewDto>> getUserReviews(
//            @RequestParam(required = false) UUID userId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails principal) {

        User currentUser = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));


        Page<ReviewDto> reviews = reviewService.getUserReviews(currentUser.getId(), pageable);
        return ResponseEntity.ok(reviews);
    }



    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review", description = "Get a specific review by ID")
    public ResponseEntity<ReviewDto> getReview(@PathVariable UUID reviewId) {
        ReviewDto review = reviewService.getReview(reviewId);
        return ResponseEntity.ok(review);
    }
}
