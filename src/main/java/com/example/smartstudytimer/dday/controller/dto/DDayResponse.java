package com.example.smartstudytimer.dday.controller.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DDayResponse {
    private LocalDate targetDate;
    private String label;
    /** 오늘 기준 남은 일수. 양수=미래, 0=오늘, 음수=지남 */
    private long dDayCount;
}
