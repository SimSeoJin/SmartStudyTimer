package com.example.smartstudytimer.study.summary.Entity;

import com.example.smartstudytimer.member.Entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_summary")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySummary {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "current_streak")
    private Integer currentStreak;

    @Column(name = "max_streak")
    private Integer maxStreak;

    @Column(name = "total_study_time")
    private Integer totalStudyTime;

    @Column(name = "total_study_days")
    private Integer totalStudyDays;

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}