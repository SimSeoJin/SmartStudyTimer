package com.example.smartstudytimer.todo.entity;

import com.example.smartstudytimer.member.Entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "todo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    private Long todoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private TodoCategory category;

    /** 할 일이 걸린 날짜 */
    @Column(name = "todo_date", nullable = false)
    private LocalDate date;

    /** 목표 소요 시간(분). 선택 항목이라 null 가능 */
    @Column(name = "time_minutes")
    private Integer timeMinutes;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
