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

    @Column(name = "daily_total")
    private Integer dailyTotal;

    @Column(name = "streak")
    private Integer streak;
}