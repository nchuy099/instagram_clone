package com.nchuy099.mini_instagram.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.user.dto.request.CreateUserReq;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String create(CreateUserReq req) {
        log.info("Creating user with email: {}", req.getEmail());
        // Implementation logic to create a user goes here

        Optional<UserEntity> existingUser = this.userRepository.findByUsername(req.getUsername());

        if (existingUser.isPresent()) {
            log.warn("Username {} is already taken", req.getUsername());
            throw new AppException(ErrorCode.NOT_FOUND);
        }

        var newUser = this.userRepository.save(UserEntity.builder()
                .email(req.getEmail())
                .username(req.getUsername())
                .fullName(req.getFullName())
                .password(passwordEncoder.encode(req.getPassword()))
                .biography(req.getBiography())
                .gender(req.getGender())
                .phoneNumber(req.getPhoneNumber())
                .dateOfBirth(req.getDateOfBirth())
                .build());
        return newUser.getId().toString();

    }
}
