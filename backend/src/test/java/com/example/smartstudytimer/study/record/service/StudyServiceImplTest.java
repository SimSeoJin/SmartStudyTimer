package com.example.smartstudytimer.study.record.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import com.example.smartstudytimer.study.daily.Entity.DailySummary;
import com.example.smartstudytimer.study.daily.repository.DailySummaryRepository;
import com.example.smartstudytimer.study.record.Entity.StudyMode;
import com.example.smartstudytimer.study.record.Entity.StudyRecord;
import com.example.smartstudytimer.study.record.controller.dto.StudyRecordRequest;
import com.example.smartstudytimer.study.record.repository.StudyRepository;
import com.example.smartstudytimer.study.summary.repository.StudySummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyServiceImplTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 9, 9, 0);
    private static final LocalDateTime END = START.plusMinutes(30); // 1800초

    private final StudyRepository studyRepository = mock(StudyRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final DailySummaryRepository dailySummaryRepository = mock(DailySummaryRepository.class);
    private final StudySummaryRepository studySummaryRepository = mock(StudySummaryRepository.class);
    private final StudyServiceImpl service = new StudyServiceImpl(
            studyRepository, memberRepository, dailySummaryRepository, studySummaryRepository);

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId(1L).name("tester").build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(dailySummaryRepository.findById(any())).thenReturn(Optional.empty());
        when(studySummaryRepository.findById(1L)).thenReturn(Optional.empty());
    }

    private StudyRecordRequest request(StudyMode mode) {
        return StudyRecordRequest.builder().memberId(1L).startTime(START).endTime(END).mode(mode).build();
    }

    private DailySummary savedDaily() {
        ArgumentCaptor<DailySummary> captor = ArgumentCaptor.forClass(DailySummary.class);
        verify(dailySummaryRepository).save(captor.capture());
        return captor.getValue();
    }

    private StudyRecord savedRecord() {
        ArgumentCaptor<StudyRecord> captor = ArgumentCaptor.forClass(StudyRecord.class);
        verify(studyRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void pureRecordCountsTowardRanking() {
        service.recordStudy(request(StudyMode.PURE));

        assertEquals(StudyMode.PURE, savedRecord().getMode());
        assertEquals(1800, savedDaily().getDailyTotal());
        assertEquals(1800, savedDaily().getPureTotal());
    }

    @Test
    void basicRecordIsKeptButExcludedFromRanking() {
        service.recordStudy(request(StudyMode.BASIC));

        assertEquals(StudyMode.BASIC, savedRecord().getMode());
        assertEquals(1800, savedDaily().getDailyTotal());
        assertEquals(0, savedDaily().getPureTotal());
    }

    @Test
    void missingModeIsTreatedAsBasic() {
        service.recordStudy(request(null));

        assertEquals(StudyMode.BASIC, savedRecord().getMode());
        assertEquals(0, savedDaily().getPureTotal());
    }

    @Test
    void pureTimeAccumulatesOnExistingDay() {
        DailySummary existing = DailySummary.builder()
                .member(member).date(LocalDate.of(2026, 11, 9))
                .dailyTotal(600).pureTotal(300).streak(2).build();
        when(dailySummaryRepository.findById(any())).thenReturn(Optional.of(existing));

        service.recordStudy(request(StudyMode.PURE));

        assertEquals(2400, savedDaily().getDailyTotal());
        assertEquals(2100, savedDaily().getPureTotal());
    }
}
