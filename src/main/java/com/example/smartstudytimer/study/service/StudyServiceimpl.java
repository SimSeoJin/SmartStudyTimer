package com.example.smartstudytimer.study.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import com.example.smartstudytimer.study.dto.StudyRecordRequest;
import com.example.smartstudytimer.study.Entity.StudyRecord;
import com.example.smartstudytimer.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyServiceimpl implements StudyService {

    private final StudyRepository studyRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public String recordStudy(StudyRecordRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("Failed to find member ID: " + request.getMemberId()));

        StudyRecord record = StudyRecord.builder()
                .member(member)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .studyMinutes(request.getStudyMinutes())
                .build();

        studyRepository.save(record);
        
        return "success";
    }
}