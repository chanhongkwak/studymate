package com.studymate.member.presentation;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.member.application.MemberService;
import com.studymate.member.application.dto.request.MemberCreateRequest;
import com.studymate.member.application.dto.request.MemberUpdateRequest;
import com.studymate.member.application.dto.response.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> createMember(@RequestBody MemberCreateRequest request) {
        MemberResponse memberResponse = memberService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponse);
    }

    @PatchMapping("/me")
    public ResponseEntity<MemberResponse> updateMember(@AuthenticationPrincipal MemberPrincipal principal,
                                                       @RequestBody MemberUpdateRequest request) {
        MemberResponse memberResponse = memberService.updateMember(principal.getMemberId(), request);
        return ResponseEntity.ok(memberResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMe(
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        MemberResponse memberResponse = memberService.getMember(principal.getMemberId());
        return ResponseEntity.ok(memberResponse);
    }

    @DeleteMapping("/me")
    public void deleteMember(@AuthenticationPrincipal MemberPrincipal principal) {
        memberService.deleteMember(principal.getMemberId());
    }

}
