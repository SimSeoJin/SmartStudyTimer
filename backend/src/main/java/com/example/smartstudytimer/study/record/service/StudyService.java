package com.example.smartstudytimer.study.record.service;

import com.example.smartstudytimer.study.record.controller.dto.StudyRecordRequest;
import com.example.smartstudytimer.study.record.controller.dto.StudyRecordResponse;

import java.util.List;

public interface StudyService {
    String recordStudy(StudyRecordRequest request);

    List<StudyRecordResponse> getRecords(Long memberId);
}
