package com.nchuy099.mini_instagram.auth.service;

import com.nchuy099.mini_instagram.auth.dto.AuthResponse;
import com.nchuy099.mini_instagram.auth.dto.LoginRequest;
import com.nchuy099.mini_instagram.auth.dto.RegisterRequest;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfullyWhenInputValid() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        UUID savedUserId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(savedUserId);
        savedUser.setUsername("testuser");
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.registerUser(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedUserId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already taken!");
    }

    @Test
    void shouldAuthenticateUserAndReturnToken() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("testuser");
        request.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(tokenProvider.generateToken("testuser")).thenReturn("mock-jwt-token");

        AuthResponse response = authService.authenticateUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-jwt-token");
    }
}
