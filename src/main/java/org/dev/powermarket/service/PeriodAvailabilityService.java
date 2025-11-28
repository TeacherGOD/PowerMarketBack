package org.dev.powermarket.service;

import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.CapacityReservation;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.ServiceAvailabilityPeriod;
import org.dev.powermarket.domain.dto.request.AvailablePeriodSearchRequest;
import org.dev.powermarket.domain.dto.response.AvailablePeriodResponse;
import org.dev.powermarket.domain.dto.response.ServiceAvailabilityResponse;
import org.dev.powermarket.domain.enums.AvailabilityStatus;
import org.dev.powermarket.repository.CapacityReservationRepository;
import org.dev.powermarket.repository.ServiceAvailabilityPeriodRepository;
import org.dev.powermarket.repository.ServiceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PeriodAvailabilityService {

    private final ServiceRepository serviceRepository;
    private final ServiceAvailabilityPeriodRepository periodRepository;
    private final CapacityReservationRepository reservationRepository;
    private final CapacityManagementService capacityManagementService;

    /**
     * Найти доступные периоды для сервиса с учетом требуемой мощности
     */
    public List<AvailablePeriodResponse> findAvailablePeriods(
            UUID serviceId, AvailablePeriodSearchRequest request) {

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        List<AvailablePeriodResponse> availablePeriods = new ArrayList<>();

        // Находим все периоды доступности сервиса в запрошенном диапазоне
        List<ServiceAvailabilityPeriod> availabilityPeriods = periodRepository.findPeriodsInRange(
                service, request.startDate(), request.endDate());

        for (ServiceAvailabilityPeriod period : availabilityPeriods) {
            // Для каждого периода доступности находим доступные подпериоды
            List<AvailablePeriodResponse> subPeriods = findAvailableSubPeriods(
                    service, period, request.requiredCapacity());
            availablePeriods.addAll(subPeriods);
        }

        return availablePeriods;
    }

    /**
     * Найти доступные подпериоды в рамках одного периода доступности
     * НОВЫЙ АЛГОРИТМ: учитывает пересекающиеся бронирования и находит все доступные промежутки
     */
    private List<AvailablePeriodResponse> findAvailableSubPeriods(
            Service service, ServiceAvailabilityPeriod period, BigDecimal requiredCapacity) {

        List<AvailablePeriodResponse> availablePeriods = new ArrayList<>();

        // Находим все бронирования, которые пересекаются с этим периодом
        List<CapacityReservation> overlappingReservations = reservationRepository
                .findOverlappingReservations(service, period.getStartDate(), period.getEndDate());

        // Если нет бронирований - весь период доступен
        if (overlappingReservations.isEmpty()) {
            if (isPeriodFullyAvailable(service, period.getStartDate(), period.getEndDate(), requiredCapacity)) {
                BigDecimal minAvailableCapacity = findMinAvailableCapacityInPeriod(
                        service, period.getStartDate(), period.getEndDate());
                availablePeriods.add(createAvailablePeriodResponse(
                        service, period.getStartDate(), period.getEndDate(), requiredCapacity, minAvailableCapacity));
            }
            return availablePeriods;
        }

        // Создаем список всех значимых дат (начала и концы периодов и бронирований)
        TreeSet<LocalDate> significantDates = new TreeSet<>();
        significantDates.add(period.getStartDate());
        significantDates.add(period.getEndDate().plusDays(1)); // +1 чтобы включить последний день

        for (CapacityReservation reservation : overlappingReservations) {
            significantDates.add(reservation.getStartDate());
            significantDates.add(reservation.getEndDate().plusDays(1)); // +1 чтобы включить последний день
        }

        // Преобразуем в отсортированный список
        List<LocalDate> dates = new ArrayList<>(significantDates);
        Collections.sort(dates);

        // Проверяем каждый промежуток между значимыми датами
        for (int i = 0; i < dates.size() - 1; i++) {
            LocalDate intervalStart = dates.get(i);
            LocalDate intervalEnd = dates.get(i + 1).minusDays(1); // -1 чтобы вернуть к нормальным датам

            // Убедимся, что промежуток не выходит за границы периода
            if (intervalStart.isBefore(period.getStartDate())) {
                intervalStart = period.getStartDate();
            }
            if (intervalEnd.isAfter(period.getEndDate())) {
                intervalEnd = period.getEndDate();
            }

            // Пропускаем некорректные промежутки
            if (intervalStart.isAfter(intervalEnd)) {
                continue;
            }

            // Проверяем доступность этого промежутка
            if (isPeriodFullyAvailable(service, intervalStart, intervalEnd, requiredCapacity)) {
                BigDecimal minAvailableCapacity = findMinAvailableCapacityInPeriod(
                        service, intervalStart, intervalEnd);
                availablePeriods.add(createAvailablePeriodResponse(
                        service, intervalStart, intervalEnd, requiredCapacity, minAvailableCapacity));
            }
        }

        return availablePeriods;
    }
    /**
     * Проверить, что в периоде достаточно доступной мощности на КАЖДЫЙ день
     */
    private boolean isPeriodFullyAvailable(Service service, LocalDate startDate,
                                           LocalDate endDate, BigDecimal requiredCapacity) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            BigDecimal availableCapacity = capacityManagementService.getAvailableCapacityForDate(
                    service.getId(), current);
            if (availableCapacity.compareTo(requiredCapacity) < 0) {
                return false;
            }
            current = current.plusDays(1);
        }
        return true;
    }

    private AvailablePeriodResponse createAvailablePeriodResponse(Service service,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  BigDecimal requiredCapacity,
                                                                  BigDecimal minAvailableCapacity) {
        boolean isFullyAvailable = minAvailableCapacity.compareTo(service.getMaxCapacity()) == 0;

        return new AvailablePeriodResponse(
                service.getId(),
                service.getTitle(),
                startDate,
                endDate,
                minAvailableCapacity,
                requiredCapacity,
                isFullyAvailable
        );
    }

    /**
     * Найти минимальную доступную мощность в периоде
     */
    private BigDecimal findMinAvailableCapacityInPeriod(Service service, LocalDate startDate,
                                                        LocalDate endDate) {
        BigDecimal minAvailableCapacity = service.getMaxCapacity();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            BigDecimal availableCapacity = capacityManagementService.getAvailableCapacityForDate(
                    service.getId(), current);
            if (availableCapacity.compareTo(minAvailableCapacity) < 0) {
                minAvailableCapacity = availableCapacity;
            }
            current = current.plusDays(1);
        }

        return minAvailableCapacity;
    }

    /**
     * Получить детальную информацию о доступности сервиса
     */
    public ServiceAvailabilityResponse getServiceAvailability(UUID serviceId,
                                                              LocalDate startDate,
                                                              LocalDate endDate) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        // Находим периоды доступности
        List<ServiceAvailabilityPeriod> periods = periodRepository.findPeriodsInRange(
                service, startDate, endDate);

        // Находим бронирования
        List<CapacityReservation> reservations = reservationRepository.findOverlappingReservations(
                service, startDate, endDate);

        List<ServiceAvailabilityResponse.AvailablePeriod> availablePeriods = new ArrayList<>();
        List<ServiceAvailabilityResponse.ReservedPeriod> reservedPeriods = reservations.stream()
                .map(reservation -> new ServiceAvailabilityResponse.ReservedPeriod(
                        reservation.getStartDate(),
                        reservation.getEndDate(),
                        reservation.getReservedCapacity(),
                        reservation.getRental().getTenant().getFullName()
                ))
                .toList();

        // Для каждого периода доступности находим доступные подпериоды
        for (ServiceAvailabilityPeriod period : periods) {
            List<AvailablePeriodResponse> availableSubPeriods = findAvailableSubPeriods(
                    service, period, BigDecimal.ONE); // Минимальная мощность для отображения

            for (AvailablePeriodResponse availablePeriod : availableSubPeriods) {
                availablePeriods.add(new ServiceAvailabilityResponse.AvailablePeriod(
                        availablePeriod.periodStart(),
                        availablePeriod.periodEnd(),
                        availablePeriod.availableCapacity()
                ));
            }
        }

        // Определяем общий статус доступности
        AvailabilityStatus status = determineOverallAvailability(availablePeriods, service.getMaxCapacity());

        return new ServiceAvailabilityResponse(
                serviceId,
                service.getTitle(),
                status,
                service.getMaxCapacity(),
                availablePeriods,
                reservedPeriods
        );
    }
    private AvailabilityStatus determineOverallAvailability(
            List<ServiceAvailabilityResponse.AvailablePeriod> availablePeriods,
            BigDecimal maxCapacity) {

        if (availablePeriods.isEmpty()) {
            return AvailabilityStatus.UNAVAILABLE;
        }

        // Проверяем, есть ли полностью доступные периоды
        boolean hasFullAvailability = availablePeriods.stream()
                .anyMatch(period -> period.availableCapacity().compareTo(maxCapacity) == 0);

        if (hasFullAvailability) {
            return AvailabilityStatus.AVAILABLE;
        }

        return AvailabilityStatus.PARTIAL;
    }
}
