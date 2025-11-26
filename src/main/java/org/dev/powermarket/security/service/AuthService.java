package org.dev.powermarket.security.service;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.dev.powermarket.domain.enums.Role;
import org.dev.powermarket.security.dto.RefreshTokenRequest;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.exception.AuthenticationException;
import org.dev.powermarket.security.repository.AuthorizedUserRepository;
import org.dev.powermarket.service.dto.AuthResponse;
import org.dev.powermarket.service.dto.LoginRequest;
import org.dev.powermarket.service.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthorizedUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        System.out.println(request + " 0");
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Неверный логин или пароль");
        }
        String token = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.fromEntity(user))
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String userLogin = jwtService.extractUsername(request.getRefreshToken());
        User user = userRepository.findByEmail(userLogin)
                .orElseThrow(() -> new AuthenticationException("Пользователь не найден"));

        if (!jwtService.isTokenValid(request.getRefreshToken(), userLogin)) {
            throw new AuthenticationException("Невалидный refresh token");
        }

        String token = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.fromEntity(user))
                .build();
    }

    public User getCurrentUser(String userLogin) {
        return userRepository.findByEmail(userLogin)
                .orElseThrow(() -> new AuthenticationException("Пользователь не найден"));
    }

    public boolean userExists(String username) {
        return userRepository.existsByEmail(username);
    }

    public AuthResponse register(@Valid RegisterRequest request) {
        // Проверяем, существует ли пользователь с таким email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        // Создаем нового пользователя
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.CUSTOMER)
                .fullName(request.getFullName())
                .companyName(request.getCompanyName())
                .inn(request.getInn())
                .phone(request.getPhone())
                .address(request.getAddress())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Сохраняем пользователя
        userRepository.save(user);

        // Генерируем JWT токены
        String token = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        // Возвращаем DTO с токенами и данными пользователя
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.fromEntity(user))
                .build();
    }

}
