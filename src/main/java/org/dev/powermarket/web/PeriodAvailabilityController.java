package org.dev.powermarket.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.dto.request.AvailablePeriodSearchRequest;
import org.dev.powermarket.domain.dto.response.AvailablePeriodResponse;
import org.dev.powermarket.domain.dto.response.ServiceAvailabilityResponse;
import org.dev.powermarket.service.PeriodAvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/availability")
@Tag(name = "Availability", description = "Service availability period endpoints")
@RequiredArgsConstructor
public class PeriodAvailabilityController {

    private final PeriodAvailabilityService periodAvailabilityService;

    @PostMapping("/service/{serviceId}/available-periods")
    @Operation(summary = "Find available periods for service",
            description = "Find available periods for a service considering required capacity")
    public ResponseEntity<List<AvailablePeriodResponse>> findAvailablePeriods(
            @PathVariable UUID serviceId,
            @RequestBody AvailablePeriodSearchRequest request) {
        List<AvailablePeriodResponse> periods = periodAvailabilityService.findAvailablePeriods(serviceId, request);
        return ResponseEntity.ok(periods);
    }

    @GetMapping("/service/{serviceId}/detailed")
    @Operation(summary = "Get detailed service availability",
            description = "Get detailed availability information including available and reserved periods")
    public ResponseEntity<ServiceAvailabilityResponse> getServiceAvailability(
            @PathVariable UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ServiceAvailabilityResponse response = periodAvailabilityService.getServiceAvailability(
                serviceId, startDate, endDate);
        return ResponseEntity.ok(response);
    }
}
