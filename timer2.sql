-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: timer_db
-- ------------------------------------------------------
-- Server version	8.0.45

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
-- Table structure for table `daily_summary`
--

DROP TABLE IF EXISTS `daily_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_summary` (
  `member_id` int NOT NULL,
  `date` date NOT NULL,
  `daily_total` int NOT NULL,
  `streak` int NOT NULL,
  PRIMARY KEY (`member_id`,`date`),
  CONSTRAINT `fk_dsummary_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `daily_summary`
--

LOCK TABLES `daily_summary` WRITE;
/*!40000 ALTER TABLE `daily_summary` DISABLE KEYS */;
INSERT INTO `daily_summary` VALUES (101,'2026-01-01',90,1),(101,'2026-01-13',20,1);
/*!40000 ALTER TABLE `daily_summary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `member_id` int NOT NULL AUTO_INCREMENT,
  `login_id` varchar(45) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nickname` varchar(45) NOT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `member_id_UNIQUE` (`member_id`),
  UNIQUE KEY `login_id_UNIQUE` (`login_id`),
  UNIQUE KEY `nickname_UNIQUE` (`nickname`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member`
--

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member` DISABLE KEYS */;
INSERT INTO `member` VALUES (101,'taeik123','1q2w3e4r','ik');
/*!40000 ALTER TABLE `member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `ranking`
--

DROP TABLE IF EXISTS `ranking`;
/*!50001 DROP VIEW IF EXISTS `ranking`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ranking` AS SELECT 
 1 AS `member_id`,
 1 AS `nickname`,
 1 AS `total_time`,
 1 AS `recent_avgtime`,
 1 AS `rank_position`,
 1 AS `tier_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `study_record`
--

DROP TABLE IF EXISTS `study_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_record` (
  `record_id` int NOT NULL AUTO_INCREMENT,
  `member_id` int NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `study_minutes` int NOT NULL,
  PRIMARY KEY (`record_id`),
  KEY `fk_study_record_member_idx` (`member_id`),
  CONSTRAINT `fk_study_record_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_record`
--

LOCK TABLES `study_record` WRITE;
/*!40000 ALTER TABLE `study_record` DISABLE KEYS */;
INSERT INTO `study_record` VALUES (1,101,'2026-01-01 09:00:00','2026-01-01 10:30:00',60),(2,101,'2026-01-01 12:00:00','2026-01-01 12:30:00',30),(3,101,'2026-01-13 17:00:00','2026-01-13 17:20:00',20);
/*!40000 ALTER TABLE `study_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `study_summary`
--

DROP TABLE IF EXISTS `study_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_summary` (
  `member_id` int NOT NULL,
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_summary`
--

LOCK TABLES `study_summary` WRITE;
/*!40000 ALTER TABLE `study_summary` DISABLE KEYS */;
INSERT INTO `study_summary` VALUES (101,0,1,110,2,'2026-01-13','2026-02-01 12:00:00');
/*!40000 ALTER TABLE `study_summary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tier_meta`
--

DROP TABLE IF EXISTS `tier_meta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tier_meta` (
  `tier_order` int NOT NULL,
  `tier_name` varchar(45) NOT NULL,
  `top_percent` int NOT NULL,
  PRIMARY KEY (`tier_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tier_meta`
--

LOCK TABLES `tier_meta` WRITE;
/*!40000 ALTER TABLE `tier_meta` DISABLE KEYS */;
INSERT INTO `tier_meta` VALUES (1,'Diamond',4),(2,'Platinum',15),(3,'Gold',45),(4,'Silver',80),(5,'Bronze',100);
/*!40000 ALTER TABLE `tier_meta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `ranking`
--

/*!50001 DROP VIEW IF EXISTS `ranking`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb3 */;
/*!50001 SET character_set_results     = utf8mb3 */;
/*!50001 SET collation_connection      = utf8mb3_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ranking` AS select `resulttable`.`member_id` AS `member_id`,`resulttable`.`nickname` AS `nickname`,`resulttable`.`total_time` AS `total_time`,`resulttable`.`recent_avgtime` AS `recent_avgtime`,`resulttable`.`rank_position` AS `rank_position`,(select `tier_meta`.`tier_name` from `tier_meta` where (`resulttable`.`top_percent_val` <= `tier_meta`.`top_percent`) order by `tier_meta`.`top_percent`,`tier_meta`.`tier_order` limit 1) AS `tier_name` from (select `aggregateddata`.`member_id` AS `member_id`,`aggregateddata`.`nickname` AS `nickname`,`aggregateddata`.`total_time` AS `total_time`,`aggregateddata`.`recent_avgtime` AS `recent_avgtime`,rank() OVER (ORDER BY `aggregateddata`.`total_time` desc )  AS `rank_position`,((rank() OVER (ORDER BY `aggregateddata`.`total_time` desc )  / count(0) OVER () ) * 100) AS `top_percent_val` from (select `member`.`member_id` AS `member_id`,`member`.`nickname` AS `nickname`,sum(ifnull(`daily_summary`.`daily_total`,0)) AS `total_time`,round((sum(ifnull(`daily_summary`.`daily_total`,0)) / 7),1) AS `recent_avgtime` from (`member` left join `daily_summary` on(((`member`.`member_id` = `daily_summary`.`member_id`) and (`daily_summary`.`date` > (curdate() - interval 7 day))))) group by `member`.`member_id`,`member`.`nickname`) `aggregateddata`) `resulttable` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-07 11:13:17
