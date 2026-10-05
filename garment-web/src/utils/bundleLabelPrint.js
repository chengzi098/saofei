/**
 * 扎货标签打印的公共工具：二维码 URL 拼接、HTML 标签构建、隐藏 iframe 打印。
 * 由 BundlePrintDrawer.vue 与 BundleQrOverviewDialog.vue 共用。
 * 纸张规格：40mm × 70mm，二维码 22mm × 22mm。
 */

/**
 * 拼接扫码可识别的二维码 URL
 * @param {string} qrCode 扎货的 32 位 qrCode
 * @returns {string} 形如 {origin}/mobile/scan?code={qrCode}
 */
export function getQrUrl(qrCode) {
  return `${window.location.origin}/mobile/scan?code=${qrCode}`
}

export function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

/**
 * 从 DOM 抓取已渲染的二维码 SVG（用于打印 HTML，避免在打印窗口重新渲染）
 * 需要二维码宿主节点带 data-print-qr="{id}" 属性
 */
export function getRenderedQrSvg(id) {
  const host = Array.from(document.querySelectorAll('[data-print-qr]'))
    .find(element => element.dataset.printQr === String(id))
  return host?.querySelector('svg')?.outerHTML || ''
}

/**
 * 构造单个标签的 HTML 片段
 * @param {object} label 扎货信息
 * @param {{companyName?:string, styleNo?:string, bedNo?:string}} fallback 缺省值
 */
export function buildLabelHtml(label, fallback = {}) {
  return `
    <div class="label">
      <div class="label-info">
        <div class="row company">${escapeHtml(fallback.companyName)}</div>
        <div class="row">款号: ${escapeHtml(label.styleNo || fallback.styleNo || '-')}</div>
        <div class="row">床号: ${escapeHtml(label.bedNo || fallback.bedNo || '-')}</div>
        <div class="row">名称: ${escapeHtml(label.bedName || '-')}</div>
        <div class="row bundle">扎号: ${escapeHtml(label.bundleNo)}</div>
        <div class="row">件数: ${escapeHtml(label.quantity)}</div>
        <div class="row">码数: ${escapeHtml(label.size || '-')}</div>
        <div class="row">颜色: ${escapeHtml(label.color || label.colors || '-')}</div>
      </div>
      <div class="qr">${getRenderedQrSvg(label.id)}</div>
    </div>`
}

/**
 * 打开隐藏 iframe 写入标签 HTML 并触发浏览器打印
 */
export function openPrintWindow(labels, fallback = {}) {
  const items = labels.map(label => buildLabelHtml(label, fallback))
  const html = `<!doctype html><html><head><meta charset="utf-8"><title>打印标签</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}html,body{width:40mm;font-family:"Microsoft YaHei",sans-serif}
.label{width:40mm;height:70mm;padding:1mm 3mm;display:flex;flex-direction:column;align-items:center;page-break-after:always;overflow:hidden}
.label:last-child{page-break-after:auto}.label-info{width:100%;border-bottom:1px dashed #999;padding-bottom:1mm;margin-bottom:1mm}
.row{font-size:2.8mm;line-height:1.4;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.company{font-size:3.2mm;font-weight:700}.bundle{font-size:3mm;font-weight:700}
.qr{margin-top:1mm}.qr svg{display:block;width:22mm;height:22mm}@page{size:40mm 70mm;margin:0}
</style></head><body>${items.join('')}
<script>
setTimeout(()=>window.print(),100);
<\/script></body></html>`

  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;left:-9999px;top:-9999px;width:0;height:0;border:0'
  document.body.appendChild(iframe)
  const doc = iframe.contentDocument || iframe.contentWindow.document
  doc.open()
  doc.write(html)
  doc.close()
  setTimeout(() => iframe.remove(), 1500)
}
