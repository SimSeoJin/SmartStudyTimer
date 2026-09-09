package com.example.smartstudytimer.study.ranking.repository;

import com.example.smartstudytimer.study.ranking.Entity.RankingView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<RankingView, Long> {

    // 순위 오름차순(1등부터) 전체 조회.
    List<RankingView> findAllByOrderByRankPositionAsc();
}
