-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
-- Host: localhost    Database: timer_db
-- ------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- 1. Table structure for table `member` (기존 user)
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `member_id` int NOT NULL AUTO_INCREMENT, -- 기존 id
  `login_id` varchar(45) NOT NULL,        -- 기존 username
  `password` varchar(255) NOT NULL,
  `nickname` varchar(45) NOT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `member_id_UNIQUE` (`member_id`),
  UNIQUE KEY `login_id_UNIQUE` (`login_id`),
  UNIQUE KEY `nickname_UNIQUE` (`nickname`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `member` WRITE;
INSERT INTO `member` VALUES (101,'taeik123','1q2w3e4r','ik');
UNLOCK TABLES;

--
-- 2. Table structure for table `study_record` (기존 studylog)
--

DROP TABLE IF EXISTS `study_record`;
CREATE TABLE `study_record` (
  `record_id` int NOT NULL AUTO_INCREMENT, -- 기존 log_id
  `member_id` int NOT NULL,               -- 기존 user_id
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `study_minutes` int NOT NULL,
  PRIMARY KEY (`record_id`),
  -- 중복되었던 인덱스를 하나로 통합 정리
  KEY `fk_study_record_member_idx` (`member_id`), 
  CONSTRAINT `fk_study_record_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `study_record` WRITE;
INSERT INTO `study_record` VALUES (1,101,'2026-01-01 09:00:00','2026-01-01 10:30:00',60),(2,101,'2026-01-01 12:00:00','2026-01-01 12:30:00',30),(3,101,'2026-01-13 17:00:00','2026-01-13 17:20:00',20);
UNLOCK TABLES;

--
-- 3. Table structure for table `dailysummary`
--

DROP TABLE IF EXISTS `dailysummary`;
CREATE TABLE `dailysummary` (
  `member_id` int NOT NULL, -- 기존 user_id
  `date` date NOT NULL,
  `daily_total` int NOT NULL,
  `streak` int NOT NULL,
  PRIMARY KEY (`member_id`,`date`),
  CONSTRAINT `fk_dsummary_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `dailysummary` WRITE;
INSERT INTO `dailysummary` VALUES (101,'2026-01-01',90,1),(101,'2026-01-02',30,2),(101,'2026-01-13',20,1);
UNLOCK TABLES;

--
-- 4. Table structure for table `study_summary` (기존 userstat)
--

DROP TABLE IF EXISTS `study_summary`;
CREATE TABLE `study_summary` (
  `member_id` int NOT NULL, -- 기존 user_id
  `current_streak` int NOT NULL DEFAULT '0',
  `max_streak` int NOT NULL DEFAULT '0',
  `total_study_time` int NOT NULL DEFAULT '0',
  `total_study_days` int NOT NULL DEFAULT '0',
  `last_study_date` date DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `member_id_UNIQUE` (`member_id`),
  CONSTRAINT `fk_study_summary_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `study_summary` WRITE;
INSERT INTO `study_summary` VALUES (101,0,2,110,3,'2026-01-13','2026-02-01 12:00:00');
UNLOCK TABLES;

--
-- 5. Table structure for table `tiermeta`
--

DROP TABLE IF EXISTS `tiermeta`;
CREATE TABLE `tiermeta` (
  `tier_order` int NOT NULL,
  `tier_name` varchar(45) NOT NULL,
  `top_percent` int NOT NULL,
  PRIMARY KEY (`tier_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `tiermeta` WRITE;
INSERT INTO `tiermeta` VALUES (1,'Diamond',4),(2,'Platinum',15),(3,'Gold',45),(4,'Silver',80),(5,'Bronze',100);
UNLOCK TABLES;

--
-- 6. Final view structure for view `ranking`
--

DROP VIEW IF EXISTS `ranking`;
/*!50001 CREATE VIEW `ranking` AS 
select 
    `resulttable`.`member_id` AS `member_id`,
    `resulttable`.`nickname` AS `nickname`,
    `resulttable`.`total_time` AS `total_time`,
    `resulttable`.`recent_avgtime` AS `recent_avgtime`,
    `resulttable`.`rank_position` AS `rank_position`,
    (select `tiermeta`.`tier_name` from `tiermeta` where (`resulttable`.`top_percent_val` <= `tiermeta`.`top_percent`) order by `tiermeta`.`top_percent`,`tiermeta`.`tier_order` limit 1) AS `tier_name` 
from (
    select 
        `aggregateddata`.`member_id` AS `member_id`,
        `aggregateddata`.`nickname` AS `nickname`,
        `aggregateddata`.`total_time` AS `total_time`,
        `aggregateddata`.`recent_avgtime` AS `recent_avgtime`,
        rank() OVER (ORDER BY `aggregateddata`.`total_time` desc )  AS `rank_position`,
        ((rank() OVER (ORDER BY `aggregateddata`.`total_time` desc ) / count(0) OVER () ) * 100) AS `top_percent_val` 
    from (
        select 
            `member`.`member_id` AS `member_id`,
            `member`.`nickname` AS `nickname`,
            sum(ifnull(`dailysummary`.`daily_total`,0)) AS `total_time`,
            round((sum(ifnull(`dailysummary`.`daily_total`,0)) / 7),1) AS `recent_avgtime` 
        from (`member` left join `dailysummary` on(((`member`.`member_id` = `dailysummary`.`member_id`) and (`dailysummary`.`date` > (curdate() - interval 7 day))))) 
        group by `member`.`member_id`,`member`.`nickname`
    ) `aggregateddata`
) `resulttable` */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;