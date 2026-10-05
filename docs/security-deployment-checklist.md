# 安全部署检查清单

部署前必须完成：

- 设置 `DB_USERNAME` 和新的 `DB_PASSWORD`。
- 设置至少 32 字节随机值的 `JWT_SECRET`。
- 首次部署设置 `ADMIN_INIT_PASSWORD`，登录后立即修改。
- 每台打印电脑设置独立随机值 `PRINT_AGENT_KEY`。
- 如业务域名发生变化，通过 `PRINT_AGENT_ORIGINS` 配置允许来源。
- 确认打印代理只监听 `127.0.0.1:9876`。
- 不在命令行、文档、聊天记录或 Git 中保存真实密码。

本次代码清理不能自动更换已经部署的密钥。生产服务器上的数据库密码和 JWT 密钥必须人工轮换，轮换后重启后端服务。

示例：

```bash
export DB_USERNAME=garment_user
export DB_PASSWORD='replace-with-a-new-random-password'
export JWT_SECRET='replace-with-at-least-32-random-bytes'
export ADMIN_INIT_PASSWORD='replace-with-a-one-time-password'
```

打印电脑：

```powershell
$env:PRINT_AGENT_KEY = 'replace-with-a-long-random-value'
$env:PRINT_AGENT_ORIGINS = 'https://www.fjsaofei.cn,https://fjsaofei.cn'
npm start
```
