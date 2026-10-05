import express from 'express'
import cors from 'cors'
import { execFile } from 'child_process'
import { Buffer } from 'buffer'
import { writeFileSync, unlinkSync } from 'fs'
import { tmpdir } from 'os'
import { join } from 'path'
import {
  isAllowedOrigin,
  isValidAgentKey,
  isValidPrinterPort,
  validatePrintPayload
} from './security.js'

const app = express()
const PORT = 9876
const HOST = '127.0.0.1'
const AGENT_KEY = process.env.PRINT_AGENT_KEY
const ALLOWED_ORIGINS = (process.env.PRINT_AGENT_ORIGINS
  || 'https://www.fjsaofei.cn,https://fjsaofei.cn,http://localhost:5173,http://127.0.0.1:5173')
  .split(',')
  .map(value => value.trim())
  .filter(Boolean)

if (!AGENT_KEY) {
  console.error('PRINT_AGENT_KEY 未设置，打印代理拒绝启动')
  process.exit(1)
}

app.use(cors({
  origin(origin, callback) {
    const allowed = isAllowedOrigin(origin, ALLOWED_ORIGINS)
    callback(allowed ? null : new Error('Origin not allowed'), allowed)
  }
}))
app.use(express.json({ limit: '2mb' }))

function requireAgentKey(req, res, next) {
  if (!isValidAgentKey(req.get('X-Print-Agent-Key'), AGENT_KEY)) {
    return res.status(401).json({ error: '打印代理密钥无效' })
  }
  next()
}

// ========== ESC/POS 指令 ==========

const ESC = {
  INIT:         Buffer.from([0x1B, 0x40]),       // 初始化
  CENTER:       Buffer.from([0x1B, 0x61, 0x01]), // 居中
  LEFT:         Buffer.from([0x1B, 0x61, 0x00]), // 左对齐
  BOLD_ON:      Buffer.from([0x1B, 0x45, 0x01]), // 加粗开
  BOLD_OFF:     Buffer.from([0x1B, 0x45, 0x00]), // 加粗关
  FONT_SIZE:    (w, h) => Buffer.from([0x1D, 0x21, ((w - 1) << 4) | (h - 1)]),
  LINE_SPACING: (n) => Buffer.from([0x1B, 0x33, n]), // 设置行间距
  FEED:         (n) => Buffer.from([0x1B, 0x64, n]),  // 走纸 n 行
  CUT:          Buffer.from([0x1D, 0x56, 0x00]),       // 切纸
  // QR Code: GS ( k pL pH cn fn [data]
  QR_RESET:     Buffer.from([0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x41, 0x31, 0x00]),
  QR_SIZE:      (size) => Buffer.from([0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size]),
  QR_EC:        Buffer.from([0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30]),
  QR_STORE:     (data) => {
    const len = data.length + 3
    const pL = len & 0xFF
    const pH = (len >> 8) & 0xFF
    return Buffer.concat([
      Buffer.from([0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30]),
      Buffer.from(data, 'utf8')
    ])
  },
  QR_PRINT:     Buffer.from([0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30]),
}

/**
 * 构造单张标签的 ESC/POS 指令缓冲区
 */
function buildLabelBuffer(label, qrUrl) {
  const parts = []

  // 初始化 + 设置行间距（紧凑）
  parts.push(ESC.INIT)
  parts.push(ESC.LINE_SPACING(20))

  // ---- 文字部分 ----
  // 公司名（居中、加粗、1.5倍字号）
  parts.push(ESC.CENTER)
  parts.push(ESC.BOLD_ON)
  parts.push(ESC.FONT_SIZE(2, 2))
  parts.push(Buffer.from(label.companyName || '', 'utf8'))
  parts.push(Buffer.from([0x0A])) // 换行
  parts.push(ESC.BOLD_OFF)
  parts.push(ESC.FONT_SIZE(1, 1))
  parts.push(Buffer.from([0x0A])) // 空行

  // 详细信息（左对齐）
  parts.push(ESC.LEFT)
  parts.push(ESC.FONT_SIZE(1, 1))
  const infoLines = [
    `款号: ${label.styleNo || '-'}`,
    `床号: ${label.bedNo || '-'}`,
    `扎号: ${label.bundleNo}`,
    `件数: ${label.quantity}`,
    `码数: ${label.size || '-'}`,
    `颜色: ${label.colors || '-'}`,
  ]
  for (const line of infoLines) {
    parts.push(ESC.BOLD_ON)
    parts.push(Buffer.from(line, 'utf8'))
    parts.push(ESC.BOLD_OFF)
    parts.push(Buffer.from([0x0A]))
  }

  parts.push(Buffer.from([0x0A])) // 空行

  // ---- 二维码部分 ----
  parts.push(ESC.CENTER)
  parts.push(ESC.QR_RESET)           // 重置 QR
  parts.push(ESC.QR_SIZE(6))         // 模块大小 6（适合 40mm 标签）
  parts.push(ESC.QR_EC)              // 纠错等级 L
  parts.push(ESC.QR_STORE(qrUrl))    // 存储数据
  parts.push(ESC.QR_PRINT)           // 打印 QR

  // 走纸 + 切纸
  parts.push(ESC.FEED(3))

  return Buffer.concat(parts)
}

