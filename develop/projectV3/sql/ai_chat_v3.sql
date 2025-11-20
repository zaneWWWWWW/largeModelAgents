/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50724
 Source Host           : localhost:3306
 Source Schema         : ai_chat_v3

 Target Server Type    : MySQL
 Target Server Version : 50724
 File Encoding         : 65001

 Date: 18/11/2025 13:33:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for mbti_questions
-- ----------------------------
DROP TABLE IF EXISTS `mbti_questions`;
CREATE TABLE `mbti_questions`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `question_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_a` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_b` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dimension` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mbti_types
-- ----------------------------
DROP TABLE IF EXISTS `mbti_types`;
CREATE TABLE `mbti_types`  (
  `type_code` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `characteristics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `strengths` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `weaknesses` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`type_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for options
-- ----------------------------
DROP TABLE IF EXISTS `options`;
CREATE TABLE `options`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL,
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `value_str` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `score` decimal(8, 2) NULL DEFAULT NULL,
  `order_no` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_options_question`(`question_id`, `order_no`) USING BTREE,
  CONSTRAINT `fk_options_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of options
-- ----------------------------
INSERT INTO `options` VALUES (1, 1, '很差', NULL, 3.00, 1);
INSERT INTO `options` VALUES (2, 1, '一般', NULL, 2.00, 2);
INSERT INTO `options` VALUES (3, 1, '较好', NULL, 1.00, 3);
INSERT INTO `options` VALUES (4, 1, '很好', NULL, 0.00, 4);
INSERT INTO `options` VALUES (5, 2, '很差', NULL, 3.00, 1);
INSERT INTO `options` VALUES (6, 2, '一般', NULL, 2.00, 2);
INSERT INTO `options` VALUES (7, 2, '较好', NULL, 1.00, 3);
INSERT INTO `options` VALUES (8, 2, '很好', NULL, 0.00, 4);

-- ----------------------------
-- Table structure for questions
-- ----------------------------
DROP TABLE IF EXISTS `questions`;
CREATE TABLE `questions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `test_id` bigint(20) NOT NULL,
  `stem` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'single',
  `order_no` int(11) NOT NULL DEFAULT 1,
  `meta_json` json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_questions_test`(`test_id`, `order_no`) USING BTREE,
  CONSTRAINT `fk_questions_test` FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of questions
-- ----------------------------
INSERT INTO `questions` VALUES (1, 1, '最近一周你的睡眠情况？', 'single', 1, NULL);
INSERT INTO `questions` VALUES (2, 1, '最近一周你的食欲情况？', 'single', 2, NULL);

-- ----------------------------
-- Table structure for responses
-- ----------------------------
DROP TABLE IF EXISTS `responses`;
CREATE TABLE `responses`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `option_id` bigint(20) NULL DEFAULT NULL,
  `value_numeric` decimal(8, 2) NULL DEFAULT NULL,
  `value_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_responses_session_question`(`session_id`, `question_id`) USING BTREE,
  INDEX `fk_responses_question`(`question_id`) USING BTREE,
  INDEX `fk_responses_option`(`option_id`) USING BTREE,
  CONSTRAINT `fk_responses_option` FOREIGN KEY (`option_id`) REFERENCES `options` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_responses_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_responses_session` FOREIGN KEY (`session_id`) REFERENCES `test_sessions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of responses
-- ----------------------------
INSERT INTO `responses` VALUES (1, 11, 1, 4, NULL, NULL);
INSERT INTO `responses` VALUES (2, 11, 2, 8, NULL, NULL);
INSERT INTO `responses` VALUES (3, 12, 1, 4, NULL, NULL);
INSERT INTO `responses` VALUES (4, 12, 2, 8, NULL, NULL);
INSERT INTO `responses` VALUES (5, 13, 1, 4, NULL, NULL);
INSERT INTO `responses` VALUES (6, 13, 2, 8, NULL, NULL);
INSERT INTO `responses` VALUES (7, 14, 1, 4, NULL, NULL);
INSERT INTO `responses` VALUES (8, 14, 2, 8, NULL, NULL);
INSERT INTO `responses` VALUES (9, 15, 1, 2, NULL, NULL);
INSERT INTO `responses` VALUES (10, 15, 2, 6, NULL, NULL);
INSERT INTO `responses` VALUES (11, 16, 1, 1, NULL, NULL);
INSERT INTO `responses` VALUES (12, 16, 2, 5, NULL, NULL);
INSERT INTO `responses` VALUES (13, 18, 1, 4, NULL, NULL);
INSERT INTO `responses` VALUES (14, 18, 2, 7, NULL, NULL);

-- ----------------------------
-- Table structure for results
-- ----------------------------
DROP TABLE IF EXISTS `results`;
CREATE TABLE `results`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` bigint(20) NOT NULL,
  `total_score` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `result_json` json NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `session_id`(`session_id`) USING BTREE,
  CONSTRAINT `fk_results_session` FOREIGN KEY (`session_id`) REFERENCES `test_sessions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of results
-- ----------------------------
INSERT INTO `results` VALUES (1, 11, 0.00, NULL, NULL, '2025-11-17 23:18:25');
INSERT INTO `results` VALUES (2, 12, 0.00, NULL, NULL, '2025-11-17 23:24:46');
INSERT INTO `results` VALUES (3, 13, 0.00, NULL, NULL, '2025-11-17 23:24:56');
INSERT INTO `results` VALUES (4, 14, 0.00, NULL, NULL, '2025-11-17 23:24:59');
INSERT INTO `results` VALUES (5, 15, 4.00, NULL, NULL, '2025-11-17 23:25:02');
INSERT INTO `results` VALUES (6, 16, 6.00, NULL, NULL, '2025-11-17 23:25:06');
INSERT INTO `results` VALUES (7, 18, 1.00, NULL, NULL, '2025-11-18 00:04:16');

-- ----------------------------
-- Table structure for scl90_results
-- ----------------------------
DROP TABLE IF EXISTS `scl90_results`;
CREATE TABLE `scl90_results`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `factor_scores` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `positive_average` double NOT NULL,
  `positive_items` int(11) NOT NULL,
  `total_average` double NOT NULL,
  `total_score` int(11) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scl90_results
-- ----------------------------
INSERT INTO `scl90_results` VALUES (1, '{\"???\":2.1,\"????\":1.6,\"??????\":2.3,\"??\":2.0}', 2.5, 45, 2, 180, 1);
INSERT INTO `scl90_results` VALUES (2, '{\"???\":2.1,\"????\":1.8}', 3.33, 45, 1.67, 150, 2);

-- ----------------------------
-- Table structure for scl_factors
-- ----------------------------
DROP TABLE IF EXISTS `scl_factors`;
CREATE TABLE `scl_factors`  (
  `id` int(11) NOT NULL,
  `factor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scl_factors
-- ----------------------------
INSERT INTO `scl_factors` VALUES (1, '躯体化', '反映主观的躯体不适感，包括心血管、胃肠道、呼吸等系统的主述不适，以及头痛、背痛、肌肉酸痛和各种心理不适引发的躯体表现。');
INSERT INTO `scl_factors` VALUES (2, '强迫症状', '与临床强迫症表现的症状、定义基本相同。主要指那种明知没有必要但又无法摆脱的无意义的思想、冲动、行为等表现。');
INSERT INTO `scl_factors` VALUES (3, '人际关系敏感', '主要指某些主观的不自在感和自卑感，尤其是在与他人相比较时更突出。');
INSERT INTO `scl_factors` VALUES (4, '抑郁', '反映的是与临床上抑郁症状群相联系的广泛的概念，包括抑郁苦闷的感情和心境。');
INSERT INTO `scl_factors` VALUES (5, '焦虑', '包括一些通常在临床上明显与焦虑症状相联系的精神症状及体验，如神经过敏、紧张等。');
INSERT INTO `scl_factors` VALUES (6, '敌对', '从思维、情感及行为三方面来反映敌对表现，包括厌烦、争论、摔物等。');
INSERT INTO `scl_factors` VALUES (7, '恐怖', '与传统的恐怖状态或广场恐怖所反映的内容基本一致，包括对出门旅行、空旷场地、人群、公共场合及交通工具等的恐惧。');
INSERT INTO `scl_factors` VALUES (8, '偏执', '偏执是一个复杂的概念，主要指思维方面的异常，如投射性思维、敌对、猜疑、关系妄想等。');
INSERT INTO `scl_factors` VALUES (9, '精神病性', '包括幻听、思维播散、被控制感、思维被插入等反映精神分裂样症状的项目。');
INSERT INTO `scl_factors` VALUES (10, '其他', '主要反映睡眠及饮食情况等其他项目。');

-- ----------------------------
-- Table structure for scl_questions
-- ----------------------------
DROP TABLE IF EXISTS `scl_questions`;
CREATE TABLE `scl_questions`  (
  `id` int(11) NOT NULL,
  `question_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `factor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `factor_id` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scl_questions
-- ----------------------------
INSERT INTO `scl_questions` VALUES (1, '头痛', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (2, '神经过敏，心中不踏实', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (3, '头脑中有不必要的想法或字句盘旋', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (4, '头昏或昏倒', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (5, '对异性的兴趣减退', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (6, '对旁人责备求全', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (7, '感到别人能控制你的思想', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (8, '责怪别人制造麻烦', '偏执', 8);
INSERT INTO `scl_questions` VALUES (9, '忘记性大', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (10, '担心自己的衣饰整齐及仪态的端正', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (11, '容易烦恼和激动', '敌对', 6);
INSERT INTO `scl_questions` VALUES (12, '胸痛', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (13, '害怕空旷的场所或街道', '恐怖', 7);
INSERT INTO `scl_questions` VALUES (14, '感到自己的精力下降，活动减慢', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (15, '想结束自己的生命', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (16, '听到旁人听不到的声音', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (17, '发抖', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (18, '感到大多数人都不可信任', '偏执', 8);
INSERT INTO `scl_questions` VALUES (19, '胃口不好', '其他', 10);
INSERT INTO `scl_questions` VALUES (20, '容易哭泣', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (21, '同异性相处时感到害羞不自在', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (22, '感到受骗，中了圈套或有人想抓您', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (23, '无缘无故地突然感到害怕', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (24, '自己不能控制地大发脾气', '敌对', 6);
INSERT INTO `scl_questions` VALUES (25, '怕单独出门', '恐怖', 7);
INSERT INTO `scl_questions` VALUES (26, '经常责怪自己', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (27, '腰痛', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (28, '感到难以完成任务', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (29, '感到孤独', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (30, '感到苦闷', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (31, '过分担忧', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (32, '对事物不感兴趣', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (33, '感到害怕', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (34, '我的感情容易受到伤害', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (35, '旁人能知道您的私下想法', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (36, '感到别人不理解您不同情您', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (37, '感到人们对你不友好，不喜欢你', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (38, '做事必须做得很慢以保证做得正确', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (39, '心跳得很厉害', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (40, '恶心或胃部不舒服', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (41, '感到比不上他人', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (42, '肌肉酸痛', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (43, '感到有人在监视您谈论您', '偏执', 8);
INSERT INTO `scl_questions` VALUES (44, '难以入睡', '其他', 10);
INSERT INTO `scl_questions` VALUES (45, '做事必须反复检查', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (46, '难以作出决定', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (47, '怕乘电车、公共汽车、地铁或火车', '恐怖', 7);
INSERT INTO `scl_questions` VALUES (48, '呼吸有困难', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (49, '一阵阵发冷或发热', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (50, '因为感到害怕而避开某些东西，场合或活动', '恐怖', 7);
INSERT INTO `scl_questions` VALUES (51, '脑子变空了', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (52, '身体发麻或刺痛', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (53, '喉咙有梗塞感', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (54, '感到对前途没有希望', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (55, '不能集中注意力', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (56, '感到身体的某一部分软弱无力', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (57, '感到紧张或容易紧张', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (58, '感到手或脚发沉', '躯体化', 1);
INSERT INTO `scl_questions` VALUES (59, '想到有关死亡的事', '其他', 10);
INSERT INTO `scl_questions` VALUES (60, '吃得太多', '其他', 10);
INSERT INTO `scl_questions` VALUES (61, '当别人看着您或谈论您时感到不自在', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (62, '有一些不属于您自己的想法', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (63, '有想打人或伤害他人的冲动', '敌对', 6);
INSERT INTO `scl_questions` VALUES (64, '醒得太早', '其他', 10);
INSERT INTO `scl_questions` VALUES (65, '必须反复洗手、点数目或触摸某些东西', '强迫症状', 2);
INSERT INTO `scl_questions` VALUES (66, '睡得不稳不深', '其他', 10);
INSERT INTO `scl_questions` VALUES (67, '有想摔坏或破坏东西的冲动', '敌对', 6);
INSERT INTO `scl_questions` VALUES (68, '有一些别人没有的想法或念头', '偏执', 8);
INSERT INTO `scl_questions` VALUES (69, '感到对别人神经过敏', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (70, '在商店或电影院等人多的地方感到不自在', '恐怖', 7);
INSERT INTO `scl_questions` VALUES (71, '感到任何事情都很难做', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (72, '一阵阵恐惧或惊恐', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (73, '感到在公共场合吃东西很不舒服', '人际关系敏感', 3);
INSERT INTO `scl_questions` VALUES (74, '经常与人争论', '敌对', 6);
INSERT INTO `scl_questions` VALUES (75, '单独一人时神经很紧张', '恐怖', 7);
INSERT INTO `scl_questions` VALUES (76, '别人对您的成绩没有作出恰当的评价', '偏执', 8);
INSERT INTO `scl_questions` VALUES (77, '即使和别人在一起也感到孤单', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (78, '感到坐立不安心神不宁', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (79, '感到自己没有什么价值', '抑郁', 4);
INSERT INTO `scl_questions` VALUES (80, '感到熟悉的东西变成陌生或不象是真的', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (81, '大叫或摔东西', '敌对', 6);
INSERT INTO `scl_questions` VALUES (82, '害怕会在公共场合昏倒', '恐怖', 7);
INSERT INTO `scl_questions` VALUES (83, '感到别人想占您的便宜', '偏执', 8);
INSERT INTO `scl_questions` VALUES (84, '为一些有关\"性\"的想法而很苦恼', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (85, '认为应该因为自己的过错而受到惩罚', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (86, '感到要赶快把事情做完', '焦虑', 5);
INSERT INTO `scl_questions` VALUES (87, '感到自己的身体有严重问题', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (88, '从未感到和其他人很亲近', '精神病性', 9);
INSERT INTO `scl_questions` VALUES (89, '感到自己有罪', '其他', 10);
INSERT INTO `scl_questions` VALUES (90, '感到自己的脑子有毛病', '精神病性', 9);

-- ----------------------------
-- Table structure for test_sessions
-- ----------------------------
DROP TABLE IF EXISTS `test_sessions`;
CREATE TABLE `test_sessions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `test_id` bigint(20) NOT NULL,
  `started_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime(0) NULL DEFAULT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ongoing',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sessions_user_test`(`user_id`, `test_id`, `status`) USING BTREE,
  INDEX `fk_sessions_test`(`test_id`) USING BTREE,
  CONSTRAINT `fk_sessions_test` FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of test_sessions
-- ----------------------------
INSERT INTO `test_sessions` VALUES (1, 5, 1, '2025-11-17 23:10:06', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (2, 5, 1, '2025-11-17 23:10:14', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (3, 5, 1, '2025-11-17 23:10:24', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (4, 5, 1, '2025-11-17 23:10:33', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (5, 5, 1, '2025-11-17 23:10:46', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (6, 5, 1, '2025-11-17 23:11:32', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (7, 5, 1, '2025-11-17 23:12:25', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (8, 5, 1, '2025-11-17 23:15:35', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (9, 5, 1, '2025-11-17 23:15:42', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (10, 5, 1, '2025-11-17 23:15:51', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (11, 5, 1, '2025-11-17 23:18:23', '2025-11-17 23:18:25', 'finished');
INSERT INTO `test_sessions` VALUES (12, 5, 1, '2025-11-17 23:24:41', '2025-11-17 23:24:46', 'finished');
INSERT INTO `test_sessions` VALUES (13, 5, 1, '2025-11-17 23:24:54', '2025-11-17 23:24:56', 'finished');
INSERT INTO `test_sessions` VALUES (14, 5, 1, '2025-11-17 23:24:57', '2025-11-17 23:24:59', 'finished');
INSERT INTO `test_sessions` VALUES (15, 5, 1, '2025-11-17 23:25:00', '2025-11-17 23:25:02', 'finished');
INSERT INTO `test_sessions` VALUES (16, 5, 1, '2025-11-17 23:25:03', '2025-11-17 23:25:06', 'finished');
INSERT INTO `test_sessions` VALUES (17, 5, 1, '2025-11-17 23:25:07', NULL, 'ongoing');
INSERT INTO `test_sessions` VALUES (18, 5, 1, '2025-11-18 00:04:14', '2025-11-18 00:04:16', 'finished');

-- ----------------------------
-- Table structure for tests
-- ----------------------------
DROP TABLE IF EXISTS `tests`;
CREATE TABLE `tests`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '未分类',
  `version` int(11) NOT NULL DEFAULT 1,
  `is_active` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `total_score_thresholds` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '格式如：0:正常;10:轻度;20:中度;30:重度',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tests
-- ----------------------------
INSERT INTO `tests` VALUES (1, 'STANDARD', '标准测试', '示例单选问卷', '心理健康综合', 1, 1, '2025-11-17 20:52:02', '2025-11-18 00:03:09', NULL);
INSERT INTO `tests` VALUES (2, '001', '11', '', '焦虑', 1, 1, '2025-11-18 00:04:39', '2025-11-18 00:04:39', '');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `mbti_type` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `age` int(11) NULL DEFAULT NULL,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `gender` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `grade` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_r43af9ap4edm43mmtq01oddj6`(`username`) USING BTREE,
  UNIQUE INDEX `UK_6dotkott2kjsp8vw4d0m25fb7`(`email`) USING BTREE,
  UNIQUE INDEX `UK_du5v5sr43g5bfnji4vb8hg5s3`(`phone`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (5, NULL, '123@qq.com', '$2a$10$wN3acFKz4J5ImC4AGHSmmu.Kh4RDUym7h2X0oAWLw9G3wmFeWD1mq', '123456', '2025-10-08 17:18:29.702082', 'admin', NULL, 18, '/uploads/avatars/6de47d13-3971-490e-bd4e-33f17f164e1d.jpg', NULL, '男', NULL);

SET FOREIGN_KEY_CHECKS = 1;
