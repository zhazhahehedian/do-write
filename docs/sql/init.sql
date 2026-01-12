-- 数据库初始化脚本
-- do-write 项目
-- 注意：所有 ID 字段使用雪花算法生成（应用层生成），不依赖数据库自增

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `do_write` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `do_write`;

-- ==========================================
-- 用户认证模块
-- ==========================================

-- 用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL COMMENT '用户ID（雪花算法生成）',
    `username` VARCHAR(50) COMMENT '用户名（唯一，可为空-仅第三方登录时）',
    `password` VARCHAR(200) COMMENT '密码（BCrypt加密，第三方登录可为空）',
    `nickname` VARCHAR(100) NOT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',

    -- 账号状态
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `user_type` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '用户类型：NORMAL-普通用户，ADMIN-管理员',

    -- 统计信息
    `login_count` INT DEFAULT 0 COMMENT '登录次数',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',

    -- 审计字段
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT NULL COMMENT '创建人ID',
    `update_by` BIGINT NULL COMMENT '更新人ID',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark` VARCHAR(500) NULL COMMENT '备注',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 第三方登录绑定表
CREATE TABLE `user_oauth` (
    `id` BIGINT NOT NULL COMMENT '绑定ID（雪花算法生成）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',

    -- 第三方平台信息
    `oauth_type` VARCHAR(20) NOT NULL COMMENT '第三方类型：LINUXDO, FISHPI, GITHUB, WECHAT, QQ',
    `oauth_id` VARCHAR(100) NOT NULL COMMENT '第三方平台用户唯一标识',
    `oauth_user_name` VARCHAR(100) COMMENT '第三方平台用户名',
    `oauth_nickname` VARCHAR(100) COMMENT '第三方平台昵称',
    `oauth_avatar` VARCHAR(500) COMMENT '第三方平台头像',
    `oauth_email` VARCHAR(100) COMMENT '第三方平台邮箱',

    -- Linux.do 特有字段
    `trust_level` TINYINT COMMENT '信任等级（Linux.do特有）：0-4',

    -- OAuth 令牌（可选，用于后续API调用）
    `access_token` TEXT COMMENT '访问令牌（AES-256加密存储）',
    `refresh_token` TEXT COMMENT '刷新令牌（AES-256加密存储）',
    `expires_at` DATETIME COMMENT '令牌过期时间',

    -- 绑定状态
    `status` TINYINT DEFAULT 1 COMMENT '绑定状态：0-已解绑，1-已绑定',
    `bind_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `unbind_time` DATETIME COMMENT '解绑时间',

    -- 审计字段
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_type_id` (`oauth_type`, `oauth_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方登录绑定表';

