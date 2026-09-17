package com.studymate.auth.application;

import com.studymate.auth.application.request.LoginRequest;
import com.studymate.auth.application.response.LoginResponse;
import com.studymate.auth.infrastructure.JwtTokenProvider;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String TOKEN_TYPE = "Bearer";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request){
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException(("이메일 또는 비밀번호가 올바르지 않습니다.")));

        if(member.getStatus() != MemberStatus.ACTIVE || member.getDeletedAt() != null){
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if(!passwordEncoder.matches(request.password(),member.getPassword())){
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member);

        return new LoginResponse(
                accessToken,
                TOKEN_TYPE,

                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

}
