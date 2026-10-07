package com.example.smartstudytimer.study.daily.controller.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class DailySummaryResponse {
    private LocalDate date;
    private Integer dailyTotal;
    private Integer streak;
}