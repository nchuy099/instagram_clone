package com.nchuy099.mini_instagram.common.security;

import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));
        
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "OAUTH2_USER";
        
        // Use email as primary key for session, fallback to username
        String principal = user.getEmail() != null ? user.getEmail() : user.getUsername();
        
        return new org.springframework.security.core.userdetails.User(
                principal,
                password,
                new ArrayList<>()
        );
    }
}
