package org.dev.powermarket.service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.dev.powermarket.domain.enums.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private Role role;
    private String fullName;
    private String companyName;
    private String inn;
    private String phone;
    private String address;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Instant createdAt;

    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", fullName='" + fullName + '\'' +
                ", companyName='" + companyName + '\'' +
                ", inn='" + inn + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", averageRating=" + averageRating +
                ", totalReviews=" + totalReviews +
                ", createdAt=" + createdAt +
                '}';
    }
}
