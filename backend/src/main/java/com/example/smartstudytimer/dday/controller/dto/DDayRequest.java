package com.example.smartstudytimer.dday.controller.dto;

import lombok.Data;

import java.time.LocalDate;

/** D-Day 설정/수정 요청. 둘 다 필수. */
@Data
public class DDayRequest {
    private LocalDate targetDate;
    private String label;
}
