package com.example.smartstudytimer.study.daily.service;

import com.example.smartstudytimer.study.daily.controller.dto.DailySummaryResponse;
import java.time.LocalDate;

public interface DailyService {
    DailySummaryResponse getDailySummary(Long memberId, LocalDate date);
}