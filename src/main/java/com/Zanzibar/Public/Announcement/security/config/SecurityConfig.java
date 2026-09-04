package com.Zanzibar.Public.Announcement.security.config;

import com.Zanzibar.Public.Announcement.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ---------------- PUBLIC AUTH ----------------
                        .requestMatchers("/api/auth/**").permitAll()

                        // ---------------- STAFF ANNOUNCEMENTS ----------------
                        .requestMatchers("/api/announcements/moderator/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR")

                        .requestMatchers("/api/announcements/admin")
                        .hasAuthority("ROLE_ADMIN")

                        // ---------------- PUBLIC READ RESOURCES ----------------
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/announcements/*/image"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/announcements/public",
                                "/api/announcements/public/track/*"
                        ).permitAll()

                        // ---------------- RADIO PDF ----------------
                        // PDF is intentionally public because the native PDF viewer
                        // may not forward the Axios JWT header. Only approved or
                        // broadcasted PDFs are returned by the controller.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/radio/announcements/*/pdf"
                        ).permitAll()

                        // ---------------- RADIO OPERATOR ----------------
                        // Use explicit authorities instead of hasRole() so there is
                        // no ambiguity about Spring's ROLE_ prefix.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/radio/announcements"
                        ).hasAuthority("ROLE_RADIO_OPERATOR")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/radio/announcements/*/broadcast"
                        ).hasAuthority("ROLE_RADIO_OPERATOR")

                        // No other /api/radio endpoint is allowed by accident.
                        .requestMatchers("/api/radio/**").denyAll()

                        // ---------------- ADMIN ONLY ----------------
                        .requestMatchers(
                                "/api/reports/**",
                                "/api/users/**"
                        ).hasAuthority("ROLE_ADMIN")

                        // ---------------- CITIZEN ANNOUNCEMENT FLOW ----------------
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/announcements/preview",
                                "/api/announcements",
                                "/api/announcements/*/submit",
                                "/api/announcements/*/upload"
                        ).permitAll()

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
