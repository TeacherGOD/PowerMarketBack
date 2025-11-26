package org.dev.powermarket.service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dev.powermarket.security.entity.User;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private UserDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private UUID id;
        private String email;
        private String fullName;
        private String companyName;
        private String inn;
        private String phone;
        private String address;
        private String role;
        private Instant createdAt;
        private Instant updatedAt;

        public static UserDto fromEntity(User user) {
            if (user == null) return null;
            return UserDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .companyName(user.getCompanyName())
                    .inn(user.getInn())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .role(user.getRole() != null ? user.getRole().toString() : null)
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }
}


