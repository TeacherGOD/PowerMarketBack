package org.dev.powermarket.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dev.powermarket.security.dto.RefreshTokenRequest;
import org.dev.powermarket.security.entity.User;
import org.dev.powermarket.security.service.AuthService;
import org.dev.powermarket.service.dto.AuthResponse;
import org.dev.powermarket.service.dto.LoginRequest;
import org.dev.powermarket.service.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register new user",
            description = "Register a new user with SUPPLIER or TENANT role"
    )
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        System.out.println("Я тут");
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser(Authentication authentication) {
        String userLogin = authentication.getName();
        User user = authService.getCurrentUser(userLogin);
        return ResponseEntity.ok(AuthResponse.UserDto.fromEntity(user));
    }
}
