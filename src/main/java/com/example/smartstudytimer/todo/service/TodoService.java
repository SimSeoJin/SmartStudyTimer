package com.example.smartstudytimer.todo.service;

import com.example.smartstudytimer.todo.controller.dto.TodoRequest;
import com.example.smartstudytimer.todo.controller.dto.TodoResponse;

import java.time.LocalDate;
import java.util.List;

public interface TodoService {

    List<TodoResponse> getTodos(Long memberId, LocalDate date);

    TodoResponse createTodo(Long memberId, TodoRequest request);

    TodoResponse updateTodo(Long memberId, Long todoId, TodoRequest request);

    TodoResponse setDone(Long memberId, Long todoId, boolean done);

    void deleteTodo(Long memberId, Long todoId);
}
