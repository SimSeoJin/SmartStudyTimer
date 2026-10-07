package com.example.smartstudytimer.study.daily.Entity;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DailySummaryId implements Serializable {
    private Long member;
    private LocalDate date;
}