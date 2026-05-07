package com.example.smartstudytimer.study.summary.controller;

import com.example.smartstudytimer.study.summary.controller.dto.StudySummaryResponse;
import com.example.smartstudytimer.study.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/study/summary")
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/{memberId}")
    public StudySummaryResponse getSummary(@PathVariable Long memberId) {
        return summaryService.getSummary(memberId);
    }
}