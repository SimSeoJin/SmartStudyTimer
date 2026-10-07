package com.example.smartstudytimer.dday.entity;

import com.example.smartstudytimer.member.Entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 회원당 1개의 D-Day. member_id 가 곧 PK (1:1).
 */
@Entity
@Table(name = "dday")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DDay {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "label", nullable = false, length = 45)
    private String label;
}
