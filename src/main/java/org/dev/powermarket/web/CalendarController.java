package org.dev.powermarket.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.dto.request.AvailablePeriodSearchRequest;
import org.dev.powermarket.domain.dto.request.NextAvailableDatesRequest;
import org.dev.powermarket.domain.dto.request.ServiceSearchRequest;
import org.dev.powermarket.domain.dto.response.*;
import org.dev.powermarket.service.PeriodAvailabilityService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/calendar")
@Tag(name = "Calendar", description = "Service availability calendar endpoints")
@RequiredArgsConstructor
public class CalendarController {

    private final PeriodAvailabilityService periodAvailabilityService;

    @GetMapping("/service/{serviceId}/periods")
    @Operation(summary = "Get service availability periods",
            description = "Get detailed period-based availability for a service")
    public ResponseEntity<ServiceAvailabilityResponse> getServiceAvailabilityPeriods(
            @PathVariable UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ServiceAvailabilityResponse response = periodAvailabilityService.getServiceAvailability(
                serviceId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/service/{serviceId}/find-available")
    @Operation(summary = "Find available periods with capacity",
            description = "Find available periods that match required capacity")
    public ResponseEntity<List<AvailablePeriodResponse>> findAvailablePeriodsWithCapacity(
            @PathVariable UUID serviceId,
            @RequestBody AvailablePeriodSearchRequest request) {
        List<AvailablePeriodResponse> periods = periodAvailabilityService.findAvailablePeriods(serviceId, request);
        return ResponseEntity.ok(periods);
    }
}
