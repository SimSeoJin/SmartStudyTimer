package com.example.smartstudytimer.study.record.Entity;

import com.example.smartstudytimer.member.Entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_record") 
@Getter 
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class StudyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Integer recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "study_seconds")
    private Integer studySeconds;

    // 기존 행은 BASIC으로 채워진다(ddl-auto=update로 컬럼이 추가될 때 기본값 사용).
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", columnDefinition = "varchar(10) not null default 'BASIC'")
    private StudyMode mode;
}