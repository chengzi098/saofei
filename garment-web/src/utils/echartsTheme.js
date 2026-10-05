/**
 * ECharts 主题适配
 * ECharts 图表不会自动跟随 CSS 变量 / html.dark，需在初始化和主题切换时
 * 显式传入文字、坐标轴、分割线颜色。
 *
 * 用法：
 *   import { getEchartsThemeOpts } from '@/utils/echartsTheme'
 *   const chart = echarts.init(el)
 *   chart.setOption({ ...getEchartsThemeOpts(), ...yourOption })
 *   // 主题切换时再次 setOption(getEchartsThemeOpts()) 即可刷新
 */

/** 读取当前是否暗黑模式 */
function isDarkMode() {
  return document.documentElement.classList.contains('dark')
}

/**
 * 返回当前主题下应合并到 option 根层的基础样式片段
 * （textStyle / legend 文字色 / 坐标轴文字与分割线色）
 */
export function getEchartsThemeOpts() {
  const dark = isDarkMode()
  const textColor = dark ? '#e5e7eb' : '#2d3748'
  const subColor = dark ? '#9ca3af' : '#64748b'
  const splitLine = dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)'

  return {
    textStyle: { color: textColor, fontFamily: 'Outfit, Noto Sans SC, sans-serif' },
    legend: { textStyle: { color: subColor } },
    tooltip: {
      backgroundColor: dark ? '#252a31' : '#fff',
      borderColor: dark ? '#3a4049' : '#e2e0dc',
      textStyle: { color: textColor }
    },
    xAxis: {
      axisLine: { lineStyle: { color: splitLine } },
      axisLabel: { color: subColor },
      splitLine: { lineStyle: { color: splitLine } }
    },
    yAxis: {
      axisLine: { lineStyle: { color: splitLine } },
      axisLabel: { color: subColor },
      splitLine: { lineStyle: { color: splitLine } }
    }
  }
}
