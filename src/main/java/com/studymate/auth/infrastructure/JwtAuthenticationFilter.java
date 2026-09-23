package com.studymate.auth.infrastructure;

import com.studymate.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityExceptionHandler securityExceptionHandler;

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

        try {
            Claims claims =
                    jwtTokenProvider.getClaims(token);

            if (claims.getSubject() == null || claims.get("role", String.class) == null
                    || claims.get("email", String.class) == null) {
                throw new IllegalArgumentException("필수 토큰 정보가 없습니다.");
            }

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
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            securityExceptionHandler.commence(request, response,
                    new BadCredentialsException("유효하지 않은 토큰입니다.", exception));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
