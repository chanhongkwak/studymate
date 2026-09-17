package com.studymate.auth.application.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
