import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const files = [
  'src/views/QrPrint.vue',
  'src/components/BundlePrintDrawer.vue'
]

for (const file of files) {
  const source = readFileSync(new URL(`../${file}`, import.meta.url), 'utf8')
  const forbiddenQrCdn = 'cdn.jsdelivr.net/npm/' + 'qrcode-generator'
  assert.equal(source.includes(forbiddenQrCdn), false, `${file} must not load QR code scripts from a CDN`)
  assert.equal(/\binnerHTML\s*=/.test(source), false, `${file} must not inject QR markup with innerHTML`)
}

console.log('qr render security ok')
