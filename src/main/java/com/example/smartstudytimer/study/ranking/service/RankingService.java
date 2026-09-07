package com.example.smartstudytimer.study.ranking.service;

import com.example.smartstudytimer.study.ranking.controller.dto.RankingResponse;

import java.util.List;

public interface RankingService {
    List<RankingResponse> getRanking();

    RankingResponse getMyRanking(Long memberId);
}
