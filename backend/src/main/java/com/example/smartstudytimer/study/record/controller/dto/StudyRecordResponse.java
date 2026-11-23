package com.example.smartstudytimer.study.record.controller.dto;

import com.example.smartstudytimer.study.record.Entity.StudyMode;
import com.example.smartstudytimer.study.record.Entity.StudyRecord;

import java.time.LocalDateTime;

/** 새 기기에서 로그인했을 때 앱이 기록을 내려받는 데 쓴다. */
public record StudyRecordResponse(Integer recordId, LocalDateTime startTime, LocalDateTime endTime, StudyMode mode) {
    public static StudyRecordResponse from(StudyRecord r) {
        return new StudyRecordResponse(r.getRecordId(), r.getStartTime(), r.getEndTime(),
                r.getMode() != null ? r.getMode() : StudyMode.BASIC);
    }
}
