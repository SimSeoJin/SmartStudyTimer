package com.example.smartstudytimer.study.summary.service;

import com.example.smartstudytimer.study.summary.Entity.StudySummary;
import com.example.smartstudytimer.study.summary.controller.dto.StudySummaryResponse;
import com.example.smartstudytimer.study.summary.repository.StudySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final StudySummaryRepository studySummaryRepository;

    @Override
    @Transactional(readOnly = true)
    public StudySummaryResponse getSummary(Long memberId) {
        StudySummary summary = studySummaryRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("No data for the selected member."));

        return StudySummaryResponse.builder()
                .currentStreak(summary.getCurrentStreak())
                .maxStreak(summary.getMaxStreak())
                .totalStudyTime(summary.getTotalStudyTime())
                .totalStudyDays(summary.getTotalStudyDays())
                .lastStudyDate(summary.getLastStudyDate())
                .build();
    }
}