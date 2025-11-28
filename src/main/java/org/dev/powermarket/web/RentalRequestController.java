package org.dev.powermarket.web;

import org.dev.powermarket.service.RentalRequestService;
import org.dev.powermarket.service.RentalService;
import org.dev.powermarket.service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/rental-requests","/api/rental-requests"})
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    private final RentalService rentalService;

    public RentalRequestController(RentalRequestService rentalRequestService, RentalService rentalService) {
        this.rentalRequestService = rentalRequestService;
        this.rentalService = rentalService;
    }

    @PostMapping
    public ResponseEntity<RentalRequestDto> createRequest(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody CreateRentalRequestRequest request) {
        System.out.println(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rentalRequestService.createRentalRequest(principal.getUsername(), request));
    }

    @PostMapping("/{requestId}/respond")
    public ResponseEntity<RentalRequestDto> respondToRequest(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID requestId,
            @RequestBody RespondToRentalRequestRequest request) {
        return ResponseEntity.ok(rentalRequestService.respondToRequest(
                principal.getUsername(), requestId, request));
    }

    @GetMapping("")
    public ResponseEntity<List<RentalRequestDto>> getMyRequests(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(rentalRequestService.getMyRequests(principal.getUsername()));
    }

    @GetMapping("/sent")
    @Operation(summary = "Get sent requests", description = "Get all rental requests sent by the authenticated tenant")
    public ResponseEntity<List<RentalRequestDto>> getSentRequests(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(rentalRequestService.getSentRequests(principal.getUsername()));
    }

    @GetMapping("/received")
    @Operation(summary = "Get received requests", description = "Get all rental requests received by the authenticated landlord")
    public ResponseEntity<List<RentalRequestDto>> getReceivedRequests(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(rentalRequestService.getReceivedRequests(principal.getUsername()));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<RentalRequestDto> getRequest(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(rentalRequestService.getRequest(principal.getUsername(), requestId));
    }
}
