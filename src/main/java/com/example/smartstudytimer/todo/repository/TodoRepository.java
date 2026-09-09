package com.example.smartstudytimer.todo.repository;

import com.example.smartstudytimer.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 특정 회원의 특정 날짜 할 일 (미완료 먼저, 생성순)
    List<Todo> findByMember_MemberIdAndDateOrderByDoneAscCreatedAtAsc(Long memberId, LocalDate date);

    // 특정 회원의 전체 할 일 (날짜순, 생성순)
    List<Todo> findByMember_MemberIdOrderByDateAscCreatedAtAsc(Long memberId);

    // 소유권 확인용: 이 회원의 이 할 일
    Optional<Todo> findByTodoIdAndMember_MemberId(Long todoId, Long memberId);
}
