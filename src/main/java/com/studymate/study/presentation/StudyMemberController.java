package com.studymate.study.presentation;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.study.application.dto.request.StudyMemberRequest;
import com.studymate.study.application.dto.response.StudyMemberPageResponse;
import com.studymate.study.application.dto.response.StudyMemberResponse;
import com.studymate.study.application.service.StudyMemberService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies/{studyId}/members/applications")
public class StudyMemberController {

    private final StudyMemberService studyMemberService;

    @PostMapping()
    public ResponseEntity<StudyMemberResponse> apply(@AuthenticationPrincipal MemberPrincipal principal,
                                                     @PathVariable UUID studyId,
                                                     @RequestBody StudyMemberRequest request) {

        StudyMemberResponse studyMemberResponse = studyMemberService.apply(studyId, principal.getMemberId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(studyMemberResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<StudyMemberResponse> getMyLatestApplication(
            @PathVariable UUID studyId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        StudyMemberResponse studyMemberResponse = studyMemberService.getMyLatestApplication(studyId,
                principal.getMemberId());

        return ResponseEntity.ok(studyMemberResponse);
    }

    @GetMapping("/pending")
    public ResponseEntity<StudyMemberPageResponse> getPendingApplications(
            @PathVariable UUID studyId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<StudyMemberResponse> result =
                studyMemberService.getPendingApplications(
                        studyId,
                        principal.getMemberId(),
                        page,
                        size
                );

        return ResponseEntity.ok(StudyMemberPageResponse.from(result));
    }

    @PatchMapping("/{studyMemberId}/approve")
    public ResponseEntity<StudyMemberResponse> approve(
            @PathVariable UUID studyId,
            @PathVariable UUID studyMemberId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        StudyMemberResponse response = studyMemberService.approve(
                studyId,
                studyMemberId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{studyMemberId}/reject")
    public ResponseEntity<StudyMemberResponse> reject(
            @PathVariable UUID studyId,
            @PathVariable UUID studyMemberId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        StudyMemberResponse response = studyMemberService.reject(
                studyId, studyMemberId, principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }
}
