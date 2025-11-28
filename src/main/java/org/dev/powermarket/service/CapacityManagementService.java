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
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        // Проверяем каждый день в диапазоне
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            BigDecimal availableCapacity = getAvailableCapacityForDateInternal(service, current);
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
        Service service = rental.getService();

        if (!isCapacityAvailable(service.getId(), startDate, endDate, capacity)) {
            throw new IllegalArgumentException("Not enough capacity available");
        }

        // ✅ СОЗДАЕМ ОДНУ ЗАПИСЬ CapacityReservation на весь период
        CapacityReservation reservation = new CapacityReservation();
        reservation.setRental(rental);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setReservedCapacity(capacity);

        // Находим подходящий период доступности для резервирования
        // (можно выбрать любой период, который покрывает даты бронирования)
        ServiceAvailabilityPeriod period = findSuitablePeriodForReservation(service, startDate, endDate);
        reservation.setAvailabilityPeriod(period);

        reservationRepository.save(reservation);

        // ✅ ОБНОВЛЯЕМ доступную мощность в периоде
        // ВАЖНО: Это упрощенный подход - в реальной системе нужно аккуратно управлять мощностью в периодах
        BigDecimal newAvailableCapacity = period.getAvailableCapacity().subtract(capacity);
        period.setAvailableCapacity(newAvailableCapacity);
        periodRepository.save(period);
    }

    /**
     * Найти подходящий период для резервирования
     */
    private ServiceAvailabilityPeriod findSuitablePeriodForReservation(Service service,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate) {
        // Ищем периоды, которые пересекаются с нашим диапазоном дат
        List<ServiceAvailabilityPeriod> periods = periodRepository.findOverlappingPeriods(
                service, startDate, endDate);

        if (periods.isEmpty()) {
            throw new IllegalArgumentException("No availability periods found for the selected dates");
        }

        // Берем первый подходящий период (в реальной системе может быть сложнее)
        return periods.getFirst();
    }

    /**
     * Освободить забронированную мощность
     */
    public void releaseCapacity(Rental rental) {
        List<CapacityReservation> reservations = reservationRepository.findByRental(rental);

        for (CapacityReservation reservation : reservations) {
            ServiceAvailabilityPeriod period = reservation.getAvailabilityPeriod();

            // ✅ ВОЗВРАЩАЕМ мощность в период
            BigDecimal newAvailableCapacity = period.getAvailableCapacity().add(reservation.getReservedCapacity());
            period.setAvailableCapacity(newAvailableCapacity);
            periodRepository.save(period);

            // Удаляем бронирование
            reservationRepository.delete(reservation);
        }
    }


    private List<ServiceAvailabilityPeriod> findOrCreatePeriodsForReservation(
            Service service, LocalDate startDate, LocalDate endDate, BigDecimal requiredCapacity) {

        List<ServiceAvailabilityPeriod> result = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Найти период, покрывающий текущую дату
            Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository
                    .findByServiceAndDate(service, current);

            if (periodOpt.isEmpty()) {
                // Создать период по умолчанию (например, на год)
                ServiceAvailabilityPeriod newPeriod = createDefaultPeriod(service, current);
                periodRepository.save(newPeriod);
                result.add(newPeriod);
            } else {
                ServiceAvailabilityPeriod period = periodOpt.get();

                // Если период слишком большой, разбить его
                if (!period.getStartDate().equals(current) || !period.getEndDate().equals(current)) {
                    period = splitPeriodForDate(period, current);
                }

                // Проверить достаточность мощности
                if (period.getAvailableCapacity().compareTo(requiredCapacity) < 0) {
                    throw new IllegalArgumentException("Insufficient capacity on date: " + current);
                }

                result.add(period);
            }

            current = current.plusDays(1);
        }

        return result;
    }

    /**
     * Разделить период на конкретную дату
     */
    private ServiceAvailabilityPeriod splitPeriodForDate(ServiceAvailabilityPeriod original, LocalDate targetDate) {
        // Создаем период ДО целевой даты
        if (original.getStartDate().isBefore(targetDate)) {
            ServiceAvailabilityPeriod before = new ServiceAvailabilityPeriod();
            before.setService(original.getService());
            before.setStartDate(original.getStartDate());
            before.setEndDate(targetDate.minusDays(1));
            before.setTotalCapacity(original.getTotalCapacity());
            before.setAvailableCapacity(original.getAvailableCapacity());
            periodRepository.save(before);
        }

        // Создаем период ПОСЛЕ целевой даты
        if (original.getEndDate().isAfter(targetDate)) {
            ServiceAvailabilityPeriod after = new ServiceAvailabilityPeriod();
            after.setService(original.getService());
            after.setStartDate(targetDate.plusDays(1));
            after.setEndDate(original.getEndDate());
            after.setTotalCapacity(original.getTotalCapacity());
            after.setAvailableCapacity(original.getAvailableCapacity());
            periodRepository.save(after);
        }

        // Модифицируем оригинальный период для целевой даты
        original.setStartDate(targetDate);
        original.setEndDate(targetDate);
        return periodRepository.save(original);
    }

    /**
     * Создать период по умолчанию
     */
    private ServiceAvailabilityPeriod createDefaultPeriod(Service service, LocalDate date) {
        ServiceAvailabilityPeriod period = new ServiceAvailabilityPeriod();
        period.setService(service);
        period.setStartDate(date);
        period.setEndDate(date.plusYears(1)); // Период на год вперед
        period.setTotalCapacity(service.getMaxCapacity());
        period.setAvailableCapacity(service.getMaxCapacity());
        return period;
    }

    /**
     * Получить доступную мощность на конкретную дату
     */
    public BigDecimal getAvailableCapacityForDate(UUID serviceId, LocalDate date) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        return getAvailableCapacityForDateInternal(service, date);
    }

    /**
     * Приватная версия для внутреннего использования
     */
    private BigDecimal getAvailableCapacityForDateInternal(Service service, LocalDate date) {
        Optional<ServiceAvailabilityPeriod> periodOpt = periodRepository.findByServiceAndDate(service, date);

        if (periodOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        ServiceAvailabilityPeriod period = periodOpt.get();
        BigDecimal reservedCapacity = reservationRepository.getReservedCapacityForPeriodAndDate(period, date);

        return period.getTotalCapacity().subtract(reservedCapacity);
    }


}

