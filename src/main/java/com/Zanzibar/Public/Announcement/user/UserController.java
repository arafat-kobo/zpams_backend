package com.Zanzibar.Public.Announcement.user;

import com.Zanzibar.Public.Announcement.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;


    // ==========================================
    // CREATE MODERATOR
    // ==========================================

    @PostMapping
    public ResponseEntity<User> createModerator(
            @RequestBody User user
    ) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException(
                    "Email already exists"
            );
        }

        user.setId(null);

        // Keep the original password only for the account-creation email.
        // The database will store only the BCrypt hash.
        String plainPassword = user.getPassword();

        user.setPassword(
                passwordEncoder.encode(
                        plainPassword
                )
        );

        user.setRole(Role.MODERATOR);

        user.setStatus(UserStatus.ACTIVE);

        User savedUser =
                userRepository.save(user);

        emailService.sendStaffCredentials(
                savedUser.getFullName(),
                savedUser.getEmail(),
                plainPassword,
                "MODERATOR"
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedUser);
    }


    // ==========================================
    // CREATE RADIO OPERATOR
    // ==========================================

    @PostMapping("/radio-operators")
    public ResponseEntity<User> createRadioOperator(
            @RequestBody User user
    ) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException(
                    "Email already exists"
            );
        }

        user.setId(null);

        // Keep the original password only for the account-creation email.
        // The database will store only the BCrypt hash.
        String plainPassword = user.getPassword();

        user.setPassword(
                passwordEncoder.encode(
                        plainPassword
                )
        );

        user.setRole(Role.RADIO_OPERATOR);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser =
                userRepository.save(user);

        emailService.sendStaffCredentials(
                savedUser.getFullName(),
                savedUser.getEmail(),
                plainPassword,
                "RADIO_OPERATOR"
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedUser);
    }


    // ==========================================
    // GET ALL USERS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        return ResponseEntity.ok(
                userRepository.findAll()
        );
    }


    // ==========================================
    // GET USER BY ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable Long id
    ) {

        User user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        return ResponseEntity.ok(user);
    }


    // ==========================================
    // UPDATE USER
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User request
    ) {

        User user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        user.setFullName(
                request.getFullName()
        );

        user.setPhone(
                request.getPhone()
        );

        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(
                            request.getPassword()
                    )
            );
        }

        User updatedUser =
                userRepository.save(user);

        return ResponseEntity.ok(updatedUser);
    }


    // ==========================================
    // CHANGE USER STATUS
    // ==========================================

    @PatchMapping("/{id}/status")
    public ResponseEntity<User> changeStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status
    ) {

        User user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        user.setStatus(status);

        return ResponseEntity.ok(
                userRepository.save(user)
        );
    }


    // ==========================================
    // DELETE USER
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id
    ) {

        if (!userRepository.existsById(id)) {

            throw new RuntimeException(
                    "User not found"
            );
        }

        userRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
