package com.example.smartstudytimer.study.record.controller;

import com.example.smartstudytimer.study.record.controller.dto.StudyRecordRequest;
import com.example.smartstudytimer.study.record.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/study")
public class StudyController {

    private final StudyService studyService;

    @PostMapping("/record")
    public String saveRecord(@AuthenticationPrincipal Long memberId, @RequestBody StudyRecordRequest request) {
        // memberId는 클라이언트가 아니라 토큰에서 인증된 값을 신뢰한다.
        request.setMemberId(memberId);
        return studyService.recordStudy(request);
    }
}