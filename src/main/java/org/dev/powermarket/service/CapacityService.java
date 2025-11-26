package org.dev.powermarket.service;

import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailability;
import org.dev.powermarket.repository.ServiceAvailabilityRepository;
import org.dev.powermarket.repository.ServiceRepository;
import org.dev.powermarket.service.dto.CapacityAvailabilityDto;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class CapacityService {

    private final ServiceRepository serviceRepository;
    private final ServiceAvailabilityRepository availabilityRepository;

    public CapacityService(ServiceRepository serviceRepository,
                           ServiceAvailabilityRepository availabilityRepository) {
        this.serviceRepository = serviceRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional(readOnly = true)
    public List<CapacityAvailabilityDto> getCapacityAvailability(UUID serviceId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        List<ServiceAvailability> allAvailabilities = availabilityRepository
                .findByServiceAndDateRange(service, startDate, endDate);

        List<ServiceAvailability> reservedAvailabilities = availabilityRepository
                .findReservedAvailabilities(service, startDate, endDate);

        // Group by date
        Map<LocalDate, List<ServiceAvailability>> reservedByDate = reservedAvailabilities.stream()
                .collect(Collectors.groupingBy(ServiceAvailability::getAvailableDate));

        List<CapacityAvailabilityDto> result = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<ServiceAvailability> reserved = reservedByDate.getOrDefault(currentDate, List.of());

            int totalCapacity = service.getTotalCapacityUnits();
            int occupiedCapacity = reserved.size();
            int availableCapacity = totalCapacity - occupiedCapacity;

            List<CapacityAvailabilityDto.OccupiedSlot> occupiedSlots = reserved.stream()
                    .filter(sa -> sa.getReservedByRental() != null)
                    .map(sa -> {
                        Rental rental = sa.getReservedByRental();
                        return new CapacityAvailabilityDto.OccupiedSlot(
                                rental.getStartDate(),
                                rental.getEndDate(),
                                rental.getTenant().getFullName(),
                                1 // Each availability represents 1 capacity unit
                        );
                    })
                    .collect(Collectors.toList());

            CapacityAvailabilityDto dto = new CapacityAvailabilityDto(
                    currentDate,
                    totalCapacity,
                    availableCapacity,
                    occupiedCapacity,
                    occupiedSlots
            );

            result.add(dto);
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public boolean isCapacityAvailable(UUID serviceId, LocalDate startDate, LocalDate endDate) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

        return availabilityRepository.isDateRangeAvailable(service, startDate, endDate, days);
    }
}
