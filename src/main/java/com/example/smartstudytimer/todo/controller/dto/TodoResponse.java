package com.example.smartstudytimer.todo.controller.dto;

import com.example.smartstudytimer.todo.entity.TodoCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TodoResponse {
    private Long todoId;
    private String title;
    private TodoCategory category;
    private LocalDate date;
    private Integer timeMinutes;
    private String memo;
    private boolean done;
    private LocalDateTime createdAt;
}
