package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.LoginRequest;
import com.falcon.airlines.dto.request.LogoutRequest;
import com.falcon.airlines.dto.request.RefreshTokenRequest;
import com.falcon.airlines.dto.request.RegisterRequest;
import com.falcon.airlines.dto.response.TokenResponse;
import com.falcon.airlines.dto.response.UserResponse;
import com.falcon.airlines.entity.RefreshToken;
import com.falcon.airlines.entity.Role;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.entity.UserRole;
import com.falcon.airlines.enums.TokenStatus;
import com.falcon.airlines.enums.UserStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.exception.DuplicateResourceException;
import com.falcon.airlines.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import com.falcon.airlines.repository.RefreshTokenRepository;
import com.falcon.airlines.repository.RoleRepository;
import com.falcon.airlines.repository.UserRepository;
import com.falcon.airlines.repository.UserRoleRepository;
import com.falcon.airlines.security.jwt.JwtProperties;
import com.falcon.airlines.security.jwt.JwtService;
import com.falcon.airlines.security.principal.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles authentication-related business operations such as registration.
 * <p>
 * A newly registered user is automatically assigned the {@code CUSTOMER} role,
 * gets an active status, and a BCrypt-hashed password.
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       UserMapper userMapper,
                       BCryptPasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userDetailsService = userDetailsService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already in use: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setMfaEnabled(Boolean.FALSE);
        user.setFailedLoginAttempts((short) 0);
        user.setLockedUntil(null);
        user.setLastLoginAt(null);

        User savedUser = userRepository.save(user);

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(this::createCustomerRole);

        assignRole(savedUser, customerRole);

        return userMapper.toResponse(savedUser);
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BaseException("Invalid username or password", HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR", ex);
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setStatus(TokenStatus.ACTIVE);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtProperties.refreshTokenExpiration()));
        refreshToken.setIpAddress(request.getIpAddress());
        refreshToken.setUserAgent(request.getUserAgent());
        refreshToken.setDeviceInfo(request.getDeviceInfo());
        refreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        Instant issuedAt = Instant.now();
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtService.getAccessTokenExpiration()))
                .refreshTokenStatus(TokenStatus.ACTIVE)
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    private Role createCustomerRole() {
        Role role = new Role();
        role.setName("CUSTOMER");
        role.setDescription("Default passenger/customer role");
        role.setIsSystem(Boolean.FALSE);
        return roleRepository.save(role);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BaseException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));

        if (refreshToken.getStatus() != TokenStatus.ACTIVE
                || refreshToken.getExpiresAt().isBefore(Instant.now())
                || refreshToken.isDeleted()) {
            throw new BaseException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }

        User user = refreshToken.getUser();
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new BaseException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = (UserPrincipal) userDetails;

        String accessToken = jwtService.generateAccessToken(userPrincipal);

        refreshToken.setStatus(TokenStatus.REVOKED);
        refreshToken.setRevokedAt(Instant.now());
        refreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        String newRefreshTokenValue = UUID.randomUUID().toString();
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(newRefreshTokenValue);
        newRefreshToken.setUser(user);
        newRefreshToken.setStatus(TokenStatus.ACTIVE);
        newRefreshToken.setExpiresAt(Instant.now().plusSeconds(jwtProperties.refreshTokenExpiration()));
        newRefreshToken.setIpAddress(request.getIpAddress());
        newRefreshToken.setUserAgent(request.getUserAgent());
        newRefreshToken.setDeviceInfo(request.getDeviceInfo());
        newRefreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(newRefreshToken);

        Instant issuedAt = Instant.now();
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtService.getAccessTokenExpiration()))
                .refreshTokenStatus(TokenStatus.ACTIVE)
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }

    private void assignRole(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setValidFrom(Instant.now());
        userRole.setValidUntil(null);
        userRoleRepository.save(userRole);
    }
}
