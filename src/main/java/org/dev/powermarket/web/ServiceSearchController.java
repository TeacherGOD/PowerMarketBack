package org.dev.powermarket.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.dto.request.ServiceSearchByCategoryRequest;
import org.dev.powermarket.domain.dto.response.ServiceSearchResultResponse;
import org.dev.powermarket.domain.enums.AvailabilityStatus;
import org.dev.powermarket.domain.enums.ServiceCategory;
import org.dev.powermarket.service.ServiceSearchService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services/search")
@Tag(name = "Service Search", description = "Advanced service search endpoints")
@RequiredArgsConstructor
public class ServiceSearchController {

    private final ServiceSearchService serviceSearchService;

    @PostMapping("/by-category-capacity")
    @Operation(summary = "Search services by category and capacity",
            description = "Search services filtered by category and minimum capacity")
    public ResponseEntity<Page<ServiceSearchResultResponse>> searchByCategoryAndCapacity(
            @RequestBody ServiceSearchByCategoryRequest searchRequest) {
        Page<ServiceSearchResultResponse> results =
                serviceSearchService.searchServicesByCategoryAndCapacity(searchRequest);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/category/{category}/min-capacity/{minCapacity}")
    @Operation(summary = "Search services by category and capacity (simple)",
            description = "Simple search by category and minimum capacity using path variables")
    public ResponseEntity<Page<ServiceSearchResultResponse>> searchByCategoryAndCapacitySimple(
            @PathVariable ServiceCategory category,
            @PathVariable BigDecimal minCapacity,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        ServiceSearchByCategoryRequest searchRequest = new ServiceSearchByCategoryRequest(
                category, minCapacity, null, null, page, size);

        Page<ServiceSearchResultResponse> results =
                serviceSearchService.searchServicesByCategoryAndCapacity(searchRequest);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/category/{category}/top-available")
    @Operation(summary = "Find top available services in category",
            description = "Find services with highest available capacity in a category")
    public ResponseEntity<List<ServiceSearchResultResponse>> findTopAvailableServices(
            @PathVariable ServiceCategory category,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) BigDecimal minCapacity) {

        if (minCapacity == null) {
            minCapacity = BigDecimal.ZERO;
        }

        List<ServiceSearchResultResponse> results =
                serviceSearchService.findTopServicesByCategoryAndCapacity(category, minCapacity, limit);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/category/{category}/grouped-by-availability")
    @Operation(summary = "Find services grouped by availability",
            description = "Find services in category grouped by availability status")
    public ResponseEntity<Map<AvailabilityStatus, List<ServiceSearchResultResponse>>> findServicesGroupedByAvailability(
            @PathVariable ServiceCategory category,
            @RequestParam(required = false) BigDecimal minCapacity) {

        if (minCapacity == null) {
            minCapacity = BigDecimal.ZERO;
        }

        Map<AvailabilityStatus, List<ServiceSearchResultResponse>> results =
                serviceSearchService.findServicesGroupedByAvailability(category, minCapacity);
        return ResponseEntity.ok(results);
    }

}