package com.aes.auth;

import com.aes.auth.dto.AuthResponse;
import com.aes.auth.dto.LoginRequest;
import com.aes.auth.dto.PasswordResetRequest;
import com.aes.auth.dto.RegisterRequest;
import com.aes.common.BadRequestException;
import com.aes.common.Role;
import com.aes.audit.AuditService;
import com.aes.users.User;
import com.aes.users.UserRepository;
import com.aes.users.UserStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService,
                        AuthenticationManager authenticationManager, AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String name = request.name().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("ADMIN accounts cannot be self-registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        if (auditService != null) auditService.log(user.getId(), "REGISTER", "User", user.getId(), "role=" + user.getRole());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account has been deactivated. Please contact administrator.");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException e) {
            if (auditService != null) auditService.log(null, "LOGIN_FAILED", "User", null, "email=" + email);
            throw new BadCredentialsException("Invalid email or password");
        }
        if (auditService != null) auditService.log(user.getId(), "LOGIN", "User", user.getId(), "successful=true");
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token has expired");
        }
        User user = token.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is deactivated");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with email: " + email));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
        if (auditService != null) auditService.log(user.getId(), "PASSWORD_RESET", "User", user.getId(), "email=" + email);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), Map.of(
                "userId", user.getId(),
                "role", user.getRole().name(),
                "name", user.getName()
        ));
        
        // Generate Refresh Token valid for 7 days
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(token, refreshToken.getToken(), user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
