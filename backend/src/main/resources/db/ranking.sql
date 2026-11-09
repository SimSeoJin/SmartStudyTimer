-- 랭킹·티어용 테이블과 뷰. MySQL에서 앱이 시작될 때마다 실행된다(application.properties의 spring.sql.init).
-- Hibernate가 테이블을 만든 뒤에 실행되도록 defer-datasource-initialization=true를 함께 쓴다.
-- 여러 번 실행해도 결과가 같아야 한다.

CREATE TABLE IF NOT EXISTS tier_meta (
  tier_order int NOT NULL,
  tier_name varchar(45) NOT NULL,
  top_percent int NOT NULL,
  PRIMARY KEY (tier_order)
);

INSERT IGNORE INTO tier_meta (tier_order, tier_name, top_percent) VALUES
  (1, 'Diamond', 4), (2, 'Platinum', 15), (3, 'Gold', 45), (4, 'Silver', 80), (5, 'Bronze', 100);

-- 최근 7일 순공 시간(daily_summary.pure_total, 초) 기준 순위. 순공 기록이 없는 회원은 제외한다.
-- 일반 타이머(BASIC) 시간은 daily_total에만 들어가고 랭킹에는 반영되지 않는다.
CREATE OR REPLACE VIEW ranking AS
SELECT r.member_id,
       r.nickname,
       r.total_time,
       r.recent_avgtime,
       r.rank_position,
       (SELECT t.tier_name FROM tier_meta t
         WHERE r.top_percent_val <= t.top_percent
         ORDER BY t.top_percent, t.tier_order LIMIT 1) AS tier_name
FROM (
  SELECT a.member_id,
         a.nickname,
         a.total_time,
         a.recent_avgtime,
         RANK() OVER (ORDER BY a.total_time DESC) AS rank_position,
         RANK() OVER (ORDER BY a.total_time DESC) / COUNT(*) OVER () * 100 AS top_percent_val
  FROM (
    SELECT m.member_id,
           m.nickname,
           SUM(IFNULL(d.pure_total, 0)) AS total_time,
           ROUND(SUM(IFNULL(d.pure_total, 0)) / 7, 1) AS recent_avgtime
    FROM member m
    LEFT JOIN daily_summary d
      ON m.member_id = d.member_id AND d.date > CURDATE() - INTERVAL 7 DAY
    GROUP BY m.member_id, m.nickname
    HAVING total_time > 0
  ) a
) r;