/**
 * 批量标签缓冲区（不切纸，最后一张才切）
 */
function buildBatchBuffer(labels, getQrUrl) {
  const parts = []
  for (let i = 0; i < labels.length; i++) {
    const b = labels[i]
    const qrUrl = getQrUrl(b.qrCode)
    const buf = buildLabelBuffer(b, qrUrl)
    parts.push(buf)
    // 最后一张切纸
    if (i === labels.length - 1) {
      parts.push(ESC.CUT)
    }
  }
  return Buffer.concat(parts)
}

// ========== Windows 打印机操作 ==========

function getPrinters() {
  return new Promise((resolve, reject) => {
    const ps = 'Get-Printer | Select-Object Name,DriverName,PrinterStatus,PortName | ConvertTo-Json -Compress'
    execFile('powershell.exe', ['-NoProfile', '-Command', ps], { encoding: 'utf8' }, (err, stdout, stderr) => {
      if (err) return reject(new Error(stderr || err.message))
      try {
        let data = JSON.parse(stdout.trim())
        if (!Array.isArray(data)) data = [data]
        resolve(data.map(p => ({
          name: p.Name,
          driver: p.DriverName,
          status: p.PrinterStatus === 0 ? 'ready' : 'other',
          port: p.PortName
        })))
      } catch (e) {
        reject(new Error('解析打印机列表失败'))
      }
    })
  })
}

// ========== Windows 打印机操作 ==========

/**
 * 通过 Windows Spooler 的 WritePrinter API 发送原始 ESC/POS 字节
 * 不经过打印机驱动翻译，指令直接送达打印机
 */
/**
 * 通过直接写入打印机端口发送 ESC/POS 数据
 */
async function printRawSimple(port, buffer) {
  const tmpFile = join(tmpdir(), `label_${Date.now()}.bin`)
  writeFileSync(tmpFile, buffer)

  try {
    if (!isValidPrinterPort(port)) throw new Error('打印机端口无效')
    // 通过 COPY /B 命令直接将二进制数据发送到打印机端口
    await new Promise((resolve, reject) => {
      execFile('cmd.exe', ['/d', '/s', '/c', `COPY /B "${tmpFile}" ${port}`],
        { encoding: 'utf8' }, (err, stdout, stderr) => {
        if (err) return reject(new Error(stderr || stdout || err.message))
        resolve()
      })
    })
  } finally {
    try { unlinkSync(tmpFile) } catch {}
  }
}

// ========== API 路由 ==========

// 健康检查
app.get('/api/status', (req, res) => {
  res.json({ status: 'ok', version: '1.0.0' })
})

// 获取打印机列表
app.get('/api/printers', requireAgentKey, async (req, res) => {
  try {
    const printers = await getPrinters()
    res.json({ printers })
  } catch (e) {
    res.status(500).json({ error: e.message })
  }
})

// 打印标签
app.post('/api/print', requireAgentKey, async (req, res) => {
  const { printerName, labels, qrBaseUrl } = req.body

  const getQrUrl = (qrCode) => {
    const base = qrBaseUrl || 'http://localhost:5173'
    return `${base}/mobile/scan?code=${encodeURIComponent(qrCode)}`
  }

  try {
    validatePrintPayload({ printerName, labels })
    const printer = (await getPrinters()).find(item => item.name === printerName)
    if (!printer) return res.status(400).json({ error: '打印机不存在' })
    const buffer = buildBatchBuffer(labels, getQrUrl)
    await printRawSimple(printer.port, buffer)
    res.json({ success: true, count: labels.length })
  } catch (e) {
    res.status(500).json({ error: `打印失败: ${e.message}` })
  }
})

// 打印单张
app.post('/api/print-one', requireAgentKey, async (req, res) => {
  const { printerName, label, qrBaseUrl } = req.body

  const getQrUrl = (qrCode) => {
    const base = qrBaseUrl || 'http://localhost:5173'
    return `${base}/mobile/scan?code=${encodeURIComponent(qrCode)}`
  }

  try {
    validatePrintPayload({ printerName, labels: [label] })
    const printer = (await getPrinters()).find(item => item.name === printerName)
    if (!printer) return res.status(400).json({ error: '打印机不存在' })
    const qrUrl = getQrUrl(label.qrCode)
    const buffer = buildLabelBuffer(label, qrUrl)
    await printRawSimple(printer.port, buffer)
    res.json({ success: true })
  } catch (e) {
    res.status(500).json({ error: `打印失败: ${e.message}` })
  }
})

// ========== 启动 ==========

app.listen(PORT, HOST, () => {
  console.log(`\n✅ 打印服务已启动: http://localhost:${PORT}`)
  console.log(`   - GET  /api/status    → 服务状态`)
  console.log(`   - GET  /api/printers  → 打印机列表`)
  console.log(`   - POST /api/print     → 批量打印`)
  console.log(`   - POST /api/print-one → 单张打印`)
  console.log(`\n等待前端连接...\n`)
})
