package org.dev.powermarket.security.entity;


import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

import lombok.*;
import org.dev.powermarket.domain.enums.Role;
import org.hibernate.annotations.UuidGenerator;


@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User{

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "inn", unique = true)
    private String inn; // ИНН

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private java.math.BigDecimal averageRating;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", role=" + role +
                ", fullName='" + fullName + '\'' +
                ", companyName='" + companyName + '\'' +
                ", inn='" + inn + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", averageRating=" + averageRating +
                ", totalReviews=" + totalReviews +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
