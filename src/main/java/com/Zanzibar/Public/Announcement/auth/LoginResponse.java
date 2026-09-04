package com.Zanzibar.Public.Announcement.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String fullName;

    private String role;

}