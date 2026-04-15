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
-- Table structure for table `dailysummary`
--

DROP TABLE IF EXISTS `dailysummary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dailysummary` (
  `user_id` int NOT NULL,
  `date` date NOT NULL,
  `daily_total` int NOT NULL,
  `streak` int NOT NULL,
  PRIMARY KEY (`user_id`,`date`),
  CONSTRAINT `fk_dsummary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dailysummary`
--

LOCK TABLES `dailysummary` WRITE;
/*!40000 ALTER TABLE `dailysummary` DISABLE KEYS */;
INSERT INTO `dailysummary` VALUES (101,'2026-01-01',90,1),(101,'2026-01-02',30,2),(101,'2026-01-13',20,1);
/*!40000 ALTER TABLE `dailysummary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `ranking`
--

DROP TABLE IF EXISTS `ranking`;
/*!50001 DROP VIEW IF EXISTS `ranking`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ranking` AS SELECT 
 1 AS `user_id`,
 1 AS `nickname`,
 1 AS `total_time`,
 1 AS `recent_avgtime`,
 1 AS `rank_position`,
 1 AS `tier_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `studylog`
--

DROP TABLE IF EXISTS `studylog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `studylog` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `study_minutes` int NOT NULL,
  PRIMARY KEY (`log_id`),
  KEY `user_id_idx` (`user_id`),
  KEY `fk_studylog_user_idx` (`user_id`),
  CONSTRAINT `fk_studylog_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `studylog`
--

LOCK TABLES `studylog` WRITE;
/*!40000 ALTER TABLE `studylog` DISABLE KEYS */;
INSERT INTO `studylog` VALUES (1,101,'2026-01-01 09:00:00','2026-01-01 10:30:00',60),(2,101,'2026-01-01 12:00:00','2026-01-01 12:30:00',30),(3,101,'2026-01-13 17:00:00','2026-01-13 17:20:00',20);
/*!40000 ALTER TABLE `studylog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tiermeta`
--

DROP TABLE IF EXISTS `tiermeta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tiermeta` (
  `tier_order` int NOT NULL,
  `tier_name` varchar(45) NOT NULL,
  `top_percent` int NOT NULL,
  PRIMARY KEY (`tier_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tiermeta`
--

LOCK TABLES `tiermeta` WRITE;
/*!40000 ALTER TABLE `tiermeta` DISABLE KEYS */;
INSERT INTO `tiermeta` VALUES (1,'Diamond',4),(2,'Platinum',15),(3,'Gold',45),(4,'Silver',80),(5,'Bronze',100);
/*!40000 ALTER TABLE `tiermeta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(45) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nickname` varchar(45) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `username_UNIQUE` (`username`),
  UNIQUE KEY `nickname_UNIQUE` (`nickname`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (101,'taeik123','1q2w3e4r','ik');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `userstat`
--

DROP TABLE IF EXISTS `userstat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userstat` (
  `user_id` int NOT NULL,
  `current_streak` int NOT NULL DEFAULT '0',
  `max_streak` int NOT NULL DEFAULT '0',
  `total_study_time` int NOT NULL DEFAULT '0',
  `total_study_days` int NOT NULL DEFAULT '0',
  `last_study_date` date DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_id_UNIQUE` (`user_id`),
  CONSTRAINT `fk_userstat_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `userstat`
--

LOCK TABLES `userstat` WRITE;
/*!40000 ALTER TABLE `userstat` DISABLE KEYS */;
INSERT INTO `userstat` VALUES (101,0,2,110,3,'2026-01-13','2026-02-01 12:00:00');
/*!40000 ALTER TABLE `userstat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `ranking`
--

/*!50001 DROP VIEW IF EXISTS `ranking`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ranking` AS select `resulttable`.`user_id` AS `user_id`,`resulttable`.`nickname` AS `nickname`,`resulttable`.`total_time` AS `total_time`,`resulttable`.`recent_avgtime` AS `recent_avgtime`,`resulttable`.`rank_position` AS `rank_position`,(select `tiermeta`.`tier_name` from `tiermeta` where (`resulttable`.`top_percent_val` <= `tiermeta`.`top_percent`) order by `tiermeta`.`top_percent`,`tiermeta`.`tier_order` limit 1) AS `tier_name` from (select `aggregateddata`.`user_id` AS `user_id`,`aggregateddata`.`nickname` AS `nickname`,`aggregateddata`.`total_time` AS `total_time`,`aggregateddata`.`recent_avgtime` AS `recent_avgtime`,rank() OVER (ORDER BY `aggregateddata`.`total_time` desc )  AS `rank_position`,((rank() OVER (ORDER BY `aggregateddata`.`total_time` desc )  / count(0) OVER () ) * 100) AS `top_percent_val` from (select `user`.`id` AS `user_id`,`user`.`nickname` AS `nickname`,sum(ifnull(`dailysummary`.`daily_total`,0)) AS `total_time`,round((sum(ifnull(`dailysummary`.`daily_total`,0)) / 7),1) AS `recent_avgtime` from (`user` left join `dailysummary` on(((`user`.`id` = `dailysummary`.`user_id`) and (`dailysummary`.`date` > (curdate() - interval 7 day))))) group by `user`.`id`,`user`.`nickname`) `aggregateddata`) `resulttable` */;
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

-- Dump completed on 2026-04-12 13:42:50
