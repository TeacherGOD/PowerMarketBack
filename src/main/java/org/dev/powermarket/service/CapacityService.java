package org.dev.powermarket.service;

import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.CapacityReservation;
import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailabilityPeriod;
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
    public List<CapacityAvailabilityDto> getCapacityAvailability(UUID serviceId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        List<CapacityAvailabilityDto> result = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository
                    .findByServiceAndDate(service, currentDate);

            BigDecimal totalCapacity = BigDecimal.ZERO;
            BigDecimal reservedCapacity = BigDecimal.ZERO;
            List<CapacityAvailabilityDto.OccupiedSlot> occupiedSlots = new ArrayList<>();

            if (periodOpt.isPresent()) {
                ServiceAvailabilityPeriod period = periodOpt.get();
                totalCapacity = period.getTotalCapacity();

                // ✅ НОВАЯ ЛОГИКА: находим бронирования на эту дату
                reservedCapacity = getReservedCapacityForDate(service, currentDate);

                // Получить детали бронирований
                List<CapacityReservation> reservations = reservationRepository
                        .findOverlappingReservations(service, currentDate, currentDate);

                occupiedSlots = reservations.stream()
                        .map(reservation -> {
                            Rental rental = reservation.getRental();
                            return new CapacityAvailabilityDto.OccupiedSlot(
                                    rental.getStartDate(),
                                    rental.getEndDate(),
                                    rental.getTenant().getFullName(),
                                    reservation.getReservedCapacity()
                            );
                        })
                        .collect(Collectors.toList());
            }

            BigDecimal availableCapacity = totalCapacity.subtract(reservedCapacity);

            CapacityAvailabilityDto dto = new CapacityAvailabilityDto(
                    currentDate,
                    totalCapacity,
                    availableCapacity,
                    reservedCapacity,
                    occupiedSlots
            );

            result.add(dto);
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
