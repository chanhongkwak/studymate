package com.studymate.auth.infrastructure;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtErrorResponseTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidTokenReturns401WithoutContinuingChain() throws Exception {
        JwtTokenProvider provider = mock(JwtTokenProvider.class);
        when(provider.getClaims("invalid")).thenThrow(new JwtException("internal detail"));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider,
                new SecurityExceptionHandler(JsonMapper.builder().build()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
        assertFalse(response.getContentAsString().contains("internal detail"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }
}
