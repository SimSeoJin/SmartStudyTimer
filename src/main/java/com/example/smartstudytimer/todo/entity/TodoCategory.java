package com.example.smartstudytimer.todo.entity;

// 앱(Room)의 TodoCategory 와 이름을 맞춘다. DB에는 이름 문자열로 저장(@Enumerated(STRING)).
public enum TodoCategory {
    GENERAL, EXAM, CERT
}
