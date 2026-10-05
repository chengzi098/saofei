# 部署指南

## 一、服务器环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 17+（推荐 21） |
| MySQL | 8.0+ |
| Node.js | 18+（仅构建时需要） |
| Nginx | 任意稳定版（反向代理前端） |

---

## 二、部署步骤

### 1. 数据库

```sql
-- 创建数据库
CREATE DATABASE garment_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入建表脚本
mysql -u root -p garment_db < schema.sql
```

### 2. 后端

```bash
# 打包
cd garment-server
mvn clean package -DskipTests

# 上传 target/garment-server-0.0.1-SNAPSHOT.jar 到服务器

# 设置环境变量（生产环境必须改！）
export DB_USERNAME=garment_user
export DB_PASSWORD=你的安全密码
export JWT_SECRET=你的JWT密钥（至少32位随机字符串）

# 启动
java -jar garment-server-0.0.1-SNAPSHOT.jar
```

### 3. 前端

```bash
cd garment-web

# 修改 API 地址（指向你的服务器）
# vite.config.js 中修改 proxy 目标地址

npm run build

# 将 dist/ 目录上传到服务器，用 Nginx 托管
```

### 4. Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/garment-web/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 三、生产环境安全清单

### 必须修改（启动前）

| 项目 | 当前值 | 操作 |
|------|--------|------|
| 数据库密码 | 不写入仓库 | 设置环境变量 `DB_PASSWORD` |
| 数据库用户 | `root` | 创建专用用户 `garment_user`，只授予 `garment_db` 权限 |
| JWT 密钥 | `garment-factory-jwt-secret-key...` | 设置环境变量 `JWT_SECRET`（随机32位+） |
| admin 密码 | 不写入仓库 | 设置 `ADMIN_INIT_PASSWORD` 或读取首次启动日志 |
| CORS 配置 | 允许局域网 | 改为你的域名 |

### 建议修改

| 项目 | 说明 |
|------|------|
| SQL 日志 | `application.yml` 注释掉 `log-impl` 那行 |
| 身份证/银行卡 | 后续考虑加密存储 |
| 数据库备份 | 设置定时备份（见下方） |

---

## 四、创建数据库专用用户（MySQL）

```sql
-- 创建专用用户（不要用 root 运行应用）
CREATE USER 'garment_user'@'localhost' IDENTIFIED BY '你的安全密码';

-- 只授予 garment_db 的权限
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX ON garment_db.* TO 'garment_user'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;
```

---

## 五、数据库定时备份

### Linux（crontab）

```bash
# 上传备份脚本
scp backup-linux.sh root@your-server:/opt/garment/backup.sh

# 赋予执行权限
chmod +x /opt/garment/backup.sh

# 添加定时任务（每天凌晨3点）
crontab -e
# 添加这行：
0 3 * * * /opt/garment/backup.sh >> /opt/garment/backup.log 2>&1
```

### Windows Server（计划任务）

1. 打开「任务计划程序」
2. 创建基本任务 → 每天 → 凌晨 3:00
3. 启动程序：`D:\garment\backup-db.bat`

---

## 六、后台运行（Linux）

### 方式一：systemd 服务（推荐）

```bash
# 创建服务文件
cat > /etc/systemd/system/garment.service << 'EOF'
[Unit]
Description=Garment Management System
After=network.target mysql.service

[Service]
Type=simple
User=garment
WorkingDirectory=/opt/garment
Environment=DB_USERNAME=garment_user
Environment=DB_PASSWORD=你的安全密码
Environment=JWT_SECRET=你的JWT密钥
ExecStart=/usr/bin/java -jar garment-server.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 启用并启动
systemctl daemon-reload
systemctl enable garment
systemctl start garment
systemctl status garment
```

### 方式二：nohup（简单）

```bash
nohup java -jar garment-server.jar > app.log 2>&1 &
```

---

## 七、防火墙

```bash
# 开放 80 端口（Nginx）
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --reload

# 8080 端口不需要对外开放（Nginx 内部代理）
```
