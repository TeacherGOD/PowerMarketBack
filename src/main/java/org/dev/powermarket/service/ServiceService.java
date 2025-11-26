package org.dev.powermarket.service;

import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailability;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.domain.enums.Role;
import org.dev.powermarket.domain.enums.ServiceCategory;
import org.dev.powermarket.repository.ServiceAvailabilityRepository;
import org.dev.powermarket.repository.ServiceRepository;
import org.dev.powermarket.integration.search.MlSearchClient;
import org.dev.powermarket.integration.search.MlSearchProperties;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.time.LocalDate;

import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.dev.powermarket.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceAvailabilityRepository availabilityRepository;
    private final AuthorizedUserRepository userRepository;
    private final MlSearchClient mlSearchClient;
    private final MlSearchProperties mlSearchProperties;

    public ServiceService(ServiceRepository serviceRepository,
                          ServiceAvailabilityRepository availabilityRepository,
                          AuthorizedUserRepository userRepository, MlSearchClient mlSearchClient, MlSearchProperties mlSearchProperties) {
        this.serviceRepository = serviceRepository;
        this.mlSearchClient = mlSearchClient;
        this.mlSearchProperties = mlSearchProperties;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ServiceDto createService(String email, CreateServiceRequest request) {
        User supplier = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (supplier.getRole() != Role.SUPPLIER) {
            throw new AccessDeniedException("Only suppliers can create services");
        }

        Service service = new Service();
        service.setTitle(request.getTitle());
        service.setDescription(request.getDescription());
        service.setCategory(request.getCategory());
        service.setPricePerDay(request.getPricePerDay());
        service.setLocation(request.getLocation());
        service.setCapacity(request.getCapacity());
        service.setTechnicalSpecs(request.getTechnicalSpecs());
        service.setAvailableCapacity(request.getCapacity());
        service.setSupplier(supplier);
        service.setIsActive(true);

        Service saved = serviceRepository.save(service);

        if (request.getAvailabilities() != null) {
            for (CreateServiceRequest.AvailabilityPeriod period : request.getAvailabilities()) {
                LocalDate currentDate = period.getStartDate();
                while (!currentDate.isAfter(period.getEndDate())) {
                    ServiceAvailability availability = new ServiceAvailability();
                    availability.setService(saved);
                    availability.setAvailableDate(currentDate);
                    availability.setIsReserved(false);
                    availabilityRepository.save(availability);
                    currentDate = currentDate.plusDays(1);
                }
            }
        }

        return toDto(saved);
    }

    @Transactional
    public ServiceDto updateService(String email, UUID serviceId, UpdateServiceRequest request) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!service.getSupplier().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only update your own services");
        }

        if (request.getTitle() != null) service.setTitle(request.getTitle());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getPricePerDay() != null) service.setPricePerDay(request.getPricePerDay());
        if (request.getLocation() != null) service.setLocation(request.getLocation());
        if (request.getCapacity() != null) service.setCapacity(request.getCapacity());
        if (request.getTechnicalSpecs() != null) service.setTechnicalSpecs(request.getTechnicalSpecs());
        if (request.getActive() != null) service.setIsActive(request.getActive());

        Service updated = serviceRepository.save(service);
        return toDto(updated);
    }

    @Transactional(readOnly = true)
    public Page<ServiceDto> searchServices(String keyword, ServiceCategory category,
                                           LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // ML-based search integration: if enabled and keyword present, delegate to ML service
        if (keyword != null && !keyword.isBlank() && mlSearchProperties != null && mlSearchProperties.isEnabled()) {
            var ids = mlSearchClient.searchServiceIds(keyword, pageable.getPageNumber() + 1, pageable.getPageSize());
            if (!ids.isEmpty()) {
                var list = serviceRepository.findByIdInAndIsActiveTrueAndDeletedFalse(ids);
                // Preserve ML order
                var order = new java.util.HashMap<java.util.UUID, Integer>();
                for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
                var sorted = list.stream()
                        .sorted(Comparator.comparingInt(s -> order.getOrDefault(s.getId(), Integer.MAX_VALUE)))
                        .map(this::toDto)
                        .collect(Collectors.toList());
                // Create Page
                return new org.springframework.data.domain.PageImpl<>(sorted, pageable, sorted.size());
            }
        }


        Page<Service> services;

        if (keyword != null && !keyword.isBlank()) {
            services = serviceRepository.searchServices(keyword, pageable);
        } else if (category != null) {
            services = serviceRepository.findByIsActiveTrueAndCategoryAndDeletedFalse(category, pageable);
        } else {
            services = serviceRepository.findByIsActiveTrueAndDeletedFalse(pageable);
        }

        return services.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ServiceDto getService(UUID serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        return toDto(service);
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> getMyServices(String email) {
        User supplier = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return serviceRepository.findBySupplierAndIsActiveTrueAndDeletedFalse(supplier).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deleteService(String email, UUID serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!service.getSupplier().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only delete your own services");
        }

        service.setIsActive(false);
        service.setDeleted(true);
        serviceRepository.save(service);
    }

    private ServiceDto toDto(Service service) {
        ServiceDto dto = new ServiceDto();
        dto.setId(service.getId());
        dto.setTitle(service.getTitle());
        dto.setDescription(service.getDescription());
        dto.setCategory(service.getCategory());
        dto.setPricePerDay(service.getPricePerDay());
        dto.setLocation(service.getLocation());
        dto.setTotalCapacity(service.getCapacity());
        dto.setTotalCapacityUnits(service.getTotalCapacityUnits());
        dto.setTechnicalSpecs(service.getTechnicalSpecs());
        dto.setSupplierId(service.getSupplier().getId());
        dto.setSupplierName(service.getSupplier().getFullName());
        dto.setActive(service.getIsActive());
        dto.setAverageRating(service.getAverageRating());
        dto.setTotalReviews(service.getTotalReviews());
        dto.setCreatedAt(service.getCreatedAt());
        dto.setAvailableCapacity(service.getAvailableCapacity());

        List<ServiceAvailability> freeAvailabilities = availabilityRepository
                .findByServiceAndIsReservedFalse(service);
        dto.setAvailableCapacityUnits(freeAvailabilities.isEmpty() ? 0 : service.getTotalCapacityUnits());

        List<ServiceAvailabilityDto> availabilities = freeAvailabilities.stream()
                .map(this::toAvailabilityDto)
                .collect(Collectors.toList());
        dto.setAvailabilities(availabilities);

        return dto;
    }

    private ServiceAvailabilityDto toAvailabilityDto(ServiceAvailability availability) {
        return new ServiceAvailabilityDto(
                availability.getId(),
                availability.getAvailableDate(),
                availability.getIsReserved()
        );
    }

    public List<ServiceDto> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
