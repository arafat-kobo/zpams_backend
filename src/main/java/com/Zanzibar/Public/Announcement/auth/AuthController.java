package com.Zanzibar.Public.Announcement.auth;

import com.Zanzibar.Public.Announcement.security.jwt.JwtService;
import com.Zanzibar.Public.Announcement.user.User;
import com.Zanzibar.Public.Announcement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            // Authentication failures should be reported as 401, not 403.
            // This makes invalid credentials distinguishable from a valid
            // account that is forbidden from using a protected endpoint.
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        User user = userRepository
                .findByEmail(email)
                .orElseThrow();

        String role = user.getRole().name();

        String token = jwtService.generateToken(
                user.getEmail(),
                role
        );

        return new LoginResponse(
                token,
                user.getFullName(),
                role
        );
    }
}
