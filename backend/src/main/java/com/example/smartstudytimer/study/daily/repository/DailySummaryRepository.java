package com.example.smartstudytimer.study.daily.repository;

import com.example.smartstudytimer.study.daily.Entity.DailySummary;
import com.example.smartstudytimer.study.daily.Entity.DailySummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, DailySummaryId> {

    // 특정 회원의 [start, end] 기간 일별 요약을 날짜순으로 조회 (캘린더 월별 조회용).
    @Query("select d from DailySummary d " +
            "where d.member.memberId = :memberId and d.date between :start and :end " +
            "order by d.date")
    List<DailySummary> findMonthly(@Param("memberId") Long memberId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);
}
