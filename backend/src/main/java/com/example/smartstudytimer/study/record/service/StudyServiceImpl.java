package com.example.smartstudytimer.study.record.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import com.example.smartstudytimer.study.record.controller.dto.StudyRecordRequest;
import com.example.smartstudytimer.study.record.Entity.StudyRecord;
import com.example.smartstudytimer.study.record.repository.StudyRepository;
import com.example.smartstudytimer.study.daily.Entity.DailySummary;
import com.example.smartstudytimer.study.daily.Entity.DailySummaryId;
import com.example.smartstudytimer.study.daily.repository.DailySummaryRepository;
import com.example.smartstudytimer.study.summary.Entity.StudySummary;
import com.example.smartstudytimer.study.summary.repository.StudySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private final StudyRepository studyRepository;
    private final MemberRepository memberRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final StudySummaryRepository studySummaryRepository; 

    @Override
    @Transactional
    public String recordStudy(StudyRecordRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        StudyRecord record = StudyRecord.builder()
                .member(member)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .studyMinutes(request.getStudyMinutes())
                .build();
        studyRepository.save(record);

        LocalDate targetDate = request.getStartTime().toLocalDate();
        DailySummary daily = dailySummaryRepository.findById(new DailySummaryId(member.getMemberId(), targetDate))
                .orElse(null);

        int currentDailyStreak = 1;
        if (daily == null) {
            DailySummary yesterday = dailySummaryRepository.findById(new DailySummaryId(member.getMemberId(), targetDate.minusDays(1)))
                    .orElse(null);
            if (yesterday != null) {
                currentDailyStreak = yesterday.getStreak() + 1;
            }
            daily = DailySummary.builder()
                    .member(member)
                    .date(targetDate)
                    .dailyTotal(request.getStudyMinutes())
                    .streak(currentDailyStreak)
                    .build();
        } else {
            daily.setDailyTotal(daily.getDailyTotal() + request.getStudyMinutes());
            currentDailyStreak = daily.getStreak();
        }
        dailySummaryRepository.save(daily);

        StudySummary summary = studySummaryRepository.findById(member.getMemberId())
                .orElse(StudySummary.builder()
                        .member(member)
                        .currentStreak(0)
                        .maxStreak(0)
                        .totalStudyTime(0)
                        .totalStudyDays(0)
                        .build());

        summary.setTotalStudyTime(summary.getTotalStudyTime() + request.getStudyMinutes());

        if (summary.getLastStudyDate() == null || !summary.getLastStudyDate().equals(targetDate)) {
            summary.setTotalStudyDays(summary.getTotalStudyDays() + 1);
        }

        summary.setCurrentStreak(currentDailyStreak);
        if (currentDailyStreak > summary.getMaxStreak()) {
            summary.setMaxStreak(currentDailyStreak);
        }

        summary.setLastStudyDate(targetDate);
        summary.setUpdatedAt(LocalDateTime.now()); 

        studySummaryRepository.save(summary);

        return "success";
    }
}