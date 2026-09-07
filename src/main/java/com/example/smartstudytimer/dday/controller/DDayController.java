package com.example.smartstudytimer.dday.controller;

import com.example.smartstudytimer.dday.controller.dto.DDayRequest;
import com.example.smartstudytimer.dday.controller.dto.DDayResponse;
import com.example.smartstudytimer.dday.service.DDayService;
import com.example.smartstudytimer.member.controller.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dday")
public class DDayController {

    private final DDayService ddayService;

    @GetMapping("/{memberId}")
    public ResponseEntity<?> getDDay(@PathVariable Long memberId) {
        try {
            return ResponseEntity.ok(ddayService.getDDay(memberId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }

    // 설정/수정 통합 (upsert)
    @PutMapping("/{memberId}")
    public ResponseEntity<?> setDDay(@PathVariable Long memberId,
                                    @RequestBody DDayRequest request) {
        try {
            return ResponseEntity.ok(ddayService.setDDay(memberId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteDDay(@PathVariable Long memberId) {
        ddayService.deleteDDay(memberId);
        return ResponseEntity.noContent().build();
    }
}
