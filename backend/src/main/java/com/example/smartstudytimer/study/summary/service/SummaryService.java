package com.example.smartstudytimer.study.summary.service;

import com.example.smartstudytimer.study.summary.controller.dto.StudySummaryResponse;

public interface SummaryService {
    StudySummaryResponse getSummary(Long memberId);
}