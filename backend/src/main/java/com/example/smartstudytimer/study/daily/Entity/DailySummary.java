package com.example.smartstudytimer.study.daily.Entity;

import com.example.smartstudytimer.member.Entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_summary")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(DailySummaryId.class)
public class DailySummary {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Id
    @Column(name = "date")
    private LocalDate date;

    /** 모든 모드의 공부 시간 합계(초). 개인 통계용 */
    @Column(name = "daily_total")
    private Integer dailyTotal;

    /** 순공 모드 시간 합계(초). 랭킹·티어는 이 값으로 계산한다(db/ranking.sql) */
    @Column(name = "pure_total", columnDefinition = "int not null default 0")
    private Integer pureTotal;

    @Column(name = "streak")
    private Integer streak;
}