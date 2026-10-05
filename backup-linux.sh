#!/bin/bash
# ============================
# 制衣厂管理系统 - Linux 数据库自动备份脚本
# 用法：chmod +x backup-linux.sh && crontab -e 添加：0 3 * * * /opt/garment/backup.sh
# ============================

# 配置区（根据实际情况修改）
DB_USER="garment_user"
DB_PASSWORD="你的安全密码"
DB_NAME="garment_db"
BACKUP_DIR="/opt/garment/backup"
KEEP_DAYS=7

# 生成日期
DATE_STR=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${DATE_STR}.sql"

# 创建备份目录
mkdir -p "$BACKUP_DIR"

echo "$(date '+%Y-%m-%d %H:%M:%S') 开始备份..."

# 执行备份
mysqldump -u"$DB_USER" -p"$DB_PASSWORD" --single-transaction --routines --triggers "$DB_NAME" > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    # 压缩
    gzip "$BACKUP_FILE"
    echo "$(date '+%Y-%m-%d %H:%M:%S') 备份成功: ${BACKUP_FILE}.gz"
else
    echo "$(date '+%Y-%m-%d %H:%M:%S') 备份失败！"
    exit 1
fi

# 清理旧备份
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$KEEP_DAYS -delete
echo "$(date '+%Y-%m-%d %H:%M:%S') 已清理 ${KEEP_DAYS} 天前的备份"

echo "$(date '+%Y-%m-%d %H:%M:%S') 备份完成"
