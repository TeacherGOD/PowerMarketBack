
package org.dev.powermarket.service;


import org.dev.powermarket.domain.Rental;
import org.dev.powermarket.domain.RentalRequest;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.dto.DashboardDto;
import org.dev.powermarket.domain.enums.RentalRequestStatus;
import org.dev.powermarket.domain.enums.Role;
import org.dev.powermarket.domain.enums.ServiceCategory;
import org.dev.powermarket.repository.RentalRepository;
import org.dev.powermarket.repository.RentalRequestRepository;
import org.dev.powermarket.repository.ServiceRepository;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AuthorizedUserRepository userRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private RentalRequestRepository rentalRequestRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private User supplier;
    private User tenant;
    private Service service;
    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        // Cleanup before each test
        rentalRepository.deleteAll();
        rentalRequestRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        supplier = createUser("integration-supplier@test.com", Role.SUPPLIER, "Test Supplier");
        tenant = createUser("integration-tenant@test.com", Role.TENANT, "Test Tenant");

        // Create test service
        service = createService(supplier, "Test Service", new BigDecimal("1000.00"));

        from = LocalDate.now().minusDays(30);
        to = LocalDate.now().plusDays(30);
    }

    @Test
    void getDashboard_WithMixedData_ReturnsCorrectSummary() {
        // Given - создаем тестовые данные, которые точно попадают в период

        // PENDING запрос создан ВНУТРИ периода
        createRentalRequest(RentalRequestStatus.PENDING,  new BigDecimal("1000.00"));

        // COMPLETED аренда с endDate ВНУТРИ периода
        createCompletedRental( new BigDecimal("5000.00")); // endDate = 2024-01-05

        // ACTIVE аренда, которая ПЕРЕСЕКАЕТСЯ с периодом
        createActiveRental();


        // When
        DashboardDto result = dashboardService.getDashboard(supplier.getEmail(), from, to);

        // Then
        assertThat(result).isNotNull();

        DashboardDto.Summary summary = result.summary();
        assertThat(summary.pendingRequests()).isEqualTo(1L);
        assertThat(summary.completedOrders()).isEqualTo(1L);
        assertThat(summary.totalAmount()).isEqualByComparingTo("5000.00");
        assertThat(summary.activeOrders()).isEqualTo(1L);
    }

    @Test
    void getDashboard_WithNoData_ReturnsZeroValues() {
        // When
        DashboardDto result = dashboardService.getDashboard(supplier.getEmail(), from, to);

        // Then
        assertThat(result).isNotNull();

        DashboardDto.Summary summary = result.summary();
        assertThat(summary.pendingRequests()).isZero();
        assertThat(summary.completedOrders()).isZero();
        assertThat(summary.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.activeOrders()).isZero();
    }

    @Test
    void getDashboard_WithDataOutsidePeriod_ReturnsZeroValues() {
        createRentalRequest(RentalRequestStatus.PENDING, new BigDecimal("1000.00"));
        createCompletedRental( new BigDecimal("5000.00"));

        // When
        DashboardDto result = dashboardService.getDashboard(supplier.getEmail(), from.plusDays(60), to.plusDays(60));

        // Then
        assertThat(result).isNotNull();

        DashboardDto.Summary summary = result.summary();
        assertThat(summary.pendingRequests()).isZero(); // запрос создан вне периода
        assertThat(summary.completedOrders()).isZero(); // аренда завершена вне периода
        assertThat(summary.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private User createUser(String email, Role role, String fullName) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        user.setFullName(fullName);
        user.setPasswordHash("password");
        return userRepository.save(user);
    }

    private Service createService(User supplier, String title, BigDecimal pricePerDay) {
        Service service = new Service();
        service.setTitle(title);
        service.setSupplier(supplier);
        service.setIsActive(true);
        service.setPricePerDay(pricePerDay);

        // Заполняем обязательные поля для Service
        service.setCategory(ServiceCategory.MANUFACTURING);
        service.setCapacity("100"); // обязательное поле
        service.setAvailableCapacity("100");
        service.setLocation("Test Location");
        service.setDescription("Test Description");
        service.setTechnicalSpecs("Test Specs");
        service.setTotalCapacityUnits(2);

        // Устанавливаем даты
        java.time.Instant now = java.time.Instant.now();
        service.setCreatedAt(now);
        service.setUpdatedAt(now);

        return serviceRepository.save(service);
    }

    private void createRentalRequest(RentalRequestStatus status, BigDecimal totalPrice) {
        RentalRequest request = new RentalRequest();
        request.setService(service);
        request.setTenant(tenant);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(6));
        request.setTotalPrice(totalPrice);
        request.setCapacityNeeded(100);
        request.setStatus(status);

        if (status == RentalRequestStatus.REJECTED) {
            request.setRejectionReason("Test rejection");
        }

        rentalRequestRepository.save(request);
    }

    private void createCompletedRental(BigDecimal totalPrice) {
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now().minusDays(5);

        RentalRequest request = new RentalRequest();
        request.setService(service);
        request.setTenant(tenant);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setTotalPrice(totalPrice);
        request.setCapacityNeeded(100);
        request.setStatus(RentalRequestStatus.COMPLETED);

        RentalRequest savedRequest = rentalRequestRepository.save(request);

        Rental rental = new Rental();
        rental.setRentalRequest(savedRequest);
        rental.setService(service);
        rental.setSupplier(supplier);
        rental.setTenant(tenant);
        rental.setStartDate(startDate);
        rental.setEndDate(endDate);
        rental.setTotalPrice(totalPrice);
        rental.setSupplierConfirmed(true);
        rental.setTenantConfirmed(true);
        rental.setIsActive(false); // завершенная аренда не активна

        rentalRepository.save(rental);
    }

    private void createActiveRental() {
        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(3);

        RentalRequest request = new RentalRequest();
        request.setService(service);
        request.setTenant(tenant);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setTotalPrice(new BigDecimal("3000.00"));
        request.setCapacityNeeded(100);
        request.setStatus(RentalRequestStatus.IN_RENT);

        RentalRequest savedRequest = rentalRequestRepository.save(request);

        Rental rental = new Rental();
        rental.setRentalRequest(savedRequest);
        rental.setService(service);
        rental.setSupplier(supplier);
        rental.setTenant(tenant);
        rental.setStartDate(startDate);
        rental.setEndDate(endDate);
        rental.setTotalPrice(new BigDecimal("3000.00"));
        rental.setSupplierConfirmed(true);
        rental.setTenantConfirmed(true);
        rental.setIsActive(true);

        rentalRepository.save(rental);
    }
}