package org.dev.powermarket.service;

import lombok.RequiredArgsConstructor;
import org.dev.powermarket.domain.*;
import org.dev.powermarket.domain.enums.NotificationType;
import org.dev.powermarket.domain.enums.RentalRequestStatus;
import org.dev.powermarket.domain.enums.Role;
import org.dev.powermarket.repository.*;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.dev.powermarket.service.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalRequestService {

    private final RentalRequestRepository rentalRequestRepository;
    private final ServiceRepository serviceRepository;
    private final AuthorizedUserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final RentalService rentalService;
    private final RentalRepository rentalRepository;
    private final ChatRepository chatRepository;
    private final CapacityManagementService capacityManagementService;

    @Transactional
    public RentalRequestDto createRentalRequest(String email, CreateRentalRequestRequest request) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (tenant.getRole() != Role.TENANT) {
            throw new AccessDeniedException("Only tenants can create rental requests");
        }

        org.dev.powermarket.domain.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        if (!service.getIsActive()) {
            throw new IllegalArgumentException("Service is not active");
        }

        boolean isAvailable = capacityManagementService.isCapacityAvailable(
                service.getId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCapacityNeeded()
        );

        if (!isAvailable) {
            throw new IllegalArgumentException("Not enough capacity available for selected dates");
        }

        // Calculate total price
        long days = request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay() + 1;
        BigDecimal totalPrice = service.getPricePerDay().multiply(BigDecimal.valueOf(days));

        RentalRequest rentalRequest = new RentalRequest();
        rentalRequest.setService(service);
        rentalRequest.setTenant(tenant);
        rentalRequest.setStartDate(request.getStartDate());
        rentalRequest.setEndDate(request.getEndDate());
        rentalRequest.setTotalPrice(totalPrice);
        rentalRequest.setMessage("");
        rentalRequest.setCapacityNeeded(request.getCapacityNeeded());
        rentalRequest.setStatus(RentalRequestStatus.PENDING);

        RentalRequest saved = rentalRequestRepository.save(rentalRequest);

        // Create rental and chat (without capacity reservation - that happens on confirmation)
        Rental rental = new Rental();
        rental.setRentalRequest(saved);
        rental.setService(service);
        rental.setSupplier(service.getSupplier());
        rental.setTenant(tenant);
        rental.setStartDate(request.getStartDate());
        rental.setEndDate(request.getEndDate());
        rental.setTotalPrice(totalPrice);
        rental.setCapacityNeeded(request.getCapacityNeeded());
        rental.setSupplierConfirmed(false);
        rental.setTenantConfirmed(false);
        rental.setIsActive(true);

        Rental savedRental = rentalRepository.save(rental);
        saved.setRental(savedRental);

        Chat chat = new Chat();
        chat.setRental(savedRental);
        Chat savedChat = chatRepository.save(chat);

        savedRental.setChat(savedChat);
        rentalRepository.save(savedRental);

        // Create notification for supplier
        createNotification(
                service.getSupplier(),
                NotificationType.NEW_RENTAL_REQUEST,
                "Новый запрос на аренду",
                String.format("Пользователь %s запросил аренду услуги '%s' с %s по %s",
                        tenant.getFullName(), service.getTitle(),
                        request.getStartDate(), request.getEndDate()),
                saved.getId()
        );

        return toDto(saved);
    }

    @Transactional
    public RentalRequestDto respondToRequest(String email, UUID requestId,
                                             RespondToRentalRequestRequest request) {
        RentalRequest rentalRequest = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Rental request not found"));

        User supplier = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!rentalRequest.getService().getSupplier().getId().equals(supplier.getId())) {
            throw new AccessDeniedException("You can only respond to requests for your services");
        }

        if (rentalRequest.getStatus() != RentalRequestStatus.PENDING) {
            throw new IllegalArgumentException("Request has already been responded to");
        }

        if (request.getApproved()) {
            rentalRequest.setStatus(RentalRequestStatus.IN_CONTRACT);
            rentalRequest.setRespondedAt(Instant.now());

            // Create rental and chat
            rentalService.createRentalFromRequest(rentalRequest);

            // Create notification for tenant
            createNotification(
                    rentalRequest.getTenant(),
                    NotificationType.REQUEST_APPROVED,
                    "Запрос одобрен",
                    String.format("Ваш запрос на аренду услуги '%s' был одобрен",
                            rentalRequest.getService().getTitle()),
                    rentalRequest.getId()
            );
        } else {
            rentalRequest.setStatus(RentalRequestStatus.REJECTED);
            rentalRequest.setRejectionReason(request.getRejectionReason());
            rentalRequest.setRespondedAt(Instant.now());

            // Create notification for tenant
            createNotification(
                    rentalRequest.getTenant(),
                    NotificationType.REQUEST_REJECTED,
                    "Запрос отклонен",
                    String.format("Ваш запрос на аренду услуги '%s' был отклонен",
                            rentalRequest.getService().getTitle()),
                    rentalRequest.getId()
            );
        }

        RentalRequest updated = rentalRequestRepository.save(rentalRequest);
        return toDto(updated);
    }

    @Transactional(readOnly = true)
    public List<RentalRequestDto> getMyRequests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.TENANT) {
            return rentalRequestRepository.findByTenant(user, Pageable.unpaged())
                    .stream()
                    .map(this::toDto)
                    .toList();
        } else if (user.getRole() == Role.SUPPLIER) {
            return rentalRequestRepository.findBySupplier(user, Pageable.unpaged())
                    .stream()
                    .map(this::toDto)
                    .toList();
        }

        return List.of();
    }

    @Transactional(readOnly = true)
    public List<RentalRequestDto> getReceivedRequests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return rentalRequestRepository.findBySupplier(user, Pageable.unpaged())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalRequestDto> getSentRequests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return rentalRequestRepository.findByTenant(user, Pageable.unpaged())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RentalRequestDto getRequest(String email, UUID requestId) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Rental request not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = request.getTenant().getId().equals(user.getId()) ||
                request.getService().getSupplier().getId().equals(user.getId());

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have access to this request");
        }

        return toDto(request);
    }

    private void createNotification(User user, NotificationType type, String title,
                                    String message, UUID relatedEntityId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private RentalRequestDto toDto(RentalRequest request) {
        RentalRequestDto dto = new RentalRequestDto();
        dto.setId(request.getId());
        dto.setServiceId(request.getService().getId());
        dto.setServiceTitle(request.getService().getTitle());
        dto.setTenantId(request.getTenant().getId());
        dto.setTenantName(request.getTenant().getFullName());
        dto.setTenantInn(request.getTenant().getInn());
        dto.setTenantEmail(request.getTenant().getEmail());
        dto.setTenantPhone(request.getTenant().getPhone());
        dto.setStartDate(request.getStartDate());
        dto.setEndDate(request.getEndDate());
        dto.setTotalPrice(request.getTotalPrice());
        dto.setMessage(request.getMessage());
        dto.setStatus(request.getStatus());
        dto.setRejectionReason(request.getRejectionReason());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setRespondedAt(request.getRespondedAt());
        dto.setCapacityNeeded(request.getCapacityNeeded());
        if (request.getRental() != null) {
            dto.setRentalId(request.getRental().getId());
        }

        return dto;
    }
}
