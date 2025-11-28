package org.dev.powermarket.service;

import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.dto.request.ServiceSearchByCategoryRequest;
import org.dev.powermarket.domain.dto.response.ServiceSearchResultResponse;
import org.dev.powermarket.domain.enums.AvailabilityStatus;
import org.dev.powermarket.domain.enums.ServiceCategory;
import org.dev.powermarket.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ServiceSearchService {

    private final ServiceRepository serviceRepository;
    private final CapacityManagementService capacityManagementService;
    private final PeriodAvailabilityService periodAvailabilityService;

    /**
     * Поиск сервисов по категории и минимальной мощности
     */
    public Page<ServiceSearchResultResponse> searchServicesByCategoryAndCapacity(
            ServiceSearchByCategoryRequest searchRequest) {

        Pageable pageable = PageRequest.of(searchRequest.page(), searchRequest.size());
        Page<Service> services;

        // Выбираем подходящий метод репозитория в зависимости от параметров
        if (searchRequest.availableFrom() != null && searchRequest.availableTo() != null) {
            // Поиск с учетом доступности в указанные даты
            services = serviceRepository.findAvailableServicesByCategoryAndCapacity(
                    searchRequest.category(),
                    searchRequest.minCapacity(),
                    searchRequest.availableFrom(),
                    searchRequest.availableTo(),
                    pageable
            );
        } else {
            // Базовый поиск по категории и мощности
            services = serviceRepository.findByCategoryAndMinCapacity(
                    searchRequest.category(),
                    searchRequest.minCapacity(),
                    pageable
            );
        }

        // Конвертируем в Response с информацией о доступности
        return services.map(service -> convertToSearchResultResponse(service, searchRequest));
    }

    /**
     * Конвертировать Service в ServiceSearchResultResponse с информацией о доступности
     */
    private ServiceSearchResultResponse convertToSearchResultResponse(
            Service service, ServiceSearchByCategoryRequest searchRequest) {

        // Получаем текущую доступную мощность
        BigDecimal currentAvailableCapacity = capacityManagementService.getAvailableCapacityForDate(
                service.getId(), LocalDate.now());

        // Определяем статус доступности
        AvailabilityStatus availabilityStatus = determineAvailabilityStatus(
                service, currentAvailableCapacity);

        // Проверяем доступность для запрошенных дат (если указаны)
        Boolean isAvailableForRequestedDates = null;
        if (searchRequest.availableFrom() != null && searchRequest.availableTo() != null) {
            isAvailableForRequestedDates = isServiceAvailableForPeriod(
                    service,
                    searchRequest.availableFrom(),
                    searchRequest.availableTo(),
                    searchRequest.minCapacity()
            );
        }

        return new ServiceSearchResultResponse(
                service.getId(),
                service.getTitle(),
                service.getDescription(),
                service.getCategory(),
                service.getPricePerDay(),
                service.getMaxCapacity(),
                service.getLocation(),
                service.getTechnicalSpecs(),
                service.getSupplier().getId(),
                service.getSupplier().getFullName(),
                service.getIsActive(),
                service.getAverageRating(),
                service.getTotalReviews(),
                service.getCreatedAt(),
                availabilityStatus,
                currentAvailableCapacity,
                isAvailableForRequestedDates
        );
    }

    /**
     * Определить статус доступности сервиса
     */
    private AvailabilityStatus determineAvailabilityStatus(Service service, BigDecimal currentAvailableCapacity) {
        if (currentAvailableCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return AvailabilityStatus.UNAVAILABLE;
        } else if (currentAvailableCapacity.compareTo(service.getMaxCapacity()) < 0) {
            return AvailabilityStatus.PARTIAL;
        } else {
            return AvailabilityStatus.AVAILABLE;
        }
    }

    /**
     * Проверить доступность сервиса на период
     */
    private Boolean isServiceAvailableForPeriod(Service service, LocalDate startDate,
                                                LocalDate endDate, BigDecimal requiredCapacity) {
        try {
            return capacityManagementService.isCapacityAvailable(
                    service.getId(), startDate, endDate, requiredCapacity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Получить сервисы с самой высокой доступной мощностью в категории
     */
    public List<ServiceSearchResultResponse> findTopServicesByCategoryAndCapacity(
            ServiceCategory category, BigDecimal minCapacity, int limit) {

        Page<Service> servicePage = serviceRepository.findByCategoryAndMinCapacity(
                category, minCapacity, PageRequest.of(0, Math.max(limit, 50))); // Берем больше для сортировки

        // ✅ ИСПРАВЛЕНИЕ: создаем новый изменяемый список
        List<Service> services = new ArrayList<>(servicePage.getContent());

        // ✅ ИСПРАВЛЕНИЕ: безопасная сортировка с обработкой исключений
        services.sort((s1, s2) -> {
            try {
                BigDecimal avail1 = capacityManagementService.getAvailableCapacityForDate(s1.getId(), LocalDate.now());
                BigDecimal avail2 = capacityManagementService.getAvailableCapacityForDate(s2.getId(), LocalDate.now());
                return avail2.compareTo(avail1); // По убыванию
            } catch (Exception e) {
                // В случае ошибки считаем доступную мощность = 0
                return BigDecimal.ZERO.compareTo(BigDecimal.ZERO);
            }
        });

        // Ограничиваем результат заданным лимитом
        if (services.size() > limit) {
            services = services.subList(0, limit);
        }

        ServiceSearchByCategoryRequest searchRequest = new ServiceSearchByCategoryRequest(
                category, minCapacity, null, null, 0, limit);

        return services.stream()
                .map(service -> convertToSearchResultResponse(service, searchRequest))
                .toList();
    }

    /**
     * Поиск сервисов по категории с группировкой по доступности
     */
    public Map<AvailabilityStatus, List<ServiceSearchResultResponse>> findServicesGroupedByAvailability(
            ServiceCategory category, BigDecimal minCapacity) {

        List<Service> services = serviceRepository.findByCategoryAndMinCapacity(
                category, minCapacity, Pageable.unpaged()).getContent();

        Map<AvailabilityStatus, List<ServiceSearchResultResponse>> groupedResults = new HashMap<>();

        for (Service service : services) {
            BigDecimal currentAvailableCapacity = capacityManagementService.getAvailableCapacityForDate(
                    service.getId(), LocalDate.now());
            AvailabilityStatus status = determineAvailabilityStatus(service, currentAvailableCapacity);

            ServiceSearchResultResponse response = convertToSearchResultResponse(service,
                    new ServiceSearchByCategoryRequest(category, minCapacity, null, null, 0, 100));

            groupedResults.computeIfAbsent(status, k -> new ArrayList<>()).add(response);
        }

        return groupedResults;
    }
}