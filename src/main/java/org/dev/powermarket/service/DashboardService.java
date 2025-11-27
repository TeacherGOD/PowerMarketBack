package org.dev.powermarket.service;

import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.dto.DashboardDto;
import org.dev.powermarket.domain.enums.DashboardOrderStatus;
import org.dev.powermarket.domain.enums.RentalRequestStatus;
import org.dev.powermarket.repository.RentalRepository;
import org.dev.powermarket.repository.RentalRequestRepository;
import org.dev.powermarket.repository.projection.IncomeChartProjection;
import org.dev.powermarket.repository.projection.RentalSummaryProjection;
import org.dev.powermarket.repository.projection.StatusCountProjection;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RentalRepository rentalRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final AuthorizedUserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardDto getDashboard(String email, LocalDate from, LocalDate to) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Установка периода по умолчанию
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }

        if (user.getRole().name().equals("SUPPLIER")) {
            return getSupplierDashboard(user, from, to);
        }

        // Для других ролей можно добавить логику
        return new DashboardDto(
                user.getRole().name(),
                new DashboardDto.Period(from, to),
                new DashboardDto.Summary(0, 0, BigDecimal.ZERO, 0),
                new DashboardDto.Charts(
                        new DashboardDto.StatusesChart(0, List.of()),
                        new DashboardDto.IncomeChart("RUB", "month", List.of())
                )
        );
    }

    private DashboardDto getSupplierDashboard(User supplier, LocalDate from, LocalDate to) {
        DashboardDto.Period period = new DashboardDto.Period(from, to);
        DashboardDto.Summary summary = getSupplierSummary(supplier, from, to);
        DashboardDto.Charts charts = getSupplierCharts(supplier, from, to);

        return new DashboardDto(
                "SUPPLIER",
                period,
                summary,
                charts
        );
    }

    private DashboardDto.Summary getSupplierSummary(User supplier, LocalDate from, LocalDate to) {
        // Активные заказы - пересекающиеся с периодом
        long activeOrders = rentalRepository.countActiveRentalsBySupplierAndPeriod(supplier, from, to);

        // Завершенные заказы и общая сумма через Projection
        RentalSummaryProjection completedData = rentalRepository.countCompletedRentalsAndTotalAmountBySupplierAndPeriod(supplier, from, to);

        long completedOrders = completedData != null ? completedData.getCount() : 0;
        BigDecimal totalAmount = completedData != null ? completedData.getTotalAmount() : BigDecimal.ZERO;

        // Ожидающие запросы - конвертируем LocalDate в Instant для запроса
        java.time.Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        java.time.Instant toInstant = to.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        long pendingRequests = rentalRequestRepository.countPendingRequestsBySupplierAndPeriod(supplier, fromInstant, toInstant);

        return new DashboardDto.Summary(activeOrders, completedOrders, totalAmount, pendingRequests);
    }

    private DashboardDto.Charts getSupplierCharts(User supplier, LocalDate from, LocalDate to) {
        return new DashboardDto.Charts(
                getStatusesChart(supplier, from, to),
                getIncomeChart(supplier, from, to)
        );
    }

    private DashboardDto.StatusesChart getStatusesChart(User supplier, LocalDate from, LocalDate to) {
        // Конвертируем LocalDate в Instant для запроса
        java.time.Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        java.time.Instant toInstant = to.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        List<StatusCountProjection> statusCounts = rentalRequestRepository.getStatusCountsBySupplierAndPeriod(supplier, fromInstant, toInstant);

        Map<DashboardOrderStatus, Long> dashboardStatusCounts = new EnumMap<>(DashboardOrderStatus.class);
        long totalOrders = 0;

        for (StatusCountProjection projection : statusCounts) {
            RentalRequestStatus status = projection.getStatus();
            Long count = projection.getCount();

            totalOrders += count;

            DashboardOrderStatus dashboardStatus = mapToDashboardStatus(status);
            dashboardStatusCounts.merge(dashboardStatus, count, Long::sum);
        }

        List<DashboardDto.StatusItem> items = Arrays.stream(DashboardOrderStatus.values())
                .map(status -> new DashboardDto.StatusItem(
                        status.name(),
                        status.getLabel(),
                        dashboardStatusCounts.getOrDefault(status, 0L)
                ))
                .toList();

        return new DashboardDto.StatusesChart(totalOrders, items);
    }

    private DashboardDto.IncomeChart getIncomeChart(User supplier, LocalDate from, LocalDate to) {
        List<IncomeChartProjection> incomeData = rentalRepository.getIncomeChartDataBySupplierAndPeriod(supplier, from, to);

        List<DashboardDto.IncomePoint> points = incomeData.stream()
                .map(projection -> {

                    int year = projection.getYear().intValue();
                    int month = projection.getMonth().intValue();

                    YearMonth yearMonth = YearMonth.of(year, month);
                    String periodLabel = yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.of("ru")));

                    return new DashboardDto.IncomePoint(
                            periodLabel,
                            yearMonth.atDay(1),
                            projection.getTotalAmount(),
                            projection.getCompletedOrders()
                    );
                })
                .sorted(Comparator.comparing(DashboardDto.IncomePoint::periodStart))
                .toList();

        return new DashboardDto.IncomeChart("RUB", "month", points);
    }

    private DashboardOrderStatus mapToDashboardStatus(RentalRequestStatus rentalStatus) {
        return switch (rentalStatus) {
            case PENDING -> DashboardOrderStatus.AWAITING_ACCEPTANCE;
            case IN_CONTRACT -> DashboardOrderStatus.ACCEPTED;
            case CONFIRMED, IN_RENT -> DashboardOrderStatus.IN_PROGRESS;
            case COMPLETED -> DashboardOrderStatus.COMPLETED;
            case REJECTED, CANCELLED -> DashboardOrderStatus.CANCELLED;
        };
    }
}