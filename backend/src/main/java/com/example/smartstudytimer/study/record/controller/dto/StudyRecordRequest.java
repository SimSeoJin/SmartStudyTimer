package com.example.smartstudytimer.study.record.controller.dto;

import com.example.smartstudytimer.study.record.Entity.StudyMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyRecordRequest {
    private Long memberId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    // 없으면 BASIC으로 본다(모드를 보내지 않던 이전 앱 버전 호환). 검증되지 않은 시간이 랭킹에 들어가지 않도록.
    private StudyMode mode;
}