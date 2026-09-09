package com.example.smartstudytimer.todo.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import com.example.smartstudytimer.todo.controller.dto.TodoRequest;
import com.example.smartstudytimer.todo.controller.dto.TodoResponse;
import com.example.smartstudytimer.todo.entity.Todo;
import com.example.smartstudytimer.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TodoResponse> getTodos(Long memberId, LocalDate date) {
        List<Todo> todos = (date != null)
                ? todoRepository.findByMember_MemberIdAndDateOrderByDoneAscCreatedAtAsc(memberId, date)
                : todoRepository.findByMember_MemberIdOrderByDateAscCreatedAtAsc(memberId);
        return todos.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public TodoResponse createTodo(Long memberId, TodoRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("할 일 제목은 필수입니다.");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("할 일 날짜는 필수입니다.");
        }

        Todo todo = Todo.builder()
                .member(member)
                .title(request.getTitle().trim())
                .date(request.getDate())
                .memo(request.getMemo() != null ? request.getMemo() : "")
                .done(Boolean.TRUE.equals(request.getDone()))
                .build();

        return toResponse(todoRepository.save(todo));
    }

    /**
     * 전체 교체(PUT). 앱은 수정 시 할 일 전체를 보내므로 받은 값으로 덮어쓴다.
     * 단 title/date 가 비어 오면(방어적으로) 기존 값을 유지한다.
     */
    @Override
    @Transactional
    public TodoResponse updateTodo(Long memberId, Long todoId, TodoRequest request) {
        Todo todo = findOwned(memberId, todoId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            todo.setTitle(request.getTitle().trim());
        }
        if (request.getDate() != null) {
            todo.setDate(request.getDate());
        }
        todo.setMemo(request.getMemo() != null ? request.getMemo() : "");
        todo.setDone(Boolean.TRUE.equals(request.getDone()));

        return toResponse(todo); // dirty checking 으로 flush 시 UPDATE
    }

    @Override
    @Transactional
    public TodoResponse setDone(Long memberId, Long todoId, boolean done) {
        Todo todo = findOwned(memberId, todoId);
        todo.setDone(done);
        return toResponse(todo);
    }

    @Override
    @Transactional
    public void deleteTodo(Long memberId, Long todoId) {
        Todo todo = findOwned(memberId, todoId);
        todoRepository.delete(todo);
    }

    private Todo findOwned(Long memberId, Long todoId) {
        return todoRepository.findByTodoIdAndMember_MemberId(todoId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 할 일을 찾을 수 없습니다."));
    }

    private TodoResponse toResponse(Todo todo) {
        return TodoResponse.builder()
                .todoId(todo.getTodoId())
                .title(todo.getTitle())
                .date(todo.getDate())
                .memo(todo.getMemo())
                .done(todo.isDone())
                .createdAt(todo.getCreatedAt())
                .build();
    }
}
