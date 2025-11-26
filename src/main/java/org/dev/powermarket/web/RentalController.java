package org.dev.powermarket.web;

import org.dev.powermarket.service.RentalService;
import org.dev.powermarket.service.dto.RentalDto;
import org.dev.powermarket.service.dto.RentalStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/rentals","/api/rentals"})
@SecurityRequirement(name = "bearerAuth")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping("/my")
    @Operation(summary = "Get my rentals", description = "Get all rentals for the authenticated user")
    public ResponseEntity<List<RentalDto>> getMyRentals(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(rentalService.getMyRentals(principal.getUsername()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get rental statistics", description = "Get rental statistics for the authenticated user")
    public ResponseEntity<RentalStatsDto> getRentalStats(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(rentalService.getRentalStats(principal.getUsername()));
    }

    @GetMapping("/{rentalId}")
    @Operation(summary = "Get rental", description = "Get a specific rental by ID")
    public ResponseEntity<RentalDto> getRental(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID rentalId) {
        return ResponseEntity.ok(rentalService.getRental(principal.getUsername(), rentalId));
    }

    @PostMapping("/{rentalId}/approve")
    @Operation(summary = "Confirm rental", description = "Confirm rental agreement (supplier or tenant)")
    public ResponseEntity<RentalDto> confirmRental(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID rentalId) {
        return ResponseEntity.ok(rentalService.confirmRental(principal.getUsername(), rentalId));
    }

    @PostMapping("/{rentalId}/start")
    @Operation(summary = "Start rental", description = "Start the rental period (system/admin only)")
    public ResponseEntity<RentalDto> startRental(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(rentalService.startRental(rentalId));
    }

    @PostMapping("/{rentalId}/complete")
    @Operation(summary = "Complete rental", description = "Mark rental as completed (system/admin only)")
    public ResponseEntity<RentalDto> completeRental(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(rentalService.completeRental(rentalId));
    }
}
