package com.nchuy099.mini_instagram.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nchuy099.mini_instagram.user.dto.request.CreateUserReq;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    void create_success() {
        // given
        CreateUserReq req = new CreateUserReq();
        req.setEmail("test@gmail.com");
        req.setUsername("testuser");
        req.setFullName("Test User");
        req.setPassword("123456");

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.empty());

        UserEntity savedUser = new UserEntity();
        savedUser.setId(UUID.randomUUID().toString());

        when(userRepository.save(any()))
                .thenReturn(savedUser);

        // when
        String result = userService.create(req);
        log.info("Created User ID: {}", result);
        // then
        assertNotNull(result);
        assertDoesNotThrow(() -> UUID.fromString(result));
        log.info("Verifying that the result is a valid UUID");

    }

    @Test
    void create_usernameExists() {
        // given
        when(userRepository.findByUsername("existing"))
                .thenReturn(Optional.of(new UserEntity()));

        CreateUserReq req = new CreateUserReq();
        req.setUsername("existing");

        // when
        String result = userService.create(req);

        // then
        assertEquals("Username is already taken", result);
        verify(userRepository, never()).save(any());
    }
}