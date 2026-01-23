package com.nchuy099.mini_instagram.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nchuy099.mini_instagram.user.dto.request.UpdateUserProfileReq;
import com.nchuy099.mini_instagram.user.dto.response.UserProfileResp;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me/details")
    public UserProfileResp getUserProfile() {
        log.info("Received Get user profile request");
        return userService.getProfile();
    }

    @PutMapping("/me/update")
    public UserProfileResp updateUserProfile(@RequestBody @Valid UpdateUserProfileReq req) {
        log.info("Received update user profile request");
        return userService.updateProfile(req);
    }

}
