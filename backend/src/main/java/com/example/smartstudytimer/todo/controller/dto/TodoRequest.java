package com.example.smartstudytimer.todo.controller.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 할 일 생성/수정 요청.
 * - 생성(POST): title, date 는 필수. done 없으면 false.
 * - 수정(PUT): 전체 교체. 보낸 값으로 덮어씀.
 */
@Data
public class TodoRequest {
    private String title;
    private LocalDate date;
    private String memo;
    private Boolean done;
}
