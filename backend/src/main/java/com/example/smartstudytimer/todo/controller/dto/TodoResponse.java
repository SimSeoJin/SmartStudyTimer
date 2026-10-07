package com.example.smartstudytimer.todo.controller.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TodoResponse {
    private Long todoId;
    private String title;
    private LocalDate date;
    private String memo;
    private boolean done;
    private LocalDateTime createdAt;
}
