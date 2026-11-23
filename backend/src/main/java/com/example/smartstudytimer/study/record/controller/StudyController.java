package com.example.smartstudytimer.study.record.controller;

import com.example.smartstudytimer.study.record.controller.dto.StudyRecordRequest;
import com.example.smartstudytimer.study.record.controller.dto.StudyRecordResponse;
import com.example.smartstudytimer.study.record.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    // 새 기기에서 로그인했을 때 앱이 내 기록 전체를 내려받는다. 회원은 토큰에서 정한다.
    @GetMapping("/record")
    public List<StudyRecordResponse> getRecords(@AuthenticationPrincipal Long memberId) {
        return studyService.getRecords(memberId);
    }
}
