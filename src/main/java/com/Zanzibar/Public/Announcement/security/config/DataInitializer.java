package com.Zanzibar.Public.Announcement.security.config;

import com.Zanzibar.Public.Announcement.user.Role;
import com.Zanzibar.Public.Announcement.user.User;
import com.Zanzibar.Public.Announcement.user.UserRepository;
import com.Zanzibar.Public.Announcement.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!userRepository.existsByEmail(
                "admin@zpams.com"
        )) {

            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@zpams.com")
                    .password(
                            passwordEncoder.encode("123456")
                    )
                    .phone("0770000000")
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();

            userRepository.save(admin);
        }
    }
}