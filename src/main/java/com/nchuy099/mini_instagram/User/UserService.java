package com.nchuy099.mini_instagram.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.user.dto.request.CreateUserReq;
import com.nchuy099.mini_instagram.user.dto.request.UpdateUserProfileReq;
import com.nchuy099.mini_instagram.user.dto.response.UserProfileResp;
import com.nchuy099.mini_instagram.common.utils.SecurityUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    public String create(CreateUserReq req) {
        log.info("Creating user with email: {}", req.getEmail());
        // Implementation logic to create a user goes here

        Optional<UserEntity> existingUser = this.userRepository.findByUsernameOrEmail(req.getUsername(),
                req.getEmail());

        if (existingUser.isPresent()) {
            log.warn("Email/ Username is already taken");
            throw new AppException(ErrorCode.CONFLICT, "Email/ Username is already taken");
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

    public UserProfileResp getProfile() {
        log.info("Processing Get user profile request");

        UUID id = securityUtils.getCurrentUserId();
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found");
                    return new AppException(
                            ErrorCode.NOT_FOUND, "User not found");
                });

        return toUserProfileResp(userEntity);

    }

    public UserProfileResp updateProfile(UpdateUserProfileReq req) {
        log.info("Processing update user profile request");

        UUID id = securityUtils.getCurrentUserId();
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found");
                    return new AppException(
                            ErrorCode.NOT_FOUND, "User not found");
                });

        if (req.getEmail() != null && !req.getEmail().equals(userEntity.getEmail())) {

            userRepository.findByEmail(req.getEmail())
                    .orElseThrow(() -> {
                        log.warn("Email is already taken");
                        return new AppException(
                                ErrorCode.CONFLICT, "Email is already taken");
                    });
            userEntity.setEmail(req.getEmail());
        }

        if (req.getUsername() != null && !req.getUsername().equals(userEntity.getUsername())) {
            userRepository.findByUsername(req.getUsername())
                    .orElseThrow(() -> {
                        log.warn("Username is already taken");
                        return new AppException(
                                ErrorCode.CONFLICT, "Usernames is already taken");
                    });
            userEntity.setUsername(req.getUsername());
        }

        if (req.getFullName() != null)
            userEntity.setFullName(req.getFullName());

        if (req.getBiography() != null)
            userEntity.setBiography(req.getBiography());

        if (req.getDateOfBirth() != null)
            userEntity.setDateOfBirth(req.getDateOfBirth());

        if (req.getGender() != null)
            userEntity.setGender(req.getGender());

        if (req.getPhoneNumber() != null)
            userEntity.setPhoneNumber(req.getPhoneNumber());

        userRepository.save(userEntity);

        return toUserProfileResp(userEntity);

    }

    private UserProfileResp toUserProfileResp(UserEntity userEntity) {
        return UserProfileResp.builder()
                .userId(userEntity.getId().toString())
                .email(userEntity.getEmail())
                .biography(userEntity.getAvatarUrl())
                .username(userEntity.getUsername())
                .fullName(userEntity.getFullName())
                .avatarUrl(userEntity.getAvatarUrl())
                .dateOfBirth(userEntity.getDateOfBirth())
                .gender(userEntity.getGender())
                .phoneNumber(userEntity.getPhoneNumber())
                .build();
    }
}
