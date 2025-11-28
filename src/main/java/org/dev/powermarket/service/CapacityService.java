package org.dev.powermarket.service;

import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.CapacityReservation;
import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailabilityPeriod;
import org.dev.powermarket.domain.dto.response.CapacityAvailabilityResponse;
import org.dev.powermarket.repository.CapacityReservationRepository;
import org.dev.powermarket.repository.ServiceAvailabilityPeriodRepository;
import org.dev.powermarket.repository.ServiceRepository;
import org.dev.powermarket.service.dto.CapacityAvailabilityDto;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CapacityService {

    private final ServiceRepository serviceRepository;
    private final ServiceAvailabilityPeriodRepository periodRepository;
    private final CapacityReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<CapacityAvailabilityResponse> getCapacityAvailability(UUID serviceId,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        List<CapacityAvailabilityResponse> result = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // Найти период для текущей даты
            Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository.findByServiceAndDate(service, currentDate);

            BigDecimal totalCapacity = BigDecimal.ZERO;
            BigDecimal reservedCapacity = BigDecimal.ZERO;
            List<CapacityAvailabilityResponse.OccupiedSlot> occupiedSlots = new ArrayList<>();

            if (periodOpt.isPresent()) {
                ServiceAvailabilityPeriod period = periodOpt.get();
                totalCapacity = period.getTotalCapacity();

                // Получить забронированную мощность для этой даты
                reservedCapacity = reservationRepository.getReservedCapacityForDate(service, currentDate);

                // Получить детали бронирований
                List<CapacityReservation> reservations = reservationRepository.findByServiceAndDateRange(
                        service, currentDate, currentDate);

                occupiedSlots = reservations.stream()
                        .map(reservation -> {
                            Rental rental = reservation.getRental();
                            return new CapacityAvailabilityResponse.OccupiedSlot(
                                    rental.getStartDate(),
                                    rental.getEndDate(),
                                    rental.getTenant().getFullName(),
                                    reservation.getReservedCapacity()
                            );
                        })
                        .toList();
            }

            BigDecimal availableCapacity = totalCapacity.subtract(reservedCapacity);

            CapacityAvailabilityResponse response = new CapacityAvailabilityResponse(
                    currentDate,
                    totalCapacity,
                    availableCapacity,
                    reservedCapacity,
                    occupiedSlots
            );

            result.add(response);
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }
    /**
     * Получить сумму забронированной мощности на конкретную дату
     */
    private BigDecimal getReservedCapacityForDate(Service service, LocalDate date) {
        List<CapacityReservation> reservations = reservationRepository
                .findOverlappingReservations(service, date, date);

        return reservations.stream()
                .map(CapacityReservation::getReservedCapacity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
