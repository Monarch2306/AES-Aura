package com.aes.auth;

import com.aes.auth.dto.AuthResponse;
import com.aes.auth.dto.RegisterRequest;
import com.aes.audit.AuditService;
import com.aes.common.BadRequestException;
import com.aes.common.Role;
import com.aes.users.User;
import com.aes.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock AuthenticationManager authenticationManager;
    @Mock AuditService auditService;
    @InjectMocks AuthService service;

    @Test void adminCannotSelfRegister() {
        var req = new RegisterRequest("Admin", "admin@example.com", "password123", Role.ADMIN);
        when(users.existsByEmail("admin@example.com")).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.register(req));
        verify(users, never()).save(any());
    }

    @Test void registrationNormalizesEmailAndName() {
        var req = new RegisterRequest("  Alice  ", " Alice@Example.COM ", "password123", Role.TEACHER);
        when(users.existsByEmail("alice@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hash");
        when(users.save(any(User.class))).thenAnswer(i -> { User u=i.getArgument(0); u.setId(7L); return u; });
        when(jwt.generateToken(any(), any())).thenReturn("token");
        AuthResponse out=service.register(req);
        assertEquals("alice@example.com", out.email());
        assertEquals("Alice", out.name());
        verify(users).save(argThat(u -> u.getEmail().equals("alice@example.com") && u.getName().equals("Alice")));
    }
}
