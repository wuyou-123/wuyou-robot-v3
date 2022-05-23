/*
 Navicat Premium Data Transfer

 Source Server         : 0.0.0.0
 Source Server Type    : MySQL
 Source Server Version : 50718
 Source Host           : 0.0.0.0:0
 Source Schema         : wuyou-robot-test

 Target Server Type    : MySQL
 Target Server Version : 50718
 File Encoding         : 65001

 Date: 07/05/2022 19:39:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for group_boot_state
-- ----------------------------
DROP TABLE IF EXISTS `group_boot_state`;
CREATE TABLE `group_boot_state`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group_code` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '群号',
  `state` tinyint(1) NULL DEFAULT NULL COMMENT '开关机状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for music_info
-- ----------------------------
DROP TABLE IF EXISTS `music_info`;
CREATE TABLE `music_info`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mid` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '音乐id',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `subtitle` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '子标题',
  `artist` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '艺术家',
  `album` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专辑',
  `preview_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面链接',
  `jump_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '跳转链接',
  `file_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件名',
  `pay_play` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否要付费播放',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `music_info_mid_uindex`(`mid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1293 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '音乐信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sentence
-- ----------------------------
DROP TABLE IF EXISTS `sentence`;
CREATE TABLE `sentence`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `text` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `sentence_id_uindex`(`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '戳一戳的句子' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
