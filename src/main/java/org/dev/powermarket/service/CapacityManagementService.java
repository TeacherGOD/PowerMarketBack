package org.dev.powermarket.service;

import jakarta.transaction.Transactional;
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
public class CapacityManagementService {

    private final ServiceAvailabilityPeriodRepository periodRepository;
    private final CapacityReservationRepository reservationRepository;
    private final ServiceRepository serviceRepository;

    public CapacityManagementService(ServiceAvailabilityPeriodRepository periodRepository,
                                     CapacityReservationRepository reservationRepository,
                                     ServiceRepository serviceRepository) {
        this.periodRepository = periodRepository;
        this.reservationRepository = reservationRepository;
        this.serviceRepository = serviceRepository;
    }

    /**
     * Создать период доступности
     */
    public ServiceAvailabilityPeriod createAvailabilityPeriod(UUID serviceId,
                                                              LocalDate startDate,
                                                              LocalDate endDate,
                                                              BigDecimal totalCapacity) {
        var service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        ServiceAvailabilityPeriod period = new ServiceAvailabilityPeriod();
        period.setService(service);
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        period.setTotalCapacity(totalCapacity);

        return periodRepository.save(period);
    }

    /**
     * Проверить доступность мощности на период
     */
    public boolean isCapacityAvailable(UUID serviceId, LocalDate startDate,
                                       LocalDate endDate, BigDecimal requiredCapacity) {
        var service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        // Проверяем что на КАЖДЫЙ день периода есть достаточная мощность
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            BigDecimal availableCapacity = getAvailableCapacityForDate(service, current);
            if (availableCapacity.compareTo(requiredCapacity) < 0) {
                return false;
            }
            current = current.plusDays(1);
        }

        return true;
    }

    /**
     * Забронировать мощность
     */
    public void reserveCapacity(Rental rental, LocalDate startDate,
                                LocalDate endDate, BigDecimal capacity) {
        var service = rental.getService();

        if (!isCapacityAvailable(service.getId(), startDate, endDate, capacity)) {
            throw new IllegalArgumentException("Not enough capacity available");
        }

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // Найти подходящий период для этой даты
            ServiceAvailabilityPeriod period = findPeriodForDate(service, current, capacity);

            // Создать бронирование
            CapacityReservation reservation = new CapacityReservation();
            reservation.setRental(rental);
            reservation.setAvailabilityPeriod(period);
            reservation.setReservationDate(current);
            reservation.setReservedCapacity(capacity);
            reservationRepository.save(reservation);

            current = current.plusDays(1);
        }
    }

    /**
     * Освободить забронированную мощность
     */
    public void releaseCapacity(Rental rental) {
        List<CapacityReservation> reservations = reservationRepository.findByRental(rental);
        for (CapacityReservation reservation : reservations) {
            reservationRepository.delete(reservation);
        }
    }

    /**
     * Получить доступную мощность на конкретную дату
     */
    public BigDecimal getAvailableCapacityForDate(UUID serviceId, LocalDate date) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        return getAvailableCapacityForDate(service, date);
    }

    /**
     * Получить доступную мощность на конкретную дату (внутренний метод)
     */
    private BigDecimal getAvailableCapacityForDate(Service service, LocalDate date) {
        Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository.findByServiceAndDate(service, date);

        if (periodOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        ServiceAvailabilityPeriod period = periodOpt.get();
        BigDecimal reservedCapacity = reservationRepository.getReservedCapacityForPeriodAndDate(period, date);

        return period.getTotalCapacity().subtract(reservedCapacity);
    }

    /**
     * Найти период для конкретной даты с достаточной доступной мощностью
     */
    private ServiceAvailabilityPeriod findPeriodForDate(Service service, LocalDate date, BigDecimal requiredCapacity) {
        // Найти все периоды, которые покрывают эту дату
        List<ServiceAvailabilityPeriod> periods = periodRepository.findOverlappingPeriods(service, date, date);

        // Найти период с достаточной доступной мощностью
        for (ServiceAvailabilityPeriod period : periods) {
            BigDecimal availableCapacity = getAvailableCapacityForDate(service, date);
            if (availableCapacity.compareTo(requiredCapacity) >= 0) {
                return period;
            }
        }

        throw new IllegalArgumentException("No available period found for date: " + date);
    }
}

