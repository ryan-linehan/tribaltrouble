-- ============================================================
-- Tribal Trouble Database Initialization for Docker
-- Combines: initmysql.sql + all migrations + user setup
-- This script runs automatically when the MySQL container
-- starts for the first time.
-- ============================================================

-- Create the matchmaker user (password set via MYSQL_PASSWORD env var)
-- The user is created by MySQL's MYSQL_USER env var in docker-compose,
-- but we need to grant it full access to the oddlabs database.
-- Docker's MySQL image auto-creates the user with MYSQL_USER/MYSQL_PASSWORD
-- and grants all privileges on MYSQL_DATABASE, so this file only needs
-- the schema setup.

-- ============================================================
-- Base Schema (from initmysql.sql)
-- ============================================================

-- Table: connections
CREATE TABLE `connections` (
  `game_id` int DEFAULT NULL,
  `nick1` varchar(128) DEFAULT NULL,
  `nick2` varchar(128) DEFAULT NULL,
  `priority` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: deleted_profiles
CREATE TABLE `deleted_profiles` (
  `nick` varchar(128) DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `wins` int DEFAULT NULL,
  `losses` int DEFAULT NULL,
  `invalid` int DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  `reg_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: game_reports
CREATE TABLE `game_reports` (
  `game_id` int DEFAULT NULL,
  `tick` int DEFAULT NULL,
  `team1` int DEFAULT NULL,
  `team2` int DEFAULT NULL,
  `team3` int DEFAULT NULL,
  `team4` int DEFAULT NULL,
  `team5` int DEFAULT NULL,
  `team6` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: games (post-migration schema - without denormalized player columns)
CREATE TABLE `games` (
  `time_create` datetime DEFAULT (curdate()),
  `name` varchar(128) DEFAULT NULL,
  `rated` varchar(1) DEFAULT NULL,
  `speed` int DEFAULT NULL,
  `size` int DEFAULT NULL,
  `hills` int DEFAULT NULL,
  `trees` int DEFAULT NULL,
  `resources` int DEFAULT NULL,
  `mapcode` varchar(128) DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  `winner` int DEFAULT NULL,
  `time_stop` datetime DEFAULT (curdate()),
  `time_start` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: game_players (from migration 001)
CREATE TABLE `game_players` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `game_id` int NOT NULL,
  `nick` varchar(128) NOT NULL,
  `team` int NOT NULL,
  `race` varchar(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: online_profiles
CREATE TABLE `online_profiles` (
  `nick` varchar(128) DEFAULT NULL,
  `game_id` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: profiles
CREATE TABLE `profiles` (
  `nick` varchar(128) DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `wins` int DEFAULT NULL,
  `losses` int DEFAULT NULL,
  `invalid` int DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  `reg_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: registrations
CREATE TABLE `registrations` (
  `username` varchar(128) DEFAULT NULL,
  `reg_key` varchar(128) DEFAULT NULL,
  `disabled` tinyint(1) DEFAULT '0',
  `banned` tinyint(1) DEFAULT '0',
  `password` varchar(128) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  `last_used_profile` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: settings
CREATE TABLE `settings` (
  `value` varchar(255) DEFAULT NULL,
  `property` varchar(128) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `settings` VALUES
  ('4','min_username_length'),
  ('64','max_username_length'),
  ('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789','allowed_chars'),
  ('10','max_profiles');

-- Table: messages
CREATE TABLE `messages` (
  `time` datetime DEFAULT (curdate()),
  `message` varchar(512) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: discord_to_profiles (from migration 002)
CREATE TABLE `discord_to_profiles` (
  `nick` varchar(128) NOT NULL,
  `discord_id` bigint NOT NULL,
  KEY `nick` (`nick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
