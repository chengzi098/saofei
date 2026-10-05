USE garment_db;

-- Add process price lock flag for existing databases.
SET @has_process_price_locked = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'process'
      AND column_name = 'price_locked'
);

SET @add_process_price_locked_sql = IF(
    @has_process_price_locked = 0,
    'ALTER TABLE `process` ADD COLUMN `price_locked` TINYINT NOT NULL DEFAULT 0 COMMENT ''价格是否已锁定：0未锁定 1已锁定'' AFTER `price`',
    'SELECT 1'
);

PREPARE add_process_price_locked FROM @add_process_price_locked_sql;
EXECUTE add_process_price_locked;
DEALLOCATE PREPARE add_process_price_locked;

SET @backfill_process_price_locked_sql = IF(
    @has_process_price_locked = 0,
    'UPDATE `process` SET `price_locked` = 1 WHERE `price` > 0',
    'SELECT 1'
);

PREPARE backfill_process_price_locked FROM @backfill_process_price_locked_sql;
EXECUTE backfill_process_price_locked;
DEALLOCATE PREPARE backfill_process_price_locked;

-- Add notification target filter for existing databases.
SET @has_notification_target_type = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'target_type'
);

SET @add_notification_target_type_sql = IF(
    @has_notification_target_type = 0,
    'ALTER TABLE `notification` ADD COLUMN `target_type` TINYINT NOT NULL DEFAULT 0 COMMENT ''目标类型：0全部 1主管 2工人'' AFTER `content`',
    'SELECT 1'
);

PREPARE add_notification_target_type FROM @add_notification_target_type_sql;
EXECUTE add_notification_target_type;
DEALLOCATE PREPARE add_notification_target_type;

-- Add notification read timestamp for existing databases.
SET @has_notification_read_time = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification_read'
      AND column_name = 'read_time'
);

SET @add_notification_read_time_sql = IF(
    @has_notification_read_time = 0,
    'ALTER TABLE `notification_read` ADD COLUMN `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''阅读时间'' AFTER `user_id`',
    'SELECT 1'
);

PREPARE add_notification_read_time FROM @add_notification_read_time_sql;
EXECUTE add_notification_read_time;
DEALLOCATE PREPARE add_notification_read_time;

SET @has_notification_read_create_time = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification_read'
      AND column_name = 'create_time'
);

SET @backfill_notification_read_sql = IF(
    @has_notification_read_time = 0 AND @has_notification_read_create_time > 0,
    'UPDATE `notification_read` SET `read_time` = `create_time` WHERE `create_time` IS NOT NULL',
    'SELECT 1'
);

PREPARE backfill_notification_read FROM @backfill_notification_read_sql;
EXECUTE backfill_notification_read;
DEALLOCATE PREPARE backfill_notification_read;

-- Add bundle process completion/operator fields for existing databases.
SET @has_bundle_process_operator_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bundle_process'
      AND column_name = 'operator_id'
);

SET @add_bundle_process_operator_id_sql = IF(
    @has_bundle_process_operator_id = 0,
    'ALTER TABLE `bundle_process` ADD COLUMN `operator_id` BIGINT DEFAULT NULL COMMENT ''operator id'' AFTER `status`',
    'SELECT 1'
);

PREPARE add_bundle_process_operator_id FROM @add_bundle_process_operator_id_sql;
EXECUTE add_bundle_process_operator_id;
DEALLOCATE PREPARE add_bundle_process_operator_id;

SET @has_bundle_process_operator_name = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bundle_process'
      AND column_name = 'operator_name'
);

SET @add_bundle_process_operator_name_sql = IF(
    @has_bundle_process_operator_name = 0,
    'ALTER TABLE `bundle_process` ADD COLUMN `operator_name` VARCHAR(50) DEFAULT NULL COMMENT ''operator name'' AFTER `operator_id`',
    'SELECT 1'
);

PREPARE add_bundle_process_operator_name FROM @add_bundle_process_operator_name_sql;
EXECUTE add_bundle_process_operator_name;
DEALLOCATE PREPARE add_bundle_process_operator_name;

SET @has_bundle_process_complete_time = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bundle_process'
      AND column_name = 'complete_time'
);

SET @add_bundle_process_complete_time_sql = IF(
    @has_bundle_process_complete_time = 0,
    'ALTER TABLE `bundle_process` ADD COLUMN `complete_time` DATETIME DEFAULT NULL COMMENT ''complete time'' AFTER `create_time`',
    'SELECT 1'
);

PREPARE add_bundle_process_complete_time FROM @add_bundle_process_complete_time_sql;
EXECUTE add_bundle_process_complete_time;
DEALLOCATE PREPARE add_bundle_process_complete_time;

-- Make bed name required for existing databases. Backfill old empty names first.
UPDATE `bed`
SET `name` = `bed_no`
WHERE `name` IS NULL OR TRIM(`name`) = '';

ALTER TABLE `bed` MODIFY COLUMN `name` VARCHAR(100) NOT NULL COMMENT 'bed name';

-- Add logical-delete column required by ProcessTemplateItem for existing databases.
SET @has_process_template_item_deleted = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'process_template_item'
      AND column_name = 'deleted'
);

SET @add_process_template_item_deleted_sql = IF(
    @has_process_template_item_deleted = 0,
    'ALTER TABLE `process_template_item` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''logical delete'' AFTER `price`',
    'SELECT 1'
);

PREPARE add_process_template_item_deleted FROM @add_process_template_item_deleted_sql;
EXECUTE add_process_template_item_deleted;
DEALLOCATE PREPARE add_process_template_item_deleted;

ALTER TABLE `process_template_item`
MODIFY COLUMN `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '计件单价';

-- Add shipped flag for existing databases (主管扫码出货功能).
SET @has_bundle_shipped = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bundle'
      AND column_name = 'shipped'
);

SET @add_bundle_shipped_sql = IF(
    @has_bundle_shipped = 0,
    'ALTER TABLE `bundle` ADD COLUMN `shipped` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否已出货: 0未出货 1已出货'' AFTER `printed`',
    'SELECT 1'
);

PREPARE add_bundle_shipped FROM @add_bundle_shipped_sql;
EXECUTE add_bundle_shipped;
DEALLOCATE PREPARE add_bundle_shipped;

-- Add shipped_time for existing databases.
SET @has_bundle_shipped_time = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bundle'
      AND column_name = 'shipped_time'
);

SET @add_bundle_shipped_time_sql = IF(
    @has_bundle_shipped_time = 0,
    'ALTER TABLE `bundle` ADD COLUMN `shipped_time` DATETIME DEFAULT NULL COMMENT ''出货时间'' AFTER `shipped`',
    'SELECT 1'
);

PREPARE add_bundle_shipped_time FROM @add_bundle_shipped_time_sql;
EXECUTE add_bundle_shipped_time;
DEALLOCATE PREPARE add_bundle_shipped_time;

-- Add shipper_id for existing databases.
SET @has_bundle_shipper_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bundle'
      AND column_name = 'shipper_id'
);

SET @add_bundle_shipper_id_sql = IF(
    @has_bundle_shipper_id = 0,
    'ALTER TABLE `bundle` ADD COLUMN `shipper_id` BIGINT DEFAULT NULL COMMENT ''出货操作人ID'' AFTER `shipped_time`',
    'SELECT 1'
);

PREPARE add_bundle_shipper_id FROM @add_bundle_shipper_id_sql;
EXECUTE add_bundle_shipper_id;
DEALLOCATE PREPARE add_bundle_shipper_id;

SELECT 'Migration completed.' AS result;
