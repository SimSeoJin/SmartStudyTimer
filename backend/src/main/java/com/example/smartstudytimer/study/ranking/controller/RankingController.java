package com.example.smartstudytimer.study.ranking.controller;

import com.example.smartstudytimer.member.controller.dto.ErrorResponse;
import com.example.smartstudytimer.study.ranking.controller.dto.RankingResponse;
import com.example.smartstudytimer.study.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ranking")
public class RankingController {

    private final RankingService rankingService;

    // 전체 랭킹 (순위 오름차순)
    @GetMapping
    public List<RankingResponse> getRanking() {
        return rankingService.getRanking();
    }

    // 특정 회원의 랭킹 한 줄
    @GetMapping("/{memberId}")
    public ResponseEntity<?> getMyRanking(@PathVariable Long memberId) {
        try {
            return ResponseEntity.ok(rankingService.getMyRanking(memberId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }
}
