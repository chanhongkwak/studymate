package com.studymate.member.application;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.member.application.dto.request.MemberCreateRequest;
import com.studymate.member.application.dto.request.MemberUpdateRequest;
import com.studymate.member.application.dto.response.MemberResponse;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
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
    public MemberResponse createMember(MemberCreateRequest memberCreateRequest) {
        if (memberRepository.existsByEmail(memberCreateRequest.email())) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(memberCreateRequest.password());

        Member member = new Member(memberCreateRequest.email(), encodedPassword,
                memberCreateRequest.nickname(), memberCreateRequest.activityRegion());

        Member savedMember = memberRepository.save(member);

        return MemberResponse.from(savedMember);
    }

    @Transactional
    public MemberResponse updateMember(UUID memberId, MemberUpdateRequest memberUpdateRequest) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (memberUpdateRequest.email() != null
                && !memberUpdateRequest.email().equals(member.getEmail())
                && memberRepository.existsByEmail(memberUpdateRequest.email())) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
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
        return MemberResponse.from(member);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(UUID memberId) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        return MemberResponse.from(member);
    }

    @Transactional
    public void deleteMember(UUID memberId) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        member.delete(memberId);
    }
}
