package com.example.smartstudytimer.todo.controller;

import com.example.smartstudytimer.member.controller.dto.ErrorResponse;
import com.example.smartstudytimer.todo.controller.dto.TodoRequest;
import com.example.smartstudytimer.todo.controller.dto.TodoResponse;
import com.example.smartstudytimer.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/todos")
public class TodoController {

    private final TodoService todoService;

    // 목록: ?date=2026-09-07 이면 그 날, 없으면 전체
    @GetMapping("/{memberId}")
    public List<TodoResponse> getTodos(
            @PathVariable Long memberId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return todoService.getTodos(memberId, date);
    }

    @PostMapping("/{memberId}")
    public ResponseEntity<?> createTodo(@PathVariable Long memberId,
                                       @RequestBody TodoRequest request) {
        try {
            TodoResponse created = todoService.createTodo(memberId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{memberId}/{todoId}")
    public ResponseEntity<?> updateTodo(@PathVariable Long memberId,
                                       @PathVariable Long todoId,
                                       @RequestBody TodoRequest request) {
        try {
            return ResponseEntity.ok(todoService.updateTodo(memberId, todoId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }

    // 완료 체크만 토글: ?done=true
    @PatchMapping("/{memberId}/{todoId}/done")
    public ResponseEntity<?> setDone(@PathVariable Long memberId,
                                     @PathVariable Long todoId,
                                     @RequestParam boolean done) {
        try {
            return ResponseEntity.ok(todoService.setDone(memberId, todoId, done));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{memberId}/{todoId}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long memberId,
                                        @PathVariable Long todoId) {
        try {
            todoService.deleteTodo(memberId, todoId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }
}
