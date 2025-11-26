package org.dev.powermarket.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Исключение для ошибок JWT аутентификации
 */
public class JwtAuthenticationException extends AuthenticationException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
