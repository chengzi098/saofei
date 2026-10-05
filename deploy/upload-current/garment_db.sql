-- 制衣厂综合管理系统 - 建表脚本
-- 数据库: garment_db

CREATE DATABASE IF NOT EXISTS garment_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE garment_db;

-- ==================== 公司租户表 ====================
CREATE TABLE IF NOT EXISTS `company` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '公司名称',
    `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    `expire_time` DATETIME DEFAULT NULL COMMENT '租期到期时间',
    `renew_time` DATETIME DEFAULT NULL COMMENT '最近续费时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='公司租户表';

-- ==================== 用户表（全局账号） ====================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
    `password` VARCHAR(200) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
    `gender` TINYINT DEFAULT NULL COMMENT '性别：0女 1男',
    `bank_card` VARCHAR(30) DEFAULT NULL COMMENT '银行卡号',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型：0超级管理员 1普通用户',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `need_change_password` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要修改密码：0不需要 1需要',
    `last_view_records_at` DATETIME DEFAULT NULL COMMENT '最近查看报工记录时间',
    `last_view_adjustment_at` DATETIME DEFAULT NULL COMMENT '最近查看工资调整时间',
    `last_view_settlement_at` DATETIME DEFAULT NULL COMMENT '最近查看工资结算时间',
    `last_view_return_at` DATETIME DEFAULT NULL COMMENT '最近查看退货时间',
    `last_view_review_at` DATETIME DEFAULT NULL COMMENT '最近查看审核时间',
    `last_view_supervisor_adj_at` DATETIME DEFAULT NULL COMMENT '主管最近查看调整时间',
    `last_view_boss_return_at` DATETIME DEFAULT NULL COMMENT '老板最近查看退货时间',
    `last_view_boss_review_at` DATETIME DEFAULT NULL COMMENT '老板最近查看审核时间',
    `last_view_boss_adj_at` DATETIME DEFAULT NULL COMMENT '老板最近查看调整时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='用户表';

-- ==================== 用户公司关联表 ====================
CREATE TABLE IF NOT EXISTS `user_company` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：boss老板 / supervisor主管 / worker工人',
    `team_id` BIGINT DEFAULT NULL COMMENT '所属班组ID',
    `hire_date` DATE DEFAULT NULL COMMENT '入职时间',
    `bundle_prefix` VARCHAR(2) DEFAULT NULL COMMENT '分扎字母前缀(A/B/C...)，仅主管',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0离职 1在职',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='用户公司关联表';

-- ==================== 公司续费记录表 ====================
CREATE TABLE IF NOT EXISTS `company_renew_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `prev_expire_time` DATETIME DEFAULT NULL COMMENT '续费前到期时间',
    `new_expire_time` DATETIME NOT NULL COMMENT '续费后到期时间',
    `months` INT NOT NULL COMMENT '续费月数',
    `remark` VARCHAR(50) DEFAULT NULL COMMENT '备注',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='公司续费记录表';

-- ==================== 款式表 ====================
CREATE TABLE IF NOT EXISTS `style` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '所属公司',
    `style_no` VARCHAR(50) DEFAULT NULL COMMENT '款式编号',
    `name` VARCHAR(100) NOT NULL COMMENT '款式名称',
    `colors` VARCHAR(500) DEFAULT NULL COMMENT '颜色（逗号分隔）',
    `quantity` INT NOT NULL DEFAULT 0 COMMENT '款式的数量',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片地址',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `archived` TINYINT NOT NULL DEFAULT 0 COMMENT '归档状态：0正常 1已归档',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='款式表';

-- ==================== 工序表 ====================
CREATE TABLE IF NOT EXISTS `process` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `style_id` BIGINT NOT NULL COMMENT '所属款式',
    `bed_id` BIGINT DEFAULT NULL COMMENT '所属床号',
    `company_id` BIGINT NOT NULL COMMENT '所属公司',
    `name` VARCHAR(100) NOT NULL COMMENT '工序名称',
    `seq` INT NOT NULL DEFAULT 0 COMMENT '工序序号',
    `price` DECIMAL(10,2) NOT NULL COMMENT '计件单价',
    `price_locked` TINYINT NOT NULL DEFAULT 0 COMMENT '价格是否已锁定：0未锁定(可修改一次) 1已锁定(不可修改)',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_style_id` (`style_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='工序表';

-- ==================== 床号表 ====================
CREATE TABLE IF NOT EXISTS `bed` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '所属公司',
    `style_id` BIGINT NOT NULL COMMENT '所属款式',
    `bed_no` VARCHAR(50) NOT NULL COMMENT '床号编号(如1号床、2号床)',
    `name` VARCHAR(100) NOT NULL COMMENT '床号名称',
    `colors` VARCHAR(500) DEFAULT NULL COMMENT '颜色',
    `quantity` INT NOT NULL COMMENT '该床总件数(创建时确定)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_style_id` (`style_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='床号表';

-- ==================== 扎货表 ====================
CREATE TABLE IF NOT EXISTS `bundle` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '所属公司',
    `style_id` BIGINT NOT NULL COMMENT '所属款式',
    `bed_id` BIGINT DEFAULT NULL COMMENT '所属床号',
    `bundle_no` VARCHAR(20) NOT NULL COMMENT '扎号(如B001)',
    `quantity` INT NOT NULL COMMENT '件数',
    `size` VARCHAR(20) DEFAULT NULL COMMENT '尺码',
    `qr_code` VARCHAR(64) NOT NULL COMMENT '二维码唯一标识(UUID)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待开始 1进行中 2已完成',
    `printed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已打印: 0未打印 1已打印',
    `shipped` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已出货: 0未出货 1已出货',
    `shipped_time` DATETIME DEFAULT NULL COMMENT '出货时间',
    `shipper_id` BIGINT DEFAULT NULL COMMENT '出货操作人ID',
    `creator_id` BIGINT NOT NULL COMMENT '创建人(主管ID)',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_qr_code` (`qr_code`),
    KEY `idx_style_id` (`style_id`),
    KEY `idx_bed_id` (`bed_id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_shipped_time` (`shipped_time`)
) ENGINE=InnoDB COMMENT='扎货表';

