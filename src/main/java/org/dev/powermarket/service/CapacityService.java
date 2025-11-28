package org.dev.powermarket.service;

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
public class CapacityService {

    private final ServiceRepository serviceRepository;
    private final ServiceAvailabilityPeriodRepository periodRepository;
    private final CapacityReservationRepository reservationRepository;

    public CapacityService(ServiceRepository serviceRepository,
                           ServiceAvailabilityPeriodRepository periodRepository,
                           CapacityReservationRepository reservationRepository) {
        this.serviceRepository = serviceRepository;
        this.periodRepository = periodRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<CapacityAvailabilityDto> getCapacityAvailability(UUID serviceId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        List<CapacityAvailabilityDto> result = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // Найти период для текущей даты
            Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository.findByServiceAndDate(service, currentDate);

            BigDecimal totalCapacity = BigDecimal.ZERO;
            BigDecimal reservedCapacity = BigDecimal.ZERO;
            List<CapacityAvailabilityDto.OccupiedSlot> occupiedSlots = new ArrayList<>();

            if (periodOpt.isPresent()) {
                ServiceAvailabilityPeriod period = periodOpt.get();
                totalCapacity = period.getTotalCapacity();

                // Получить забронированную мощность для этой даты
                reservedCapacity = reservationRepository.getReservedCapacityForPeriodAndDate(period, currentDate);

                // Получить детали бронирований
                List<CapacityReservation> reservations = reservationRepository.findByServiceAndDateRange(
                        service, currentDate, currentDate);

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

    @Transactional(readOnly = true)
    public boolean isCapacityAvailable(UUID serviceId, LocalDate startDate,
                                       LocalDate endDate, BigDecimal requiredCapacity) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        // Проверить каждый день в диапазоне
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository.findByServiceAndDate(service, currentDate);

            if (periodOpt.isEmpty()) {
                return false; // Нет периода доступности на эту дату
            }

            ServiceAvailabilityPeriod period = periodOpt.get();
            BigDecimal reservedCapacity = reservationRepository.getReservedCapacityForPeriodAndDate(period, currentDate);
            BigDecimal availableCapacity = period.getTotalCapacity().subtract(reservedCapacity);

            if (availableCapacity.compareTo(requiredCapacity) < 0) {
                return false; // Недостаточно доступной мощности
            }

            currentDate = currentDate.plusDays(1);
        }

        return true;
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, BigDecimal> getAvailableCapacityByDate(UUID serviceId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        Map<LocalDate, BigDecimal> result = new HashMap<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository.findByServiceAndDate(service, currentDate);

            BigDecimal availableCapacity = BigDecimal.ZERO;
            if (periodOpt.isPresent()) {
                ServiceAvailabilityPeriod period = periodOpt.get();
                BigDecimal reservedCapacity = reservationRepository.getReservedCapacityForPeriodAndDate(period, currentDate);
                availableCapacity = period.getTotalCapacity().subtract(reservedCapacity);
            }

            result.put(currentDate, availableCapacity);
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableCapacityForDate(UUID serviceId, LocalDate date) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        return getAvailableCapacityForDate(service, date);
    }

    // Приватная версия для внутреннего использования
    private BigDecimal getAvailableCapacityForDate(Service service, LocalDate date) {
        Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository.findByServiceAndDate(service, date);

        if (periodOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        ServiceAvailabilityPeriod period = periodOpt.get();
        BigDecimal reservedCapacity = reservationRepository.getReservedCapacityForPeriodAndDate(period, date);

        return period.getTotalCapacity().subtract(reservedCapacity);
    }
}
