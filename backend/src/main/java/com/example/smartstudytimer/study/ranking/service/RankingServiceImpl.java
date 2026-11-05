package com.example.smartstudytimer.study.ranking.service;

import com.example.smartstudytimer.study.ranking.Entity.RankingView;
import com.example.smartstudytimer.study.ranking.controller.dto.RankingResponse;
import com.example.smartstudytimer.study.ranking.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingRepository rankingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> getRanking() {
        return rankingRepository.findAllByOrderByRankPositionAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RankingResponse getMyRanking(Long memberId) {
        return rankingRepository.findById(memberId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("최근 7일 공부 기록이 없어 랭킹에 포함되지 않습니다."));
    }

    private RankingResponse toResponse(RankingView v) {
        return RankingResponse.builder()
                .memberId(v.getMemberId())
                .nickname(v.getNickname())
                .totalTime(v.getTotalTime())
                .recentAvgTime(v.getRecentAvgTime())
                .rankPosition(v.getRankPosition())
                .tierName(v.getTierName())
                .build();
    }
}