-- ==================== 扎货工序快照表 ====================
CREATE TABLE IF NOT EXISTS `bundle_process` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `bundle_id` BIGINT NOT NULL COMMENT '所属扎货',
    `company_id` BIGINT NOT NULL COMMENT '所属公司',
    `process_name` VARCHAR(100) NOT NULL COMMENT '工序名称(快照)',
    `price` DECIMAL(10,2) NOT NULL COMMENT '单价(快照)',
    `seq` INT NOT NULL DEFAULT 0 COMMENT '序号',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0未开始 1进行中 2已完成',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_bundle_id` (`bundle_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='扎货工序快照表';

-- ==================== 工资调整表 ====================
CREATE TABLE IF NOT EXISTS `wage_adjustment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `worker_id` BIGINT NOT NULL COMMENT '工人ID',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `type` TINYINT NOT NULL COMMENT '类型：1加钱 2扣钱',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
    `remark` VARCHAR(500) NOT NULL COMMENT '原因',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审批 1已通过 2已删除 3已驳回',
    `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `settlement_id` BIGINT DEFAULT NULL COMMENT '结算单ID(null=未结算)',
    `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_worker_id` (`worker_id`),
    KEY `idx_settlement_id` (`settlement_id`)
) ENGINE=InnoDB COMMENT='工资调整表';

-- ==================== 报工记录表 ====================
CREATE TABLE IF NOT EXISTS `work_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '所属公司',
    `worker_id` BIGINT NOT NULL COMMENT '报工工人ID',
    `bundle_id` BIGINT NOT NULL COMMENT '扎货ID',
    `bundle_process_id` BIGINT NOT NULL COMMENT '扎货工序ID',
    `quantity` INT NOT NULL COMMENT '完成数量',
    `price` DECIMAL(10,2) NOT NULL COMMENT '单价(快照)',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '小计=数量×单价',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态: 0待审核 1通过 2驳回 3已退回',
    `settlement_id` BIGINT DEFAULT NULL COMMENT '结算单ID(null=未结算)',
    `worker_remark` VARCHAR(500) DEFAULT NULL COMMENT '员工报工备注',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `return_reason` VARCHAR(500) DEFAULT NULL COMMENT '退回原因',
    `returner_id` BIGINT DEFAULT NULL COMMENT '退回人ID',
    `return_time` DATETIME DEFAULT NULL COMMENT '退回时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_worker_id` (`worker_id`),
    KEY `idx_bundle_id` (`bundle_id`),
    KEY `idx_settlement_id` (`settlement_id`)
) ENGINE=InnoDB COMMENT='报工记录表';

-- ==================== 工序模板表 ====================
CREATE TABLE IF NOT EXISTS `process_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='工序模板表';

-- ==================== 工序模板明细表 ====================
CREATE TABLE IF NOT EXISTS `process_template_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `name` VARCHAR(100) NOT NULL COMMENT '工序名称',
    `seq` INT NOT NULL DEFAULT 0 COMMENT '序号',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '计件单价',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='工序模板明细表';

-- ==================== 工资结算表 ====================
CREATE TABLE IF NOT EXISTS `wage_settlement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `start_date` VARCHAR(10) NOT NULL COMMENT '结算起始日期(YYYY-MM-DD)',
    `end_date` VARCHAR(10) NOT NULL COMMENT '结算截止日期(YYYY-MM-DD)',
    `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    `worker_count` INT NOT NULL DEFAULT 0 COMMENT '工人数',
    `confirmed_count` INT NOT NULL DEFAULT 0 COMMENT '已确认人数',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待结算 1已结算',
    `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='工资结算表';

-- ==================== 工资结算明细表 ====================
CREATE TABLE IF NOT EXISTS `wage_settlement_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `settlement_id` BIGINT NOT NULL COMMENT '结算单ID',
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `worker_id` BIGINT NOT NULL COMMENT '工人ID',
    `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '完成件数',
    `base_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '计件工资',
    `supplement_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '调整工资',
    `final_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应发工资',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待确认 1已确认',
    `visible_to_employee` TINYINT NOT NULL DEFAULT 1 COMMENT '员工是否可见：0不可见 1可见',
    `employee_confirmed` TINYINT NOT NULL DEFAULT 0 COMMENT '员工是否已确认：0未确认 1已确认',
    `employee_confirm_time` DATETIME DEFAULT NULL COMMENT '员工确认时间',
    `confirmed_time` DATETIME DEFAULT NULL COMMENT '老板确认时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_settlement_id` (`settlement_id`),
    KEY `idx_worker_id` (`worker_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB COMMENT='工资结算明细表';

-- ==================== 通知表 ====================
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送人ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `target_type` TINYINT NOT NULL DEFAULT 0 COMMENT '接收范围：0全部 1仅主管 2仅工人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_sender_id` (`sender_id`)
) ENGINE=InnoDB COMMENT='通知表';

-- ==================== 通知已读表 ====================
CREATE TABLE IF NOT EXISTS `notification_read` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `notification_id` BIGINT NOT NULL COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_user` (`notification_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='通知已读表';

-- ==================== 初始数据 ====================
-- 超级管理员账号通过后端 DataInitializer 自动创建，初始密码来自环境变量或首次启动日志