-- 用户会话表（可选，用于JWT令牌管理和多设备登录）
CREATE TABLE `user_session` (
    `id` VARCHAR(64) NOT NULL COMMENT '会话ID (UUID)',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `token` VARCHAR(500) NOT NULL COMMENT 'JWT Token',
    `login_type` VARCHAR(20) COMMENT '登录方式：PASSWORD, LINUXDO, FISHPI, GITHUB等',
    `login_ip` VARCHAR(50) COMMENT '登录IP',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `device_type` VARCHAR(20) COMMENT '设备类型：WEB, MOBILE, APP',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_token` (`token`(255)),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话表';

-- 用户API配置表
CREATE TABLE `user_api_config` (
    `id` BIGINT NOT NULL COMMENT '配置ID（雪花算法生成）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID (0表示系统配置)',
    `api_type` VARCHAR(50) NOT NULL COMMENT 'AI提供商类型: OPENAI, OLLAMA等',
    `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称 (用户自定义，如"我的GPT4配置")',
    `api_key` VARCHAR(500) COMMENT 'API密钥 (AES-256加密存储)',
    `base_url` VARCHAR(500) COMMENT 'API基础URL',
    `model_name` VARCHAR(100) COMMENT '模型名称 (如gpt-4o, deepseek-r1)',
    `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数 (0.0-2.0)',
    `max_tokens` INT DEFAULT 4096 COMMENT '最大Token数',
    `embedding_model` VARCHAR(100) COMMENT '嵌入模型名称',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否为默认配置 (每个用户只能有一个默认配置)',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `remark` VARCHAR(500) COMMENT '备注',

    -- 审计字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_config` (`user_id`, `config_name`, `is_deleted`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_provider` (`api_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户AI API配置表';

-- ==========================================
-- 小说创作模块
-- ==========================================

-- 小说项目表
CREATE TABLE `novel_project` (
    `id` BIGINT NOT NULL COMMENT '项目ID（雪花算法生成）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID (多用户隔离)',
    `title` VARCHAR(200) NOT NULL COMMENT '书名',
    `description` TEXT COMMENT '简介',
    `theme` VARCHAR(100) COMMENT '主题',
    `genre` VARCHAR(50) COMMENT '类型 (玄幻/都市/科幻等)',
    `target_words` INT DEFAULT 0 COMMENT '目标字数',
    `current_words` INT DEFAULT 0 COMMENT '当前字数',
    `status` VARCHAR(20) DEFAULT 'planning' COMMENT '状态 (planning/writing/completed)',

    -- 世界观字段 (JSON存储)
    `world_time_period` TEXT COMMENT '时间背景',
    `world_location` TEXT COMMENT '地理位置',
    `world_atmosphere` TEXT COMMENT '氛围基调',
    `world_rules` TEXT COMMENT '世界规则',

    -- 项目配置
    `chapter_count` INT DEFAULT 0 COMMENT '计划章节数',
    `narrative_perspective` VARCHAR(20) COMMENT '叙事视角 (第一人称/第三人称)',
    `character_count` INT DEFAULT 0 COMMENT '角色数量',
    `outline_mode` VARCHAR(20) DEFAULT 'one-to-one' COMMENT '大纲模式 (one-to-one/one-to-many)',

    -- 向导状态
    `wizard_status` VARCHAR(20) COMMENT '向导状态',
    `wizard_step` INT DEFAULT 0 COMMENT '向导步骤',

    -- AI配置
    `ai_model` VARCHAR(50) COMMENT 'AI模型名称',
    `writing_style_code` VARCHAR(50) COMMENT '写作风格编码（对应枚举 WritingStyle.code）',

    -- 审计字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小说项目表';

-- 章节表
CREATE TABLE `novel_chapter` (
    `id` BIGINT NOT NULL COMMENT '章节ID（雪花算法生成）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID (FK)',
    `outline_id` BIGINT COMMENT '大纲ID (FK)',

    -- 章节基本信息
    `chapter_number` INT NOT NULL COMMENT '章节序号',
    `sub_index` INT DEFAULT 0 COMMENT 'one-to-many模式下的子序号',
    `title` VARCHAR(200) NOT NULL COMMENT '章节标题',
    `content` LONGTEXT COMMENT '章节内容',
    `summary` TEXT COMMENT '章节摘要（自动生成）',
    `word_count` INT DEFAULT 0 COMMENT '字数统计',

    -- 状态管理（分离内容状态和生成状态）
    `status` VARCHAR(20) DEFAULT 'draft' COMMENT '内容状态: draft-草稿/published-已发布/archived-已归档',
    `generation_status` VARCHAR(20) DEFAULT 'pending' COMMENT '生成状态: pending-待生成/generating-生成中/completed-已完成/failed-失败',

    -- 生成参数
    `expansion_plan` JSON COMMENT 'one-to-many展开规划',
    `ai_model` VARCHAR(50) COMMENT '使用的AI模型',
    `generation_params` JSON COMMENT '生成参数（温度、top_p等）',
    `style_code` VARCHAR(50) COMMENT '写作风格编码（对应枚举 WritingStyle.code）',

    -- 版本管理
    `version` INT DEFAULT 1 COMMENT '版本号',
    `previous_version_id` BIGINT COMMENT '上一版本ID',

    -- 审计字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_chapter` (`project_id`, `chapter_number`, `sub_index`),
    KEY `idx_project_chapter_number` (`project_id`, `chapter_number`),
    KEY `idx_outline_id` (`outline_id`),
    KEY `idx_status` (`status`),
    KEY `idx_generation_status` (`generation_status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='章节表';

-- 大纲表
CREATE TABLE `novel_outline` (
    `id` BIGINT NOT NULL COMMENT '大纲ID（雪花算法生成）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID (FK)',
    `order_index` INT NOT NULL COMMENT '排序序号',
    `title` VARCHAR(200) NOT NULL COMMENT '大纲标题',
    `content` TEXT COMMENT '大纲内容',

    -- 结构化数据 (JSON)
    `structure` JSON COMMENT '结构化数据 (情节点、冲突、转折等)',

    -- 审计字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,

    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_order_index` (`order_index`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大纲表';

-- 角色/组织表
CREATE TABLE `novel_character` (
    `id` BIGINT NOT NULL COMMENT '角色ID（雪花算法生成）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID (FK)',
    `name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `is_organization` TINYINT(1) DEFAULT 0 COMMENT '是否组织 (0=角色, 1=组织)',

    -- 角色信息
    `role_type` VARCHAR(20) COMMENT '角色类型 (protagonist/supporting/antagonist)',
    `age` INT COMMENT '年龄',
    `gender` VARCHAR(10) COMMENT '性别',
    `appearance` TEXT COMMENT '外貌描写',
    `personality` TEXT COMMENT '性格特点',
    `background` TEXT COMMENT '背景故事',

    -- 关系网 (JSON)
    `relationships` JSON COMMENT '与其他角色的关系',

    -- 组织信息
    `organization_type` VARCHAR(50) COMMENT '组织类型',
    `organization_purpose` TEXT COMMENT '组织目的',
    `organization_members` JSON COMMENT '组织成员列表',

    -- 审计字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,

    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_role_type` (`role_type`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色/组织表';

-- 故事记忆表
CREATE TABLE `novel_story_memory` (
    `id` BIGINT NOT NULL COMMENT '记忆ID（雪花算法生成）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID (FK)',
    `chapter_id` BIGINT NOT NULL COMMENT '来源章节ID (FK)',

    -- 记忆内容
    `memory_type` VARCHAR(30) NOT NULL COMMENT '记忆类型: plot_point-情节点/hook-悬念钩子/foreshadow-伏笔/character_event-角色事件/location_event-地点事件',
    `title` VARCHAR(200) NOT NULL COMMENT '记忆标题',
    `content` TEXT NOT NULL COMMENT '记忆简化内容',
    `full_context` TEXT COMMENT '完整上下文',

    -- 关联信息 (JSON)
    `related_characters` JSON COMMENT '相关角色ID列表',
    `related_locations` JSON COMMENT '相关地点',

    -- 重要性与时间线
    `importance_score` DECIMAL(3,2) DEFAULT 0.50 COMMENT '重要性分数 0.00-1.00',
    `story_timeline` INT COMMENT '故事时间线（章节序号）',

    -- 伏笔追踪
    `is_foreshadow` TINYINT(1) DEFAULT 0 COMMENT '伏笔状态: 0-普通/1-已埋下/2-已回收',
    `foreshadow_resolved_at` BIGINT COMMENT '伏笔回收时的章节ID',

    -- 向量存储
    `vector_id` VARCHAR(100) COMMENT '向量数据库中的ID',
    `embedding_model` VARCHAR(50) COMMENT '使用的嵌入模型',

    -- 审计字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_chapter_id` (`chapter_id`),
    KEY `idx_memory_type` (`memory_type`),
    KEY `idx_project_importance` (`project_id`, `importance_score` DESC),
    KEY `idx_project_foreshadow` (`project_id`, `is_foreshadow`),
    KEY `idx_story_timeline` (`project_id`, `story_timeline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='故事记忆表';

-- 剧情分析表
CREATE TABLE `novel_plot_analysis` (
    `id` BIGINT NOT NULL COMMENT '分析ID（雪花算法生成）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID (FK)',
    `chapter_id` BIGINT NOT NULL COMMENT '章节ID (FK, UNIQUE)',

    -- 剧情结构
    `plot_stage` VARCHAR(20) COMMENT '剧情阶段 (开端/发展/高潮/结局)',
    `conflict_level` INT COMMENT '冲突等级 (1-10)',
    `conflict_types` JSON COMMENT '冲突类型列表',

    -- 情感分析
    `emotional_tone` VARCHAR(50) COMMENT '情感基调',
    `emotional_intensity` DECIMAL(3,2) COMMENT '情感强度 (0.0-1.0)',
    `emotional_curve` JSON COMMENT '情感曲线数据',

    -- 钩子分析 (JSON数组)
    `hooks` JSON COMMENT '钩子列表 [{type, content, strength, position}]',
    `hooks_count` INT DEFAULT 0 COMMENT '钩子数量',
    `hooks_avg_strength` DECIMAL(3,2) COMMENT '钩子平均强度',

    -- 伏笔分析 (JSON数组)
    `foreshadows` JSON COMMENT '伏笔列表',
    `foreshadows_planted` INT DEFAULT 0 COMMENT '本章埋下的伏笔数量',
    `foreshadows_resolved` INT DEFAULT 0 COMMENT '本章回收的伏笔数量',

    -- 质量评分
    `overall_quality_score` DECIMAL(3,1) COMMENT '整体质量评分 (0.0-10.0)',
    `pacing_score` DECIMAL(3,1) COMMENT '节奏评分',
    `engagement_score` DECIMAL(3,1) COMMENT '吸引力评分',
    `coherence_score` DECIMAL(3,1) COMMENT '连贯性评分',

    -- 改进建议
    `analysis_report` TEXT COMMENT '分析报告（Markdown格式）',
    `suggestions` JSON COMMENT '改进建议列表',

    -- AI元数据
    `ai_model` VARCHAR(50) COMMENT '分析使用的AI模型',

    -- 审计字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chapter_analysis` (`chapter_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='剧情分析表';

-- 生成任务表
CREATE TABLE `novel_generation_task` (
    `id` BIGINT NOT NULL COMMENT '任务ID（雪花算法生成）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID (FK)',
    `user_id` BIGINT NOT NULL COMMENT '用户ID (用于限制并发任务数)',

    -- 任务信息
    `task_type` VARCHAR(30) NOT NULL COMMENT '任务类型: single_chapter-单章生成/batch_chapter-批量生成/analysis-剧情分析',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending-等待/running-执行中/completed-已完成/failed-失败/cancelled-已取消',

    -- 任务参数 (JSON)
    `params` JSON COMMENT '任务参数',
    `chapter_ids` JSON COMMENT '关联的章节ID列表',

    -- 进度和结果
    `progress` INT DEFAULT 0 COMMENT '进度百分比 0-100',
    `current_step` VARCHAR(100) COMMENT '当前步骤描述（如"正在生成第3/10章"）',
    `result` JSON COMMENT '任务结果',
    `error_message` TEXT COMMENT '错误信息',

    -- 时间记录
    `started_at` DATETIME COMMENT '开始执行时间',
    `completed_at` DATETIME COMMENT '完成时间',

    -- 审计字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_status` (`status`),
    KEY `idx_task_type` (`task_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成任务表';

-- ==========================================
-- 初始化数据
-- ==========================================

-- 插入测试用户（密码为 123456 的 BCrypt 加密）
-- 注意：ID 使用具体数字值，实际应用中由 MyBatis-Plus 自动生成雪花 ID
INSERT INTO `user` (`id`, `username`, `nickname`, `email`, `password`, `status`, `user_type`) VALUES
(1000000000000000001, 'admin', '管理员', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, 'ADMIN'),
(1000000000000000002, 'test', '测试用户', 'test@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, 'NORMAL');

-- 插入系统级嵌入模型配置（user_id=0 表示系统配置，用于向量搜索）
-- 注意：api_key 需要根据实际情况填写并进行 AES-256 加密，这里使用占位符
-- 可选模型示例：text-embedding-3-small (OpenAI), text-embedding-ada-002 (OpenAI), nomic-embed-text (Ollama)
INSERT INTO `user_api_config` (`id`, `user_id`, `api_type`, `config_name`, `api_key`, `base_url`, `embedding_model`, `is_default`, `status`, `remark`) VALUES
(1000000000000000001, 0, 'OPENAI', '系统嵌入模型配置', NULL, 'https://api.openai.com', 'text-embedding-3-small', 1, 1, '系统级嵌入模型，用于故事记忆向量化。请在管理后台配置实际的 API Key');

-- 创建索引优化查询性能
-- 用户表额外索引
CREATE INDEX idx_user_create_time ON `user` (`create_time`);
CREATE INDEX idx_user_last_login ON `user` (`last_login_time`);

-- OAuth表额外索引
CREATE INDEX idx_oauth_create_time ON `user_oauth` (`create_time`);

-- 项目表额外索引
CREATE INDEX idx_project_genre ON `novel_project` (`genre`);

-- 章节表额外索引
CREATE INDEX idx_chapter_create_time ON `novel_chapter` (`create_time`);
