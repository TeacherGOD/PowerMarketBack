package org.dev.powermarket.service;

import org.dev.powermarket.domain.dto.DashboardDto;
import org.dev.powermarket.domain.enums.DashboardOrderStatus;
import org.dev.powermarket.domain.enums.RentalRequestStatus;
import org.dev.powermarket.domain.enums.Role;
import org.dev.powermarket.repository.RentalRepository;
import org.dev.powermarket.repository.RentalRequestRepository;
import org.dev.powermarket.repository.projection.*;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private RentalRequestRepository rentalRequestRepository;

    @Mock
    private AuthorizedUserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User supplier;
    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        supplier = new User();
        supplier.setId(UUID.randomUUID());
        supplier.setEmail("supplier@test.com");
        supplier.setRole(Role.SUPPLIER);
        supplier.setFullName("Test Supplier");

        from = LocalDate.of(2024, 1, 1);
        to = LocalDate.of(2024, 1, 31);
    }

    @Test
    void getDashboard_WhenSupplier_ReturnsDashboardData() {
        // Given
        when(userRepository.findByEmail(supplier.getEmail()))
                .thenReturn(Optional.of(supplier));

        // Mock summary data
        when(rentalRepository.countActiveRentalsBySupplierAndPeriod(supplier, from, to))
                .thenReturn(5L);

        RentalSummaryProjection summaryProjection = new RentalSummaryProjection() {
            @Override
            public Long getCount() { return 10L; }
            @Override
            public BigDecimal getTotalAmount() { return new BigDecimal("50000.00"); }
        };
        when(rentalRepository.countCompletedRentalsAndTotalAmountBySupplierAndPeriod(supplier, from, to))
                .thenReturn(summaryProjection);

        when(rentalRequestRepository.countPendingRequestsBySupplierAndPeriod(
                eq(supplier), any(), any()))
                .thenReturn(3L);

        // Mock status chart data
        StatusCountProjection pendingStatus = createStatusProjection(RentalRequestStatus.PENDING, 3L);
        StatusCountProjection completedStatus = createStatusProjection(RentalRequestStatus.COMPLETED, 10L);
        StatusCountProjection inRentStatus = createStatusProjection(RentalRequestStatus.IN_RENT, 2L);

        when(rentalRequestRepository.getStatusCountsBySupplierAndPeriod(
                eq(supplier), any(), any()))
                .thenReturn(List.of(pendingStatus, completedStatus, inRentStatus));

        // Mock income chart data
        IncomeChartProjection incomeProjection = new IncomeChartProjection() {
            @Override
            public Double getYear() { return 2024.0; }
            @Override
            public Double getMonth() { return 1.0; }
            @Override
            public BigDecimal getTotalAmount() { return new BigDecimal("50000.00"); }
            @Override
            public Long getCompletedOrders() { return 10L; }
        };
        when(rentalRepository.getIncomeChartDataBySupplierAndPeriod(supplier, from, to))
                .thenReturn(List.of(incomeProjection));

        // When
        DashboardDto result = dashboardService.getDashboard(supplier.getEmail(), from, to);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.role()).isEqualTo("SUPPLIER");
        assertThat(result.period().from()).isEqualTo(from);
        assertThat(result.period().to()).isEqualTo(to);

        // Verify summary
        DashboardDto.Summary summary = result.summary();
        assertThat(summary.activeOrders()).isEqualTo(5L);
        assertThat(summary.completedOrders()).isEqualTo(10L);
        assertThat(summary.totalAmount()).isEqualByComparingTo("50000.00");
        assertThat(summary.pendingRequests()).isEqualTo(3L);

        // Verify status chart
        DashboardDto.StatusesChart statusesChart = result.charts().statusesChart();
        assertThat(statusesChart.totalOrders()).isEqualTo(15L); // 3 + 10 + 2

        // Verify status items mapping
        assertThat(statusesChart.items())
                .anyMatch(item -> item.status().equals(DashboardOrderStatus.AWAITING_ACCEPTANCE.name()) && item.count() == 3L)
                .anyMatch(item -> item.status().equals(DashboardOrderStatus.COMPLETED.name()) && item.count() == 10L)
                .anyMatch(item -> item.status().equals(DashboardOrderStatus.IN_PROGRESS.name()) && item.count() == 2L);

        // Verify income chart
        DashboardDto.IncomeChart incomeChart = result.charts().incomeChart();
        assertThat(incomeChart.points()).hasSize(1);
        DashboardDto.IncomePoint incomePoint = incomeChart.points().get(0);
        assertThat(incomePoint.totalAmount()).isEqualByComparingTo("50000.00");
        assertThat(incomePoint.completedOrders()).isEqualTo(10L);
        assertThat(incomePoint.periodLabel()).isEqualTo("янв. 2024");
    }

    @Test
    void getDashboard_WhenDefaultPeriod_UsesLast30Days() {
        // Given
        when(userRepository.findByEmail(supplier.getEmail()))
                .thenReturn(Optional.of(supplier));

        // Mock empty data for default period
        when(rentalRepository.countActiveRentalsBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(0L);

        RentalSummaryProjection emptySummary = new RentalSummaryProjection() {
            @Override
            public Long getCount() { return 0L; }
            @Override
            public BigDecimal getTotalAmount() { return BigDecimal.ZERO; }
        };
        when(rentalRepository.countCompletedRentalsAndTotalAmountBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(emptySummary);

        when(rentalRequestRepository.countPendingRequestsBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(0L);
        when(rentalRequestRepository.getStatusCountsBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(List.of());
        when(rentalRepository.getIncomeChartDataBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(List.of());

        // When
        DashboardDto result = dashboardService.getDashboard(supplier.getEmail(), null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.period().from()).isEqualTo(LocalDate.now().minusDays(30));
        assertThat(result.period().to()).isEqualTo(LocalDate.now());
    }

    @Test
    void getDashboard_WhenNonSupplier_ReturnsEmptyDashboard() {
        // Given
        User tenant = new User();
        tenant.setId(UUID.randomUUID());
        tenant.setEmail("tenant@test.com");
        tenant.setRole(Role.TENANT);

        when(userRepository.findByEmail(tenant.getEmail()))
                .thenReturn(Optional.of(tenant));

        // When
        DashboardDto result = dashboardService.getDashboard(tenant.getEmail(), from, to);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.role()).isEqualTo("TENANT");
        assertThat(result.summary().activeOrders()).isZero();
        assertThat(result.summary().totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.charts().statusesChart().items()).isEmpty();
        assertThat(result.charts().incomeChart().points()).isEmpty();
    }

    @Test
    void getDashboard_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByEmail("nonexistent@test.com"))
                .thenReturn(Optional.empty());

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> dashboardService.getDashboard("nonexistent@test.com", from, to)
        );
    }

    @Test
    void getDashboard_WhenStatusMapping_CorrectlyMapsAllStatuses() {
        // Given
        when(userRepository.findByEmail(supplier.getEmail()))
                .thenReturn(Optional.of(supplier));

        // Mock all possible statuses
        List<StatusCountProjection> allStatuses = List.of(
                createStatusProjection(RentalRequestStatus.PENDING, 1L),
                createStatusProjection(RentalRequestStatus.IN_CONTRACT, 2L),
                createStatusProjection(RentalRequestStatus.CONFIRMED, 3L),
                createStatusProjection(RentalRequestStatus.IN_RENT, 4L),
                createStatusProjection(RentalRequestStatus.COMPLETED, 5L),
                createStatusProjection(RentalRequestStatus.REJECTED, 6L),
                createStatusProjection(RentalRequestStatus.CANCELLED, 7L)
        );

        when(rentalRequestRepository.getStatusCountsBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(allStatuses);

        // Mock other methods to return minimal data
        when(rentalRepository.countActiveRentalsBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(0L);
        when(rentalRepository.countCompletedRentalsAndTotalAmountBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(null);
        when(rentalRequestRepository.countPendingRequestsBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(0L);
        when(rentalRepository.getIncomeChartDataBySupplierAndPeriod(any(), any(), any()))
                .thenReturn(List.of());

        // When
        DashboardDto result = dashboardService.getDashboard(supplier.getEmail(), from, to);

        // Then
        DashboardDto.StatusesChart statusesChart = result.charts().statusesChart();

        // Verify status mapping
        assertThat(statusesChart.items())
                .anyMatch(item -> item.status().equals("AWAITING_ACCEPTANCE") && item.count() == 1L) // PENDING
                .anyMatch(item -> item.status().equals("ACCEPTED") && item.count() == 2L) // IN_CONTRACT
                .anyMatch(item -> item.status().equals("IN_PROGRESS") && item.count() == 7L) // CONFIRMED(3) + IN_RENT(4)
                .anyMatch(item -> item.status().equals("COMPLETED") && item.count() == 5L)
                .anyMatch(item -> item.status().equals("CANCELLED") && item.count() == 13L); // REJECTED(6) + CANCELLED(7)
    }

    private StatusCountProjection createStatusProjection(RentalRequestStatus status, Long count) {
        return new StatusCountProjection() {
            @Override
            public RentalRequestStatus getStatus() { return status; }
            @Override
            public Long getCount() { return count; }
        };
    }
}