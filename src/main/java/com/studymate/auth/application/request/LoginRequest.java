package com.studymate.auth.application.request;

public record LoginRequest(
        String email,
        String password
) {
}
