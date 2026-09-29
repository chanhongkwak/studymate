package com.studymate.study.application.service;

import com.studymate.study.application.dto.response.StudyMemberResponse;
import com.studymate.study.domain.StudyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyMemberService {

    private final StudyMemberRepository studyMemberRepository;


}
