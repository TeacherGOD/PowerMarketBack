package org.dev.powermarket.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dev.powermarket.service.CapacityService;
import org.dev.powermarket.service.dto.CapacityAvailabilityDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/capacity")
@Tag(name = "Capacity", description = "Capacity availability endpoints")
public class CapacityController {

    private final CapacityService capacityService;

    public CapacityController(CapacityService capacityService) {
        this.capacityService = capacityService;
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get capacity availability",
            description = "Get detailed capacity availability showing free and occupied slots")
    public ResponseEntity<List<CapacityAvailabilityDto>> getCapacityAvailability(
            @PathVariable UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CapacityAvailabilityDto> availability =
                capacityService.getCapacityAvailability(serviceId, startDate, endDate);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/service/{serviceId}/check")
    @Operation(summary = "Check if capacity is available",
            description = "Check if service has available capacity for the given date range")
    public ResponseEntity<Boolean> checkCapacityAvailable(
            @PathVariable UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        boolean available = capacityService.isCapacityAvailable(serviceId, startDate, endDate);
        return ResponseEntity.ok(available);
    }
}
