package com.example.smartstudytimer.study.summary.controller.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class StudySummaryResponse {
    private Integer currentStreak;
    private Integer maxStreak;
    private Integer totalStudyTime;
    private Integer totalStudyDays;
    private LocalDate lastStudyDate;
}