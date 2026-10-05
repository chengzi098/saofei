# 部署文档

制衣厂管理系统（缝迹）部署指南与已知问题记录。

---

## 一、环境要求

| 组件 | 版本要求 |
|------|---------|
| Java | 21+（后端按 Java 21 编译，服务器也必须安装 Java 21） |
| Maven | 3.6+ |
| Node.js | 18+ |
| MySQL | 8.0+ |

---

## 二、后端部署（garment-server）

### 1. 数据库初始化

```sql
CREATE DATABASE garment_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

建表脚本位于 `garment-server/src/main/resources/db/schema.sql`，导入后执行。

### 2. 修改数据库连接

编辑 `garment-server/src/main/resources/application.yml`，修改数据库地址、用户名、密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/garment_db?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 3. 编译与启动

```bash
cd garment-server
mvn compile
mvn spring-boot:run
```

后端默认端口：`8080`

### 4. 初始管理员账号

账号为 `admin`。密码通过 `ADMIN_INIT_PASSWORD` 提供；未提供时会随机生成并只在首次启动日志中显示。

---

## 三、前端部署（garment-web）

### 1. 安装依赖

```bash
cd garment-web
npm install
```

### 2. 开发模式

```bash
npm run dev
```

前端默认端口：`5173`

### 3. 生产构建

```bash
npm run build
```

构建产物输出到 `garment-web/dist/`，用 Nginx 或其他 Web 服务器托管。

### 4. Nginx 参考配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/garment-web/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 四、局域网部署（工厂场景）

工厂内网环境，需让手机和电脑在同一局域网下。

### 1. 查看电脑 IP

```bash
# Windows
ipconfig

# Linux/Mac
ifconfig
```

假设电脑 IP 为 `192.168.10.83`。

### 2. 前端启动时绑定局域网

开发模式下 Vite 默认只监听 localhost，需绑定 `0.0.0.0` 让局域网设备访问。

编辑 `garment-web/vite.config.js`：

```js
export default defineConfig({
  server: {
    host: '0.0.0.0',
    port: 5173
  }
})
```

### 3. 手机访问地址

```
http://192.168.10.83:5173
```

---

## 五、已知问题

### ✅ 2026-06-17 线上部署问题记录，下次上传前必须检查

1. **后端运行 Java 版本必须是 21+**
   - 现象：`UnsupportedClassVersionError ... class file version 65.0`
   - 原因：`garment-server/pom.xml` 使用 `<java.version>21</java.version>`
   - 检查：`java -version`
   - 修复：服务器安装 `openjdk-21-jdk-headless`，`garment.service` 使用 `/usr/lib/jvm/java-21-openjdk-amd64/bin/java`

2. **systemd 必须带生产环境变量**
   - 必须配置：`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`
   - 否则后端生产配置 `application-prod.yml` 读取不到数据库和 JWT 配置

3. **上传 zip 的目录结构是顶层 `dist/`，不是 `frontend/dist/`**
   - 前端部署应复制：`/opt/garment/backend/dist/.` 到 `/var/www/garment/`
   - 不要复制 `/opt/garment/frontend/dist`

4. **上线前必须执行数据库迁移**
   - 线上已发生：`notification.target_type` 缺失导致公告查询报错
   - 线上已发生：`notification_read.read_time` 与代码字段不一致
   - 每次上传后执行：
     ```bash
     mysql -u root -p < /opt/garment/backend/migration.sql
     ```

5. **HTTPS 域名必须保留**
   - 手机摄像头 API 需要 HTTPS 或 localhost，生产访问使用：
     - `https://www.fjsaofei.cn/`
     - `https://fjsaofei.cn/`
   - 不要用 `http://43.130.31.244` 测手机扫码摄像头，IP HTTPS 也会证书不匹配

6. **CORS 必须允许线上 HTTPS 域名**
   - 已在 `CorsConfig` 放行：
     - `https://www.fjsaofei.cn`
     - `https://fjsaofei.cn`
   - 现象：登录请求 `/api/auth/login` 返回 403，多半是 CORS Origin 未放行

7. **手机扫码页必须保持 video 元素挂载**
   - 已修复：`MobileScan.vue` 和 `MobileSupervisorScanBundle.vue` 的 `<video>` 使用 `v-show`，不能用 `v-if="cameraStatus === 'ready'"`
   - 原因：摄像头流需要先挂到 `videoRef`，再切换 ready；用 `v-if` 会导致 video 未挂载，手机端无法打开摄像头
   - 前端构建前运行：
     ```bash
     cd garment-web
     npm run test:scanner
     npm run build
     ```

### ❌ 手机原生相机扫码无法直接使用

**状态**：应用内扫码已修复；原生相机扫码跳转仍建议使用 HTTPS 域名测试。

**描述**：
二维码内容为完整 URL（如 `http://192.168.10.83:5173/mobile/scan?code=xxx`），理论上手机原生相机扫码后会跳转浏览器打开该页面。但目前实际测试中，手机原生相机扫码后无法正常使用，只能在应用内通过拍照上传识别二维码。

**临时解决方案**：
使用应用内的「拍照扫描」功能，拍摄二维码图片后由 jsQR 库识别。

**可能原因**：
- 手机浏览器直接访问 `http://192.168.10.83:5173` 时，摄像头 API（`navigator.mediaDevices`）需要 HTTPS 或 localhost 才能使用，HTTP 环境下不可用
- 手机原生相机跳转浏览器后，页面可能无法正确加载或执行 JavaScript

**后续排查方向**：
- 确认手机浏览器直接打开 URL 后页面是否正常渲染
- 检查浏览器控制台是否有报错
- 考虑使用 HTTPS 部署（自签名证书或 Let's Encrypt）
