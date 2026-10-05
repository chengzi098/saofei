#!/bin/bash
# ==================== 制衣厂管理系统 - 服务器环境安装脚本 ====================
# 适用于 Ubuntu 22.04 / 24.04
# 使用方法：sudo bash setup-server.sh

set -e

: "${MYSQL_ROOT_PASSWORD:?请先设置 MYSQL_ROOT_PASSWORD 环境变量}"

echo "=============================="
echo "  制衣厂管理系统 - 环境安装"
echo "=============================="

# 1. 更新系统
echo "[1/5] 更新系统包..."
apt update && apt upgrade -y

# 2. 安装 JDK 17 (Spring Boot 3.x 需要 JDK 17+)
echo "[2/5] 安装 JDK 17..."
apt install -y openjdk-17-jdk

# 验证 Java 安装
java -version
echo "JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))" >> /etc/environment
source /etc/environment

# 3. 安装 MySQL 8
echo "[3/5] 安装 MySQL 8..."
apt install -y mysql-server

# 启动 MySQL
systemctl start mysql
systemctl enable mysql

# 设置 root 密码和远程访问
echo "配置 MySQL..."
mysql -u root <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';
FLUSH PRIVILEGES;
EOF

# 修改 MySQL 配置允许远程连接
sed -i 's/bind-address.*/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf
systemctl restart mysql

# 4. 安装 Nginx
echo "[4/5] 安装 Nginx..."
apt install -y nginx

systemctl start nginx
systemctl enable nginx

# 5. 安装常用工具
echo "[5/5] 安装常用工具..."
apt install -y curl wget git unzip

echo ""
echo "=============================="
echo "  环境安装完成！"
echo "=============================="
echo ""
echo "已安装："
echo "  - JDK 17"
echo "  - MySQL 8（root 密码来自 MYSQL_ROOT_PASSWORD）"
echo "  - Nginx"
echo ""
echo "下一步：上传项目文件并执行 deploy.sh"
