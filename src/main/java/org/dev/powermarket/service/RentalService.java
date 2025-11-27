package org.dev.powermarket.service;

import org.dev.powermarket.domain.*;
import org.dev.powermarket.domain.enums.NotificationType;
import org.dev.powermarket.domain.enums.RentalRequestStatus;
import org.dev.powermarket.repository.*;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.dev.powermarket.service.dto.RentalDto;
import org.dev.powermarket.service.dto.RentalStatsDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RentalService {

    private final RentalRepository rentalRepository;
    private final ChatRepository chatRepository;
    private final ServiceAvailabilityRepository availabilityRepository;
    private final AuthorizedUserRepository userRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final NotificationRepository notificationRepository;

    public RentalService(RentalRepository rentalRepository,
                         ChatRepository chatRepository,
                         ServiceAvailabilityRepository availabilityRepository,
                         AuthorizedUserRepository userRepository,
                         RentalRequestRepository rentalRequestRepository,
                         NotificationRepository notificationRepository) {
        this.rentalRepository = rentalRepository;
        this.chatRepository = chatRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createRentalFromRequest(RentalRequest request) {
        // Create rental only if it doesn't already exist (created during request submission)
        Rental rental = rentalRepository.findByRentalRequest(request).orElse(null);

        if (rental == null) {
            rental = new Rental();
            rental.setRentalRequest(request);
            rental.setService(request.getService());
            rental.setSupplier(request.getService().getSupplier());
            rental.setTenant(request.getTenant());
            rental.setStartDate(request.getStartDate());
            rental.setEndDate(request.getEndDate());
            rental.setTotalPrice(request.getTotalPrice());
            rental.setSupplierConfirmed(false);
            rental.setTenantConfirmed(false);
            rental.setIsActive(true);

            Rental savedRental = rentalRepository.save(rental);

            // Create chat if doesn't exist
            Chat chat = chatRepository.findByRental(savedRental).orElse(null);
            if (chat == null) {
                chat = new Chat();
                chat.setRental(savedRental);
                Chat savedChat = chatRepository.save(chat);
                savedRental.setChat(savedChat);
                rentalRepository.save(savedRental);
            }
        }

        request.setStatus(RentalRequestStatus.IN_CONTRACT);
        rentalRequestRepository.save(request);
    }

    @Transactional
    public RentalDto confirmRental(String email, UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        RentalRequest request = rental.getRentalRequest();

        if (request.getStatus() != RentalRequestStatus.IN_CONTRACT &&
                request.getStatus() != RentalRequestStatus.CONFIRMED) {
            throw new IllegalArgumentException("Rental is not in contract status");
        }

        boolean isSupplier = rental.getSupplier().getId().equals(user.getId());
        boolean isTenant = rental.getTenant().getId().equals(user.getId());

        if (!isSupplier && !isTenant) {
            throw new AccessDeniedException("You are not part of this rental");
        }

        Instant now = Instant.now();

        if (isSupplier && !rental.getSupplierConfirmed()) {
            rental.setSupplierConfirmed(true);
            rental.setSupplierConfirmedAt(now);

            createNotification(
                    rental.getTenant(),
                    NotificationType.REQUEST_APPROVED,
                    "Арендодатель подтвердил аренду",
                    String.format("Арендодатель подтвердил аренду '%s'", rental.getService().getTitle()),
                    rental.getId()
            );
        } else if (isTenant && !rental.getTenantConfirmed()) {
            rental.setTenantConfirmed(true);
            rental.setTenantConfirmedAt(now);

            createNotification(
                    rental.getSupplier(),
                    NotificationType.REQUEST_APPROVED,
                    "Арендатор подтвердил аренду",
                    String.format("Арендатор подтвердил аренду '%s'", rental.getService().getTitle()),
                    rental.getId()
            );
        } else {
            throw new IllegalArgumentException("You have already confirmed this rental");
        }

        if (rental.getSupplierConfirmed() && rental.getTenantConfirmed()) {
            request.setStatus(RentalRequestStatus.CONFIRMED);
            rentalRequestRepository.save(request);

            // Reserve dates in availability calendar
            List<ServiceAvailability> availabilities = availabilityRepository
                    .findOverlappingAvailabilities(
                            request.getService(),
                            request.getStartDate(),
                            request.getEndDate()
                    );

            for (ServiceAvailability availability : availabilities) {
                availability.setIsReserved(true);
                availability.setReservedByRental(rental);
                availabilityRepository.save(availability);
            }
        }

        Rental saved = rentalRepository.save(rental);
        return toDto(saved);
    }

    @Transactional
    public RentalDto startRental(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));

        RentalRequest request = rental.getRentalRequest();

        if (request.getStatus() != RentalRequestStatus.CONFIRMED) {
            throw new IllegalArgumentException("Rental must be confirmed before starting");
        }

        if (!rental.getStartDate().equals(LocalDate.now()) &&
                !rental.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Rental start date has not arrived yet");
        }

        request.setStatus(RentalRequestStatus.IN_RENT);
        rentalRequestRepository.save(request);

        return toDto(rental);
    }

    @Transactional
    public RentalDto completeRental(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));

        RentalRequest request = rental.getRentalRequest();

        if (request.getStatus() != RentalRequestStatus.IN_RENT) {
            throw new IllegalArgumentException("Rental must be in rent status");
        }

        if (rental.getEndDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Rental end date has not arrived yet");
        }

        request.setStatus(RentalRequestStatus.COMPLETED);
        rental.setIsActive(false);
        rentalRequestRepository.save(request);
        rentalRepository.save(rental);

        // Notify both parties
        createNotification(
                rental.getSupplier(),
                NotificationType.REQUEST_APPROVED,
                "Аренда завершена",
                String.format("Аренда '%s' завершена. Оставьте отзыв!", rental.getService().getTitle()),
                rental.getId()
        );

        createNotification(
                rental.getTenant(),
                NotificationType.REQUEST_APPROVED,
                "Аренда завершена",
                String.format("Аренда '%s' завершена. Оставьте отзыв!", rental.getService().getTitle()),
                rental.getId()
        );

        return toDto(rental);
    }

    @Transactional(readOnly = true)
    public List<RentalDto> getMyRentals(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Rental> rentals = rentalRepository.findBySupplierOrTenant(user, user);
        return rentals.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RentalStatsDto getRentalStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Rental> rentals = rentalRepository.findBySupplierOrTenant(user, user);

        long totalRentals = rentals.size();
        long activeRentals = rentals.stream()
                .filter(r -> r.getIsActive() &&
                        (r.getRentalRequest().getStatus() == RentalRequestStatus.IN_RENT ||
                                r.getRentalRequest().getStatus() == RentalRequestStatus.CONFIRMED))
                .count();
        long completedRentals = rentals.stream()
                .filter(r -> r.getRentalRequest().getStatus() == RentalRequestStatus.COMPLETED)
                .count();

        java.math.BigDecimal totalRevenue = rentals.stream()
                .filter(r -> r.getRentalRequest().getStatus() == RentalRequestStatus.COMPLETED)
                .map(Rental::getTotalPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Calculate average rating from user's reviews
        java.math.BigDecimal averageRating = user.getAverageRating() != null ?
                user.getAverageRating() : java.math.BigDecimal.ZERO;

        return new org.dev.powermarket.service.dto.RentalStatsDto(
                totalRentals, activeRentals, completedRentals, totalRevenue, averageRating);
    }

    @Transactional(readOnly = true)
    public RentalDto getRental(String email, UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = rental.getSupplier().getId().equals(user.getId()) ||
                rental.getTenant().getId().equals(user.getId());

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have access to this rental");
        }

        return toDto(rental);
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

    private RentalDto toDto(Rental rental) {
        RentalDto dto = new RentalDto();
        dto.setId(rental.getId());
        dto.setServiceId(rental.getService().getId());
        dto.setServiceTitle(rental.getService().getTitle());
        dto.setSupplierId(rental.getSupplier().getId());
        dto.setSupplierName(rental.getSupplier().getFullName());
        dto.setTenantId(rental.getTenant().getId());
        dto.setTenantName(rental.getTenant().getFullName());
        dto.setStartDate(rental.getStartDate());
        dto.setCapacityRented(Double.parseDouble(rental.getService().getCapacity()));
        dto.setEndDate(rental.getEndDate());
        dto.setTotalPrice(rental.getTotalPrice());
        dto.setChatId(rental.getChat() != null ? rental.getChat().getId() : null);
        dto.setSupplierConfirmed(rental.getSupplierConfirmed());
        dto.setTenantConfirmed(rental.getTenantConfirmed());
        dto.setSupplierConfirmedAt(rental.getSupplierConfirmedAt());
        dto.setTenantConfirmedAt(rental.getTenantConfirmedAt());
        dto.setStatus(rental.getRentalRequest().getStatus());
        dto.setIsActive(rental.getIsActive());
        dto.setCreatedAt(rental.getCreatedAt());
        return dto;
    }
}
