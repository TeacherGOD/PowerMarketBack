package org.dev.powermarket.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.CapacityReservation;
import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailabilityPeriod;
import org.dev.powermarket.repository.CapacityReservationRepository;
import org.dev.powermarket.repository.ServiceAvailabilityPeriodRepository;
import org.dev.powermarket.repository.ServiceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@org.springframework.stereotype.Service
@Transactional
@RequiredArgsConstructor
public class CapacityManagementService {

    private final ServiceAvailabilityPeriodRepository periodRepository;
    private final CapacityReservationRepository reservationRepository;
    private final ServiceRepository serviceRepository;



    /**
     * Забронировать мощность на период (ОДНА запись)
     */
    public void reserveCapacity(Rental rental, LocalDate startDate,
                                LocalDate endDate, BigDecimal capacity) {
        Service service = rental.getService();

        if (!isCapacityAvailable(service.getId(), startDate, endDate, capacity)) {
            throw new IllegalArgumentException("Not enough capacity available");
        }

        // Создаем ОДНУ запись бронирования на весь период
        CapacityReservation reservation = new CapacityReservation();
        reservation.setRental(rental);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setReservedCapacity(capacity);

        reservationRepository.save(reservation);
    }

    /**
     * Освободить забронированную мощность
     */
    public void releaseCapacity(Rental rental) {
        List<CapacityReservation> reservations = reservationRepository.findByRental(rental);
        reservationRepository.deleteAll(reservations);
    }

    /**
     * Получить доступную мощность на конкретную дату
     */
    public BigDecimal getAvailableCapacityForDate(UUID serviceId, LocalDate date) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        // Находим период, покрывающий эту дату
        Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository.findByServiceAndDate(service, date);
        if (periodOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCapacity = periodOpt.get().getTotalCapacity();
        BigDecimal reservedCapacity = getReservedCapacityForDate(service, date);

        return totalCapacity.subtract(reservedCapacity);
    }

    /**
     * Проверить доступность мощности на период
     */
    public boolean isCapacityAvailable(UUID serviceId, LocalDate startDate,
                                       LocalDate endDate, BigDecimal requiredCapacity) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        // Находим все периоды, которые пересекаются с запрошенным диапазоном
        List<ServiceAvailabilityPeriod> periods = periodRepository.findOverlappingPeriods(
                service, startDate, endDate);

        if (periods.isEmpty()) {
            return false;
        }

        // Для каждого дня в диапазоне проверяем доступность
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            BigDecimal availableCapacity = getAvailableCapacityForDate(serviceId, current);
            if (availableCapacity.compareTo(requiredCapacity) < 0) {
                return false;
            }
            current = current.plusDays(1);
        }

        return true;
    }

    /**
     * Получить сумму забронированной мощности на конкретную дату
     */
    private BigDecimal getReservedCapacityForDate(Service service, LocalDate date) {
        return reservationRepository.getReservedCapacityForDate(service, date);
    }
}

