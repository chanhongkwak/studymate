package com.studymate.auth.infrastructure;

import com.studymate.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(BEARER_PREFIX.length());

        if (jwtTokenProvider.validateToken(token)) {
            Claims claims =
                    jwtTokenProvider.getClaims(token);

            MemberPrincipal principal = new MemberPrincipal(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    MemberRole.valueOf(claims.get("role", String.class))
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
