package com.studymate.global.exception;

public record ErrorResponse(
        String code,
        String message
) {
}
