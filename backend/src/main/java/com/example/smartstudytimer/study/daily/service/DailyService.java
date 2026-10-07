package com.example.smartstudytimer.study.daily.service;

import com.example.smartstudytimer.study.daily.controller.dto.DailySummaryResponse;
import java.time.LocalDate;
import java.util.List;

public interface DailyService {
    DailySummaryResponse getDailySummary(Long memberId, LocalDate date);

    List<DailySummaryResponse> getMonthlySummary(Long memberId, int year, int month);
}
