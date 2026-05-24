package com.example.smartstudytimer.study.daily.controller;

import com.example.smartstudytimer.study.daily.controller.dto.DailySummaryResponse;
import com.example.smartstudytimer.study.daily.service.DailyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/study/daily")
public class DailyController {

    private final DailyService dailyService;

    @GetMapping("/{memberId}")
    public DailySummaryResponse getSummary(
            @PathVariable Long memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyService.getDailySummary(memberId, date);
    }
}