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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.nchuy099.mini_instagram.MiniInstagramApplication;
import com.nchuy099.mini_instagram.user.dto.request.CreateUserReq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = MiniInstagramApplication.class)
class UserServiceIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @Test
    void create_success() {
        // given
        CreateUserReq req = new CreateUserReq();
        req.setEmail("test@gmail.com");
        req.setUsername("testuser");
        req.setFullName("Test User");
        req.setPassword("123456");

        // when
        String userId = userService.create(req);
        log.info("Created User ID: {}", userId);
        assertNotNull(userId);

        UserEntity savedUser = userRepository
                .findById(UUID.fromString(userId))
                .orElseThrow();

        log.info("Saved User: {}", savedUser);
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@gmail.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getFullName());

    }

    @Test
    void create_usernameExists() {
        // given
        UserEntity existing = new UserEntity();
        existing.setUsername("existing");
        existing.setEmail("exist@gmail.com");
        existing.setFullName("Existing User");
        existing.setPassword("password");

        userRepository.save(existing);

        CreateUserReq req = new CreateUserReq();
        req.setUsername("existing");

        // when
        String result = userService.create(req);

        log.info("Result when username exists: {}", result);

        // then
        assertEquals("Username is already taken", result);
    }
}