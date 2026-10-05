# 服务器部署指南

## 一、服务器要求

| 项目 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 1 核 | 2 核 |
| 内存 | 1 GB | 2 GB |
| 硬盘 | 20 GB | 40 GB |
| 系统 | Ubuntu 22.04 | Ubuntu 22.04 |

## 二、需要准备的东西

1. **服务器**：阿里云/腾讯云买一个
2. **域名**：阿里云/腾讯云买一个（可选，用 IP 也能访问）
3. **数据库**：MySQL 8.0（装在服务器上）

## 三、服务器环境安装

SSH 登录服务器后执行：

```bash
# 1. 更新系统
sudo apt update && sudo apt upgrade -y

# 2. 安装 MySQL 8.0
sudo apt install mysql-server -y
sudo mysql_secure_installation

# 3. 安装 Nginx
sudo apt install nginx -y

# 4. 安装 Java 17（Spring Boot 3 需要）
sudo apt install openjdk-17-jdk -y

# 5. 安装 Node.js 18（用于前端打包）
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install nodejs -y
```

## 四、数据库配置

```bash
# 登录 MySQL
sudo mysql -u root -p

# 创建数据库和用户
CREATE DATABASE garment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'garment'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON garment_db.* TO 'garment'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 导入建表 SQL
mysql -u garment -p garment_db < /opt/garment/schema.sql
```

## 五、部署后端

```bash
# 创建目录
sudo mkdir -p /opt/garment/server
sudo mkdir -p /opt/garment/logs

# 上传打包好的 jar 文件到 /opt/garment/server/
# 本地打包：cd garment-server && mvn clean package -DskipTests

# 创建配置文件
sudo nano /opt/garment/server/application-prod.yml
```

配置文件内容：

```yaml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/garment_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: garment
    password: 你的密码

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  secret: 换一个复杂的密钥
  expiration: 86400000
```

启动后端：

```bash
cd /opt/garment/server
nohup java -jar garment-server-1.0.0.jar --spring.profiles.active=prod > /opt/garment/logs/server.log 2>&1 &
```

## 六、部署前端

```bash
# 本地打包前端
cd garment-web
npm run build

# 上传 dist 目录到服务器的 /opt/garment/web/
```

配置 Nginx：

```bash
sudo nano /etc/nginx/sites-available/garment
```

Nginx 配置内容：

```nginx
server {
    listen 80;
    server_name 你的域名或IP;

    # 前端静态文件
    location / {
        root /opt/garment/web/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

启用配置：

```bash
sudo ln -s /etc/nginx/sites-available/garment /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## 七、验证部署

打开浏览器访问：`http://你的服务器IP`

初始账号为 `admin`，密码来自 `ADMIN_INIT_PASSWORD` 或首次启动日志。

## 八、开机自启（可选）

创建 systemd 服务：

```bash
sudo nano /etc/systemd/system/garment.service
```

内容：

```ini
[Unit]
Description=Garment Factory Management System
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/garment/server
ExecStart=/usr/bin/java -jar garment-server-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable garment
sudo systemctl start garment
```

## 九、常见问题

### 1. 后端启动失败
查看日志：`tail -f /opt/garment/logs/server.log`

### 2. 数据库连接失败
检查 MySQL 是否启动：`sudo systemctl status mysql`

### 3. 前端页面空白
检查 Nginx 配置：`sudo nginx -t`

### 4. 无法访问
检查防火墙：`sudo ufw allow 80/tcp`

## 十、后续微信公众号对接

等服务器部署好后，需要：
1. 域名备案（国内服务器必须）
2. 配置 SSL 证书（HTTPS）
3. 在公众号后台配置网页授权域名
4. 我来改代码对接微信

---

**部署过程中遇到问题，把错误信息发给我，我帮你解决。**
