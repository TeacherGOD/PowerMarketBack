package org.dev.powermarket.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.dto.request.CapacityCheckRequest;
import org.dev.powermarket.domain.dto.response.CapacityAvailabilityResponse;
import org.dev.powermarket.service.CapacityManagementService;
import org.dev.powermarket.service.CapacityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/capacity")
@Tag(name = "Capacity", description = "Capacity availability endpoints")
@RequiredArgsConstructor
public class CapacityController {

    private final CapacityService capacityService;
    private final CapacityManagementService capacityManagementService;

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get capacity availability",
            description = "Get detailed capacity availability showing free and occupied slots")
    public ResponseEntity<List<CapacityAvailabilityResponse>> getCapacityAvailability(
            @PathVariable UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CapacityAvailabilityResponse> availability =
                capacityService.getCapacityAvailability(serviceId, startDate, endDate);
        return ResponseEntity.ok(availability);
    }

    @PostMapping("/service/{serviceId}/check")
    @Operation(summary = "Check if capacity is available",
            description = "Check if service has available capacity for the given date range and required capacity")
    public ResponseEntity<Boolean> checkCapacityAvailable(
            @PathVariable UUID serviceId,
            @RequestBody CapacityCheckRequest request) {
        boolean available = capacityManagementService.isCapacityAvailable(
                serviceId, request.startDate(), request.endDate(), request.requiredCapacity());
        return ResponseEntity.ok(available);
    }
}
