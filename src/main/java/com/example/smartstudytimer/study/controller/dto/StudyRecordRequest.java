package com.example.smartstudytimer.study.dto;

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
    private Integer studyMinutes;
}