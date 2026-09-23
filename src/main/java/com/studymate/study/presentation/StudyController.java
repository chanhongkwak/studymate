package com.studymate.study.presentation;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.study.application.StudyService;
import com.studymate.study.application.dto.request.StudyCreateRequest;
import com.studymate.study.application.dto.request.StudyStatusUpdatedRequest;
import com.studymate.study.application.dto.request.StudyUpdateRequest;
import com.studymate.study.application.dto.response.StudyPageResponse;
import com.studymate.study.application.dto.response.StudyResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/studies")
public class StudyController {

    private final StudyService studyService;

    @PostMapping
    public ResponseEntity<StudyResponse> createStudy(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody StudyCreateRequest request
    ) {
        StudyResponse response = studyService.create(
                principal.getMemberId(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{studyId}")
    public ResponseEntity<StudyResponse> updateStudy(
            @PathVariable UUID studyId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody StudyUpdateRequest request
    ) {
        StudyResponse response = studyService.update(studyId, principal.getMemberId(), request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{studyId}")
    public ResponseEntity<Void> deleteStudy(
            @PathVariable UUID studyId,
            @AuthenticationPrincipal MemberPrincipal principal
    ){
        studyService.delete(studyId, principal.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{studyId}")
    public ResponseEntity<StudyResponse> getStudy(
            @PathVariable UUID studyId
    ) {
        StudyResponse response = studyService.getStudy(studyId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<StudyPageResponse> getStudies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<StudyResponse> result = studyService.getStudies(page, size);

        return ResponseEntity.ok(StudyPageResponse.from(result));
    }

    @PatchMapping("/{studyId}/status")
    public ResponseEntity<StudyResponse> changeStatus(
            @PathVariable UUID studyId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody StudyStatusUpdatedRequest request
    ) {
        StudyResponse response = studyService.changeStatus(
                studyId,
                principal.getMemberId(),
                request
        );

        return ResponseEntity.ok(response);
    }

}
