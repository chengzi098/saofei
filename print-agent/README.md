# CX 热敏打印机 - 本地打印服务

## 简介

这是一个本地打印代理服务，让网页直接向 CX 热敏打印机发送 ESC/POS 指令，跳过浏览器的打印对话框。

## 使用方法

### 首次运行

```bash
cd print-agent
npm install    # 安装依赖
set PRINT_AGENT_KEY=请替换为随机长字符串
npm start      # 启动服务
```

或直接双击 `start.bat`（会自动安装依赖）。

### 启动后

1. 服务只监听 `http://127.0.0.1:9876`
2. 打开前端页面，进入"二维码打印"页面
3. 页面右上角会显示"打印服务已连接"（绿色）
4. 选择打印机（自动识别 CX-805），点击"打印全部"或单张打印

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/status` | 服务状态检查 |
| GET | `/api/printers` | 列出 Windows 已安装的打印机 |
| POST | `/api/print` | 批量打印标签 |
| POST | `/api/print-one` | 打印单张标签 |

除 `/api/status` 外，请求必须携带：

```http
X-Print-Agent-Key: 与 PRINT_AGENT_KEY 相同的值
```

可通过 `PRINT_AGENT_ORIGINS` 配置允许访问代理的网页来源，多个来源用逗号分隔。

### POST /api/print

```json
{
  "printerName": "CX-805",
  "labels": [
    {
      "companyName": "公司名",
      "styleNo": "A001",
      "bedNo": "1",
      "bundleNo": "001",
      "quantity": 50,
      "size": "M",
      "colors": "红色",
      "qrCode": "B01-001-001"
    }
  ],
  "qrBaseUrl": "http://101.34.209.192"
}
```

## 注意事项

- 需要 Windows 系统，且 CX 打印机驱动已安装
- 启动前必须设置 `PRINT_AGENT_KEY`
- 打印机名称可通过 `GET /api/printers` 获取
- 标签纸尺寸：40mm × 80mm
