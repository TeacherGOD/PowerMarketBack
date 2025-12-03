package org.dev.powermarket.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dev.powermarket.domain.dto.request.ServiceSearchByCategoryRequest;
import org.dev.powermarket.domain.dto.response.ServiceSearchResultResponse;
import org.dev.powermarket.domain.enums.ServiceCategory;
import org.dev.powermarket.service.ServiceSearchService;
import org.dev.powermarket.service.ServiceService;
import org.dev.powermarket.service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/services","/api/services"})
@RequiredArgsConstructor
@Slf4j
public class ServiceController {


    private final ServiceService serviceService;
    private final ServiceSearchService serviceSearchService;



    @PostMapping
    public ResponseEntity<ServiceDto> createService(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody CreateServiceRequest request) {
        log.info(String.valueOf(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceService.createService(principal.getUsername(), request));
    }



    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceDto> updateService(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID serviceId,
            @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(serviceService.updateService(principal.getUsername(), serviceId, request));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ServiceDto>> searchServices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(serviceService.searchServices(keyword, category, startDate, endDate, pageable));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceDto> getService(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(serviceService.getService(serviceId));
    }

    @GetMapping
    public ResponseEntity<List<ServiceDto>> getMyServices(@AuthenticationPrincipal UserDetails principal) {
        String role = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("")
                .toUpperCase();
        log.info(role);

        if (role.contains("SUPPLIER")) {
            return ResponseEntity.ok(serviceService.getMyServices(principal.getUsername()));
        }  else {
            return ResponseEntity.ok(serviceService.getAllServices());
        }
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> deleteService(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID serviceId) {
        serviceService.deleteService(principal.getUsername(), serviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search/category-capacity")
    @Operation(summary = "Search services by category and capacity",
            description = "Search services by category and minimum capacity requirement")
    public ResponseEntity<Page<ServiceSearchResultResponse>> searchServicesByCategoryAndCapacity(
            @RequestBody ServiceSearchByCategoryRequest searchRequest) {
        Page<ServiceSearchResultResponse> results =
                serviceSearchService.searchServicesByCategoryAndCapacity(searchRequest);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/category/{category}/capacity/{minCapacity}")
    @Operation(summary = "Quick search by category and capacity",
            description = "Quick search using path variables for category and minimum capacity")
    public ResponseEntity<Page<ServiceSearchResultResponse>> quickSearchByCategoryAndCapacity(
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
}
