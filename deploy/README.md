# 制衣厂管理系统 - 部署指南

## 服务器信息
- **IP**: 101.34.209.192
- **系统**: Ubuntu 22.04 / 24.04
- **配置**: 4核 4G内存 40G硬盘

## 一键部署步骤

### 第一步：本地打包（Windows）

双击运行 `build.bat`，等待打包完成。

打包完成后会生成 `garment-deploy.zip` 文件。

### 第二步：上传到服务器

使用 SCP 或其他工具上传 `garment-deploy.zip` 到服务器：

```bash
scp garment-deploy.zip ubuntu@101.34.209.192:/tmp/
```

### 第三步：安装服务器环境

SSH 登录服务器：

```bash
ssh ubuntu@101.34.209.192
```

执行环境安装脚本：

```bash
# 切换到 root 用户
sudo su -

# 安装环境
bash /tmp/setup-server.sh
```

### 第四步：部署项目

```bash
# 解压部署文件
mkdir -p /opt/garment/backend
cd /opt/garment/backend
unzip /tmp/garment-deploy.zip

# 执行部署脚本
bash deploy.sh
```

### 第五步：访问系统

打开浏览器访问：**http://101.34.209.192**

初始管理员账号为 `admin`，密码来自 `ADMIN_INIT_PASSWORD` 或首次启动日志。

---

## 常用命令

```bash
# 查看后端服务状态
systemctl status garment

# 重启后端服务
systemctl restart garment

# 查看后端日志
tail -f /opt/garment/logs/backend.log

# 重启 Nginx
systemctl restart nginx

# 查看 MySQL 状态
systemctl status mysql
```

## 更新部署

当代码有更新时：

1. 本地重新运行 `build.bat` 打包
2. 上传新的 `garment-deploy.zip` 到服务器
3. 执行更新命令：

```bash
cd /opt/garment/backend
unzip -o /tmp/garment-deploy.zip
systemctl restart garment
```

## 数据库备份

建议定期备份数据库：

```bash
# 手动备份
mysqldump -u root -p garment_db > /opt/garment/backup/garment_db_$(date +%Y%m%d).sql

# 自动备份（每天凌晨3点）
crontab -e
# 添加: 0 3 * * * mysqldump -u root -p garment_db > /opt/garment/backup/garment_db_$(date +\%Y\%m\%d).sql
```

## 常见问题

### 1. 无法访问网站
- 检查防火墙：`ufw status`
- 检查 Nginx：`systemctl status nginx`
- 检查后端：`systemctl status garment`

### 2. 数据库连接失败
- 检查 MySQL：`systemctl status mysql`
- 检查密码是否正确
- 检查数据库是否初始化

### 3. 端口被占用
```bash
# 查看端口占用
netstat -tlnp | grep 8080
netstat -tlnp | grep 80
```
