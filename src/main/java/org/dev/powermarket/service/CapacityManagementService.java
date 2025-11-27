package org.dev.powermarket.service;

import jakarta.transaction.Transactional;
import org.dev.powermarket.domain.CapacityReservation;
import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.ServiceAvailabilityPeriod;
import org.dev.powermarket.repository.CapacityReservationRepository;
import org.dev.powermarket.repository.ServiceAvailabilityPeriodRepository;
import org.dev.powermarket.repository.ServiceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
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
        period.setAvailableCapacity(totalCapacity); // Изначально вся мощность доступна

        return periodRepository.save(period);
    }

    /**
     * Проверить доступность мощности на период
     */
    public boolean isCapacityAvailable(UUID serviceId, LocalDate startDate,
                                       LocalDate endDate, BigDecimal requiredCapacity) {
        var service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        List<ServiceAvailabilityPeriod> availablePeriods =
                periodRepository.findAvailablePeriodsWithCapacity(service, startDate, endDate, requiredCapacity);

        // Проверяем что на КАЖДЫЙ день периода есть достаточная мощность
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            LocalDate finalCurrent = current;
            boolean dayAvailable = availablePeriods.stream()
                    .anyMatch(period -> period.overlapsWith(finalCurrent, finalCurrent) &&
                            getAvailableCapacityForDate(service, finalCurrent).compareTo(requiredCapacity) >= 0);

            if (!dayAvailable) {
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
            ServiceAvailabilityPeriod period = findOrSplitPeriodForDate(service, current, capacity);

            // Создать бронирование
            CapacityReservation reservation = new CapacityReservation();
            reservation.setRental(rental);
            reservation.setAvailabilityPeriod(period);
            reservation.setReservationDate(current);
            reservation.setReservedCapacity(capacity);
            reservationRepository.save(reservation);

            // Обновить доступную мощность в периоде
            period.setAvailableCapacity(period.getAvailableCapacity().subtract(capacity));
            periodRepository.save(period);

            current = current.plusDays(1);
        }
    }

    /**
     * Освободить забронированную мощность
     */
    public void releaseCapacity(Rental rental) {
        List<CapacityReservation> reservations = reservationRepository.findByRental(rental);

        Set<ServiceAvailabilityPeriod> updatedPeriods = new HashSet<>();

        for (CapacityReservation reservation : reservations) {
            ServiceAvailabilityPeriod period = reservation.getAvailabilityPeriod();

            // Вернуть мощность
            period.setAvailableCapacity(period.getAvailableCapacity().add(reservation.getReservedCapacity()));
            ServiceAvailabilityPeriod savedPeriod = periodRepository.save(period);
            updatedPeriods.add(savedPeriod);

            // Удалить бронирование
            reservationRepository.delete(reservation);
        }

        // Объединить все обновленные периоды
        for (ServiceAvailabilityPeriod period : updatedPeriods) {
            mergeAdjacentPeriods(period.getService(), period.getStartDate(), period.getEndDate());
        }
    }

    /**
     * Объединяет смежные периоды с одинаковой мощностью
     */
    @Transactional
    public void mergeAdjacentPeriods(org.dev.powermarket.domain.Service service, LocalDate startDate, LocalDate endDate) {
        boolean merged;
        do {
            merged = false;

            // Попробовать объединить с предыдущим периодом
            Optional<ServiceAvailabilityPeriod> previousPeriod = periodRepository
                    .findByServiceAndEndDate(service, startDate.minusDays(1));

            if (previousPeriod.isPresent() && canMergePeriods(previousPeriod.get(), startDate, endDate)) {
                ServiceAvailabilityPeriod mergedPeriod = mergeTwoPeriods(previousPeriod.get(), startDate, endDate);
                startDate = mergedPeriod.getStartDate();
                endDate = mergedPeriod.getEndDate();
                merged = true;
            }

            // Попробовать объединить со следующим периодом
            Optional<ServiceAvailabilityPeriod> nextPeriod = periodRepository
                    .findByServiceAndStartDate(service, endDate.plusDays(1));

            if (nextPeriod.isPresent() && canMergePeriods(nextPeriod.get(), startDate, endDate)) {
                ServiceAvailabilityPeriod mergedPeriod = mergeTwoPeriods(nextPeriod.get(), startDate, endDate);
                startDate = mergedPeriod.getStartDate();
                endDate = mergedPeriod.getEndDate();
                merged = true;
            }

        } while (merged); // Продолжаем пока есть что объединять
    }

    /**
     * Проверяет, можно ли объединить периоды
     */
    private boolean canMergePeriods(ServiceAvailabilityPeriod adjacentPeriod,
                                    LocalDate currentStart, LocalDate currentEnd) {
        // Периоды можно объединить если:
        // 1. Они смежные (даты стыкуются)
        // 2. Имеют одинаковую общую мощность
        // 3. Имеют одинаковую доступную мощность

        boolean datesAdjacent =
                adjacentPeriod.getEndDate().plusDays(1).equals(currentStart) ||
                        adjacentPeriod.getStartDate().minusDays(1).equals(currentEnd);

        boolean sameCapacity =
                adjacentPeriod.getTotalCapacity().compareTo(adjacentPeriod.getTotalCapacity()) == 0 &&
                        adjacentPeriod.getAvailableCapacity().compareTo(adjacentPeriod.getAvailableCapacity()) == 0;

        return datesAdjacent && sameCapacity;
    }

    /**
     * Объединяет два периода в один
     */
    private ServiceAvailabilityPeriod mergeTwoPeriods(ServiceAvailabilityPeriod period1,
                                                      LocalDate startDate2, LocalDate endDate2) {
        LocalDate mergedStart = period1.getStartDate().isBefore(startDate2) ?
                period1.getStartDate() : startDate2;
        LocalDate mergedEnd = period1.getEndDate().isAfter(endDate2) ?
                period1.getEndDate() : endDate2;

        // Создаем новый объединенный период
        ServiceAvailabilityPeriod mergedPeriod = new ServiceAvailabilityPeriod();
        mergedPeriod.setService(period1.getService());
        mergedPeriod.setStartDate(mergedStart);
        mergedPeriod.setEndDate(mergedEnd);
        mergedPeriod.setTotalCapacity(period1.getTotalCapacity());
        mergedPeriod.setAvailableCapacity(period1.getAvailableCapacity());

        // Сохраняем новый период
        ServiceAvailabilityPeriod savedMerged = periodRepository.save(mergedPeriod);

        // Удаляем старые периоды
        periodRepository.delete(period1);

        // Найти и удалить второй период
        periodRepository.findByServiceAndStartDateAndEndDate(
                        period1.getService(), startDate2, endDate2)
                .ifPresent(periodRepository::delete);

        return savedMerged;
    }

    /**
     * Получить доступную мощность на конкретную дату
     */
    private BigDecimal getAvailableCapacityForDate(org.dev.powermarket.domain.Service service, LocalDate date) {
        List<ServiceAvailabilityPeriod> periods = periodRepository.findOverlappingPeriods(service, date, date);
        return periods.stream()
                .map(ServiceAvailabilityPeriod::getAvailableCapacity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Найти или разделить период для конкретной даты
     */
    private ServiceAvailabilityPeriod findOrSplitPeriodForDate(org.dev.powermarket.domain.Service  service, LocalDate date, BigDecimal requiredCapacity) {
        List<ServiceAvailabilityPeriod> periods = periodRepository.findAvailablePeriodsWithCapacity(
                service, date, date, requiredCapacity);

        if (periods.isEmpty()) {
            throw new IllegalArgumentException("No available period found for date: " + date);
        }

        // Берем первый подходящий период
        ServiceAvailabilityPeriod period = periods.getFirst();

        // Если период охватывает больше чем одну дату, разделяем его
        if (!period.getStartDate().equals(period.getEndDate())) {
            return splitPeriodForDate(period, date);
        }

        return period;
    }

    /**
     * Разделить период на указанную дату
     */
    private ServiceAvailabilityPeriod splitPeriodForDate(ServiceAvailabilityPeriod original, LocalDate splitDate) {
        // Создаем период ДО splitDate
        if (original.getStartDate().isBefore(splitDate)) {
            ServiceAvailabilityPeriod before = new ServiceAvailabilityPeriod();
            before.setService(original.getService());
            before.setStartDate(original.getStartDate());
            before.setEndDate(splitDate.minusDays(1));
            before.setTotalCapacity(original.getTotalCapacity());
            before.setAvailableCapacity(original.getAvailableCapacity());
            periodRepository.save(before);
        }

        // Создаем период ПОСЛЕ splitDate
        if (original.getEndDate().isAfter(splitDate)) {
            ServiceAvailabilityPeriod after = new ServiceAvailabilityPeriod();
            after.setService(original.getService());
            after.setStartDate(splitDate.plusDays(1));
            after.setEndDate(original.getEndDate());
            after.setTotalCapacity(original.getTotalCapacity());
            after.setAvailableCapacity(original.getAvailableCapacity());
            periodRepository.save(after);
        }

        // Модифицируем оригинальный период для splitDate
        original.setStartDate(splitDate);
        original.setEndDate(splitDate);
        return periodRepository.save(original);
    }
}
