package com.example.smartstudytimer.study.daily.controller;

import com.example.smartstudytimer.member.controller.dto.ErrorResponse;
import com.example.smartstudytimer.study.daily.controller.dto.DailySummaryResponse;
import com.example.smartstudytimer.study.daily.service.DailyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // 캘린더 화면: 한 달치 일별 요약을 한 번에 조회. 예) GET /study/daily/101/monthly?year=2026&month=1
    @GetMapping("/{memberId}/monthly")
    public ResponseEntity<?> getMonthlySummary(
            @PathVariable Long memberId,
            @RequestParam int year,
            @RequestParam int month) {
        try {
            return ResponseEntity.ok(dailyService.getMonthlySummary(memberId, year, month));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }
}
