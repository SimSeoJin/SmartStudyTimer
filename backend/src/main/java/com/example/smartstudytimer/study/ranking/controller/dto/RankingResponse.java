package com.example.smartstudytimer.study.ranking.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingResponse {
    private Long memberId;
    private String nickname;
    private Long totalTime;
    private Double recentAvgTime;
    private Long rankPosition;
    private String tierName;
}
