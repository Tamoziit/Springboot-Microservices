package com.tamojit.authservice.dto;

public class LoginResponseDTO {
    private final String token;

    // for only 1 property - better approach is constructor, rather than getter-setters (for setting values)
    public LoginResponseDTO(String token) {
        this.token = token;
    }

    // but still getter is required to retrieve value
    public String getToken() {
        return token;
    }
}
