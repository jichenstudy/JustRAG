/*
 Navicat Premium Data Transfer

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80406
 Source Host           : localhost:3306
 Source Schema         : just-rag

 Target Server Type    : MySQL
 Target Server Version : 80406
 File Encoding         : 65001

 Date: 18/06/2026 19:35:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_mcp_server_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_mcp_server_config`;
CREATE TABLE `ai_mcp_server_config`  (
  `id` bigint NOT NULL COMMENT 'MCP服务配置ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MCP服务名称（唯一标识）',
  `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '传输类型: stdio/sse/http',
  `command` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'STDIO模式：启动命令',
  `args` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'STDIO模式：命令参数（JSON格式）',
  `env` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'STDIO模式：环境变量（JSON格式）',
  `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'SSE/HTTP模式：服务地址URL',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_enabled`(`is_enabled` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MCP服务配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_mcp_server_config
-- ----------------------------

-- ----------------------------
-- Table structure for ai_model_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_model_config`;
CREATE TABLE `ai_model_config`  (
  `id` bigint NOT NULL COMMENT 'AI大模型配置ID',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型名称',
  `model_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型类型 CHAT/EMBEDDING/VISION',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提供商: DASHSCOPE, OPENAI, OLLAMA, AZURE_OPENAI',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型密钥',
  `api_endpoint` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型端点',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI大模型配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_model_config
-- ----------------------------

-- ----------------------------
-- Table structure for chat_assistant
-- ----------------------------
DROP TABLE IF EXISTS `chat_assistant`;
CREATE TABLE `chat_assistant`  (
  `id` bigint NOT NULL COMMENT '聊天助理主键ID',
  `assistant_avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '助理头像URL或存储路径',
  `assistant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '助理名称',
  `assistant_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '助理描述信息',
  `empty_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '空回复（无匹配回复时的默认回复）',
  `opening_statement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '开场白（会话开始时的欢迎语）',
  `knowledge_base_id` bigint NULL DEFAULT NULL COMMENT '关联知识库ID',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系统提示词（指导AI行为的指令）',
  `top_n` int NULL DEFAULT 5 COMMENT '检索数量（返回最相似结果数量，3-10，默认5）',
  `top_p` decimal(3, 2) NULL DEFAULT 0.50 COMMENT '核采样参数（控制词汇选择范围，0-1，默认0.8）',
  `enable_reasoning_mode` tinyint(1) NULL DEFAULT 0 COMMENT '是否开启推理模式（0-关闭，1-开启）',
  `model_id` bigint NULL DEFAULT NULL COMMENT '关联模型ID',
  `temperature` decimal(3, 2) NULL DEFAULT 0.80 COMMENT '温度参数（控制随机性，0-2，默认0.8）',
  `presence_penalty` decimal(3, 2) NULL DEFAULT 0.00 COMMENT '存在处罚（减少重复内容，-2到2，默认0）',
  `frequency_penalty` decimal(3, 2) NULL DEFAULT 0.00 COMMENT '频率惩罚（减少高频词，-2到2，默认0）',
  `max_tokens` int NULL DEFAULT 2000 COMMENT '最大token数（单次生成最大长度）',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assistant_name`(`assistant_name` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_status_model`(`enable_reasoning_mode` ASC, `model_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天助理配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_assistant
-- ----------------------------

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL COMMENT '聊天消息主键ID',
  `session_id` bigint NOT NULL COMMENT '所属聊天会话ID，对应 chat_session.id',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息角色：USER 用户 / ASSISTANT 助手',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容文本',
  `created_at` timestamp NOT NULL COMMENT '消息发送或生成时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_message
-- ----------------------------

-- ----------------------------
-- Table structure for chat_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session`  (
  `id` bigint NOT NULL COMMENT '聊天会话主键ID',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题，通常取自首条用户问题',
  `knowledge_base_id` bigint NULL DEFAULT NULL COMMENT '关联的知识库ID，可为空表示自由对话',
  `model_id` bigint NULL DEFAULT NULL,
  `assistant_id` bigint NULL DEFAULT NULL COMMENT '聊天助理ID',
  `created_at` datetime NOT NULL COMMENT '会话创建时间',
  `updated_at` datetime NOT NULL COMMENT '会话最后活跃时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天会话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_session
-- ----------------------------

-- ----------------------------
-- Table structure for document
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document`  (
  `id` bigint NOT NULL COMMENT '文档主键ID',
  `knowledge_base_id` bigint NULL DEFAULT NULL COMMENT '所属知识库ID',
  `file_id` bigint NOT NULL COMMENT '关联的文件ID，对应 file_detail.id',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档名称',
  `doc_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档类型：PDF / DOCX / TXT / HTML / MARKDOWN',
  `parse_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '解析状态：UPLOADED 已上传 / PARSING 解析中 / PARSED 已解析 / FAILED 解析失败',
  `chunk_count` int NULL DEFAULT 0 COMMENT '文档拆分后的分块数量',
  `created_at` datetime NOT NULL COMMENT '文档创建时间',
  `updated_at` datetime NOT NULL COMMENT '文档最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库文档表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of document
-- ----------------------------

-- ----------------------------
-- Table structure for document_chunk
-- ----------------------------
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk`  (
  `id` bigint NOT NULL COMMENT '文档分块主键ID',
  `document_id` bigint NOT NULL COMMENT '所属文档ID，对应 document.id',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分块后的文本内容',
  `chunk_index` int NOT NULL COMMENT '分块在文档中的顺序编号，从 0 或 1 开始',
  `token_size` int NULL DEFAULT NULL COMMENT '分块文本的 token 数量，用于上下文窗口控制',
  `section_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '章节路径，如：第一章 > 1.1 概述',
  `section_title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '章节标题',
  `position` int NULL DEFAULT NULL COMMENT '分片在原文章节内的位置序号',
  `char_start_index` int NULL DEFAULT NULL COMMENT '分片在原文档中的字符起始位置',
  `char_end_index` int NULL DEFAULT NULL COMMENT '分片在原文档中的字符结束位置',
  `created_at` datetime NOT NULL COMMENT '分块创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_document_id`(`document_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档内容分块表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of document_chunk
-- ----------------------------

-- ----------------------------
-- Table structure for file_detail
-- ----------------------------
DROP TABLE IF EXISTS `file_detail`;
CREATE TABLE `file_detail`  (
  `id` bigint NOT NULL COMMENT '文件记录主键ID',
  `url` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '文件访问地址',
  `size` bigint NULL DEFAULT NULL COMMENT '文件大小，单位字节',
  `filename` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件名称',
  `original_filename` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `bucket_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'minio 桶名称',
  `object_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'minio 对象名称',
  `base_path` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '基础存储路径',
  `path` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '存储路径',
  `ext` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件扩展名',
  `content_type` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'MIME类型',
  `platform` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '存储平台',
  `hash_info` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '哈希信息',
  `upload_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '上传ID',
  `upload_status` int NULL DEFAULT NULL COMMENT '上传状态 0-上传失败 1-上传成功',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `knowledge_base_id` bigint NULL DEFAULT NULL COMMENT '绑定的知识库ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '文件记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_detail
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base`  (
  `id` bigint NOT NULL COMMENT '知识库主键ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述说明',
  `embedding_model_id` bigint NULL DEFAULT NULL COMMENT '向量模型ID',
  `vision_model_id` bigint NULL DEFAULT NULL COMMENT '视觉模型ID',
  `collections_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'milvus文档库名称',
  `chunk_strategy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'SMART' COMMENT '切分策略：SMART 结构感知 / FIXED 固定长度',
  `chunk_size` int NULL DEFAULT 1000 COMMENT '分片字符数',
  `chunk_overlap` int NULL DEFAULT 200 COMMENT '分片重叠字符数',
  `chunk_min_size` int NULL DEFAULT 100 COMMENT '最小分片字符数',
  `team_id` bigint NULL DEFAULT NULL COMMENT '团队ID',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_base
-- ----------------------------

-- ----------------------------
-- Table structure for oss_config
-- ----------------------------
DROP TABLE IF EXISTS `oss_config`;
CREATE TABLE `oss_config`  (
  `oss_config_id` bigint NOT NULL COMMENT '存储配置主键ID',
  `config_key` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '配置key',
  `access_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'accessKey',
  `secret_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'secretKey',
  `bucket_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '桶名称',
  `prefix` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '前缀',
  `endpoint` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '访问站点',
  `domain` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '自定义域名',
  `is_https` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '是否https（Y=是,N=否）',
  `region` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '域',
  `access_policy` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '桶权限类型(0=private 1=public 2=custom)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否默认（0=是,1=否）',
  `ext_json` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`oss_config_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存储配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of oss_config
-- ----------------------------
INSERT INTO `oss_config` VALUES (1, 'minio', 'minioadmin', 'minioadmin', 'just-rag', '', 'http://localhost:9000', '', 'N', '', '1', '0', '', 1, '', '2026-06-22 10:57:10', '2026-06-22 10:57:10');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL COMMENT '用户主键ID',
  `username` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '账号',
  `password` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录密码',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` int NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  `ip` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'ip地址',
  `ip_location` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'ip来源',
  `os` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录系统',
  `last_login_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
  `browser` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '浏览器',
  `nickname` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '头像',
  `mobile` varchar(15) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `sex` int NULL DEFAULT NULL COMMENT '性别',
  `login_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录方式',
  `signature` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '个性签名',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$9eqqZ6CVCPeLHThuo9s22edJ7ZQHofiU9BphFs4xhyfuxNTxjVDHi', '2026-06-18 12:23:30', '2026-06-18 12:23:40', 1, '0:0:0:0:0:0:0:1', '内网IP', 'Windows 10 or Windows Server 2016', '2026-06-20 22:09:01', 'QQBrowser', '超级管理员', 'https://q8.itc.cn/q_70/images03/20240305/5637ad3f16d144ecb5469acbde2b67c7.jpeg', '15888888888', 'mail@shujichen.com', 1, 'PASSWORD', 'The world belongs to all, long live the people.');

-- ----------------------------
-- Table structure for sys_user_team
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_team`;
CREATE TABLE `sys_user_team`  (
  `id` bigint NOT NULL COMMENT '团队主键ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '团队的用户ID',
  `team_id` bigint NULL DEFAULT NULL COMMENT '团队ID，也是用户ID',
  `create_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '团队关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_team
-- ----------------------------
INSERT INTO `sys_user_team` VALUES (1, 1, 1, '2026-06-20 12:23:45');

SET FOREIGN_KEY_CHECKS = 1;
