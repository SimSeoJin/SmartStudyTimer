package com.example.smartstudytimer.study.record.service;

import com.example.smartstudytimer.study.record.controller.dto.StudyRecordRequest;

public interface StudyService {
    String recordStudy(StudyRecordRequest request);
}