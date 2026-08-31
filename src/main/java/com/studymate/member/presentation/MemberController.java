package com.studymate.member.presentation;

import com.studymate.member.application.MemberService;
import com.studymate.member.application.dto.request.MemberCreateRequest;
import com.studymate.member.application.dto.request.MemberUpdateRequest;
import com.studymate.member.application.dto.response.MemberResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(@PathVariable UUID memberId,
                                                       @RequestBody MemberUpdateRequest request) {
        MemberResponse memberResponse = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(memberResponse);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable UUID memberId) {
        MemberResponse memberResponse = memberService.getMember(memberId);
        return ResponseEntity.ok(memberResponse);
    }

    @DeleteMapping("/{memberId}")
    public void deleteMember(@PathVariable UUID memberId) {
        memberService.deleteMember(memberId);
    }

}
