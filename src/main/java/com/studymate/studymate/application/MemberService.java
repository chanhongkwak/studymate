package com.studymate.studymate.application;

import com.studymate.studymate.application.dto.request.MemberCreateRequest;
import com.studymate.studymate.application.dto.request.MemberUpdateRequest;
import com.studymate.studymate.application.dto.response.MemberCreateResponse;
import com.studymate.studymate.application.dto.response.MemberUpdateResponse;
import com.studymate.studymate.domain.Member;
import com.studymate.studymate.domain.MemberRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberCreateResponse createMember(MemberCreateRequest memberCreateRequest) {
        if (memberRepository.existsByEmail(memberCreateRequest.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(memberCreateRequest.password());

        Member member = new Member(memberCreateRequest.email(), encodedPassword,
                memberCreateRequest.nickname(), memberCreateRequest.activityRegion());

        Member savedMember = memberRepository.save(member);

        return MemberCreateResponse.from(savedMember);
    }

    @Transactional
    public MemberUpdateResponse updateMember(UUID memberId, MemberUpdateRequest memberUpdateRequest){
        Member member = memberRepository.findByIdAndDeleteAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (memberUpdateRequest.email() != null
                && !memberUpdateRequest.email().equals(member.getEmail())
                && memberRepository.existsByEmail(memberUpdateRequest.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = memberUpdateRequest.password() == null
                ? null
                : passwordEncoder.encode(memberUpdateRequest.password());

        member.update(
                memberUpdateRequest.email(),
                encodedPassword,
                memberUpdateRequest.nickname(),
                memberUpdateRequest.activityRegion()
        );
        return MemberUpdateResponse.from(member);
    }
}
