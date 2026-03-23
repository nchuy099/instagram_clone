package com.nchuy099.mini_instagram.auth.service;

import com.nchuy099.mini_instagram.auth.dto.AuthResponse;
import com.nchuy099.mini_instagram.auth.dto.LoginRequest;
import com.nchuy099.mini_instagram.auth.dto.RegisterRequest;
import com.nchuy099.mini_instagram.auth.entity.UserRefreshToken;
import com.nchuy099.mini_instagram.auth.repository.UserRefreshTokenRepository;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRefreshTokenRepository userRefreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_WhenEmailValid_ShouldRegisterSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setMobileOrEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.registerUser(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WhenPhoneValid_ShouldRegisterSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setMobileOrEmail("+12345678901");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+12345678901")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        User savedUser = User.builder().id(UUID.randomUUID()).phoneNumber("+12345678901").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.registerUser(request);

        assertThat(result).isNotNull();
        assertThat(result.getPhoneNumber()).isEqualTo("+12345678901");
    }

    @Test
    void registerUser_WhenUsernameExists_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already taken!");
    }

    @Test
    void registerUser_WhenInvalidFormat_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest();
        request.setMobileOrEmail("invalid-format");

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid mobile number or email format!");
    }

    @Test
    void authenticateUser_ShouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        User user = User.builder().username("testuser").email("test@example.com").build();
        when(userRepository.findByUsernameOrEmailOrPhoneNumber(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(tokenProvider.generateToken("test@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.authenticateUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isNotBlank();
        verify(userRefreshTokenRepository).save(any(UserRefreshToken.class));
    }

    @Test
    void refreshToken_WhenValid_ShouldRotateTokens() {
        String oldRefreshToken = "old-token";
        User user = User.builder().username("testuser").email("test@example.com").build();
        UserRefreshToken session = UserRefreshToken.builder()
                .user(user)
                .expiresAt(ZonedDateTime.now().plusDays(1))
                .build();

        when(userRefreshTokenRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(session));
        when(tokenProvider.generateToken("test@example.com")).thenReturn("new-jwt");

        AuthResponse response = authService.refreshToken(oldRefreshToken);

        assertThat(response.getAccessToken()).isEqualTo("new-jwt");
        assertThat(response.getRefreshToken()).isNotEqualTo(oldRefreshToken);
        verify(userRefreshTokenRepository).save(session);
    }

    @Test
    void refreshToken_WhenExpired_ShouldThrowException() {
        UserRefreshToken session = UserRefreshToken.builder()
                .expiresAt(ZonedDateTime.now().minusDays(1))
                .build();
        when(userRefreshTokenRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.refreshToken("some-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    void logout_ShouldRevokeToken() {
        UserRefreshToken session = UserRefreshToken.builder().build();
        when(userRefreshTokenRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(session));

        authService.logout("token-to-revoke");

        assertThat(session.getRevokedAt()).isNotNull();
        verify(userRefreshTokenRepository).save(session);
    }

    @Test
    void getCurrentUser_WhenAuthenticated_ShouldReturnDTO() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("testuser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .fullName("Full Name")
                .build();
        when(userRepository.findByUsernameOrEmailOrPhoneNumber("testuser", "testuser", "testuser"))
                .thenReturn(Optional.of(user));

        UserDTO result = authService.getCurrentUser();

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getFullName()).isEqualTo("Full Name");
    }

    @Test
    void getCurrentUser_WhenNotAuthenticated_ShouldThrowException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user found");
    }
}
