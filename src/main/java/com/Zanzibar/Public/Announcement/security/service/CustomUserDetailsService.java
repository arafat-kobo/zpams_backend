package com.Zanzibar.Public.Announcement.security.service;

import com.Zanzibar.Public.Announcement.user.User;
import com.Zanzibar.Public.Announcement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String cleanEmail = email == null ? "" : email.trim();

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + cleanEmail));

        if (user.getRole() == null) {
            throw new UsernameNotFoundException(
                    "User has no assigned role: " + cleanEmail);
        }

        String authority = "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority(authority))
                .disabled(user.getStatus() != null
                        && user.getStatus().name().equals("INACTIVE"))
                .build();
    }
}
