package com.example.smartstudytimer.study.ranking.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

/**
 * DB의 `ranking` 뷰를 읽기 전용으로 매핑한다.
 *
 * - {@code @Subselect}: 이 엔티티를 "뷰(파생 쿼리)"로 취급 → ddl-auto=update 여도 Hibernate가
 *   이 이름으로 테이블을 만들지 않는다.
 * - {@code @Immutable}: 변경 감지/INSERT/UPDATE 대상에서 제외 (조회만).
 * - {@code @Synchronize}: 이 뷰가 참조하는 테이블. 같은 트랜잭션에서 그 테이블을 수정했다면
 *   Hibernate가 뷰 조회 전에 flush 하도록 알려준다.
 */
@Entity
@Immutable
@Subselect(
        "select member_id, nickname, total_time, recent_avgtime, rank_position, tier_name from ranking"
)
@Synchronize({"member", "daily_summary", "tier_meta"})
@Getter
public class RankingView {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "nickname")
    private String nickname;

    /** 최근 7일 공부시간 합계(분) */
    @Column(name = "total_time")
    private Long totalTime;

    /** 최근 7일 일평균(분, 소수 1자리) */
    @Column(name = "recent_avgtime")
    private Double recentAvgTime;

    /** 전체 순위 (1등부터) */
    @Column(name = "rank_position")
    private Long rankPosition;

    /** tier_meta 기준 티어명 (Diamond/Platinum/...) */
    @Column(name = "tier_name")
    private String tierName;
}
