package com.example.smartstudytimer.study.daily.service;

import com.example.smartstudytimer.study.daily.Entity.DailySummary;
import com.example.smartstudytimer.study.daily.Entity.DailySummaryId;
import com.example.smartstudytimer.study.daily.controller.dto.DailySummaryResponse;
import com.example.smartstudytimer.study.daily.repository.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyServiceImpl implements DailyService {

    private final DailySummaryRepository dailySummaryRepository;

    @Override
    @Transactional(readOnly = true)
    public DailySummaryResponse getDailySummary(Long memberId, LocalDate date) {
        DailySummary summary = dailySummaryRepository.findById(new DailySummaryId(memberId, date))
                .orElseThrow(() -> new RuntimeException("No summary data for the selected date."));

        return toResponse(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailySummaryResponse> getMonthlySummary(Long memberId, int year, int month) {
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("잘못된 연/월입니다. (year, month=1~12)");
        }

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // 기록이 있는 날짜만 반환한다. 기록이 없는 날은 응답에 포함하지 않는다.
        return dailySummaryRepository.findMonthly(memberId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    private DailySummaryResponse toResponse(DailySummary summary) {
        return DailySummaryResponse.builder()
                .date(summary.getDate())
                .dailyTotal(summary.getDailyTotal())
                .streak(summary.getStreak())
                .build();
    }
}
