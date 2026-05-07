package com.example.smartstudytimer.study.daily.service;

import com.example.smartstudytimer.study.daily.Entity.DailySummary;
import com.example.smartstudytimer.study.daily.Entity.DailySummaryId;
import com.example.smartstudytimer.study.daily.controller.dto.DailySummaryResponse;
import com.example.smartstudytimer.study.daily.repository.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyServiceImpl implements DailyService {

    private final DailySummaryRepository dailySummaryRepository;

    @Override
    @Transactional(readOnly = true)
    public DailySummaryResponse getDailySummary(Long memberId, LocalDate date) {
        DailySummary summary = dailySummaryRepository.findById(new DailySummaryId(memberId, date))
                .orElseThrow(() -> new RuntimeException("No summary data for the selected date."));

        return DailySummaryResponse.builder()
                .date(summary.getDate())
                .dailyTotal(summary.getDailyTotal())
                .streak(summary.getStreak())
                .build();
    }
}