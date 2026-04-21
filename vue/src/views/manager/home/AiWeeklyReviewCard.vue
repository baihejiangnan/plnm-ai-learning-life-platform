<template>
  <el-card ref="cardRef" class="weekly-card" shadow="never">
    <template #header>
      <div class="weekly-header">
        <div>
          <div class="weekly-title">AI周复盘卡片</div>
          <div class="weekly-subtitle">{{ weekRangeText }}</div>
        </div>
        <el-button text @click="reload" :loading="loading">刷新</el-button>
      </div>
    </template>

    <div v-if="!startedLoading" class="placeholder-wrap">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="loading" class="placeholder-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <el-alert v-else-if="errorMsg" type="error" :closable="false" show-icon :title="errorMsg" />

    <div v-else class="weekly-content">
      <div class="kpi-grid">
        <div class="kpi-item">
          <div class="kpi-label">综合效率分</div>
          <div class="kpi-value">{{ metrics.thisWeekScore }}</div>
          <div :class="['kpi-trend', metrics.scoreDelta >= 0 ? 'up' : 'down']">
            {{ metrics.scoreDelta >= 0 ? '+' : '' }}{{ metrics.scoreDelta }}%
          </div>
        </div>
        <div class="kpi-item">
          <div class="kpi-label">本周完成任务</div>
          <div class="kpi-value">{{ metrics.tasksCompleted }}</div>
          <div class="kpi-trend neutral">上周 {{ metrics.prevTasksCompleted }}</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-label">学习活跃项</div>
          <div class="kpi-value">{{ metrics.learningActive }}</div>
          <div class="kpi-trend neutral">平均进度 {{ metrics.avgPercent }}%</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-label">本周支出</div>
          <div class="kpi-value">￥{{ formatAmount(metrics.expenseTotal) }}</div>
          <div :class="['kpi-trend', metrics.expenseDelta <= 0 ? 'up' : 'down']">
            {{ metrics.expenseDelta >= 0 ? '+' : '' }}{{ metrics.expenseDelta }}%
          </div>
        </div>
      </div>

      <div class="compare-row">
        <el-switch v-model="showCompare" inline-prompt active-text="周对比" inactive-text="简洁视图" />
      </div>

      <div class="chart-grid">
        <div class="chart-box">
          <div class="chart-title">时间/精力分配</div>
          <div ref="pieRef" class="chart"></div>
        </div>
        <div class="chart-box" v-show="showCompare">
          <div class="chart-title">周对比趋势</div>
          <div ref="lineRef" class="chart"></div>
        </div>
        <div class="chart-box">
          <div class="chart-title">关键任务完成</div>
          <div ref="barRef" class="chart"></div>
        </div>
      </div>

      <div class="insight-box">
        <div class="insight-title">Actionable Insights</div>
        <ul>
          <li v-for="(item, idx) in insights" :key="idx">{{ item }}</li>
        </ul>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { expenseApi, learningProgressApi } from '@/api'
import { useNoteStore } from '@/stores/note'

const CACHE_TTL_MS = 10 * 60 * 1000
const cardRef = ref(null)
const pieRef = ref(null)
const lineRef = ref(null)
const barRef = ref(null)

const loading = ref(false)
const startedLoading = ref(false)
const errorMsg = ref('')
const showCompare = ref(true)
const insights = ref([])

const metrics = reactive({
  thisWeekScore: 0,
  prevWeekScore: 0,
  scoreDelta: 0,
  tasksCompleted: 0,
  prevTasksCompleted: 0,
  learningActive: 0,
  avgPercent: 0,
  expenseTotal: 0,
  prevExpenseTotal: 0,
  expenseDelta: 0
})

const noteStore = useNoteStore()
let pieChart = null
let lineChart = null
let barChart = null
let observer = null

const weekRangeText = computed(() => {
  const range = getWeekRange(new Date())
  return `${range.startStr} 至 ${range.endStr}`
})

const formatAmount = (val) => {
  try {
    return Number(val || 0).toFixed(2)
  } catch {
    return val
  }
}

const getWeekRange = (date) => {
  const d = new Date(date)
  const day = d.getDay() === 0 ? 7 : d.getDay()
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() - day + 1)
  const start = new Date(d)
  const end = new Date(d)
  end.setDate(start.getDate() + 6)
  return {
    start,
    end,
    startStr: formatDate(start),
    endStr: formatDate(end)
  }
}

const formatDate = (date) => {
  const d = new Date(date)
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const da = `${d.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${da}`
}

const inRange = (value, start, end) => {
  if (!value) return false
  const t = new Date(value).getTime()
  return Number.isFinite(t) && t >= start.getTime() && t <= end.getTime() + 86399999
}

const getCacheKey = () => {
  const week = getWeekRange(new Date())
  return `ai-weekly-review:${week.startStr}`
}

const loadFromCache = () => {
  const raw = sessionStorage.getItem(getCacheKey())
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (!parsed.ts || Date.now() - parsed.ts > CACHE_TTL_MS) return null
    return parsed.data
  } catch {
    return null
  }
}

const saveToCache = (data) => {
  sessionStorage.setItem(getCacheKey(), JSON.stringify({ ts: Date.now(), data }))
}

const toPercentDelta = (current, prev) => {
  if (!prev) return current > 0 ? 100 : 0
  return Math.round(((current - prev) / prev) * 100)
}

const clampScore = (value) => {
  const v = Math.round(value)
  if (v < 0) return 0
  if (v > 100) return 100
  return v
}

const computeScore = (payload) => {
  const notesScore = Math.min(payload.notesCreated / 5, 1) * 30
  const taskScore = Math.min(payload.tasksCompleted / 6, 1) * 30
  const learningScore = Math.min(payload.learningActive / 4, 1) * 20 + Math.min(payload.avgPercent / 100, 1) * 10
  const expenseScore = payload.expenseTotal <= payload.prevExpenseTotal ? 10 : Math.max(0, 10 - Math.min(10, (payload.expenseTotal - payload.prevExpenseTotal) / 20))
  return clampScore(notesScore + taskScore + learningScore + expenseScore)
}

const buildInsights = () => {
  const list = []
  if (metrics.scoreDelta >= 8) {
    list.push(`本周综合效率较上周提升 ${metrics.scoreDelta}%，可保持当前节奏并继续固化高效时段。`)
  } else if (metrics.scoreDelta <= -8) {
    list.push(`综合效率较上周下降 ${Math.abs(metrics.scoreDelta)}%，建议把任务拆成更小粒度并设置每日完成阈值。`)
  } else {
    list.push('本周效率整体平稳，建议增加 1 项可量化目标，提升突破感。')
  }
  if (metrics.expenseDelta > 0) {
    list.push(`本周支出较上周上升 ${metrics.expenseDelta}%，建议优先复盘餐饮/购物类明细并设置下周限额。`)
  } else {
    list.push('本周支出控制良好，建议把省下预算转为学习投入，形成正反馈。')
  }
  if (metrics.learningActive < 2) {
    list.push('学习活跃项偏少，建议下周至少安排 3 次学习打卡并记录进度。')
  } else {
    list.push(`学习活跃项 ${metrics.learningActive} 个，继续保持每次学习后同步一句复盘笔记。`)
  }
  insights.value = list
}

const renderCharts = (chartData) => {
  if (!pieRef.value || !lineRef.value || !barRef.value) return
  if (!pieChart) pieChart = echarts.init(pieRef.value)
  if (!lineChart) lineChart = echarts.init(lineRef.value)
  if (!barChart) barChart = echarts.init(barRef.value)

  pieChart.setOption({
    animationDuration: 500,
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        data: chartData.pieData,
        label: { formatter: '{b}\n{d}%', fontSize: 11 }
      }
    ]
  })

  lineChart.setOption({
    animationDuration: 500,
    tooltip: { trigger: 'axis' },
    grid: { left: 34, right: 16, top: 24, bottom: 30 },
    xAxis: { type: 'category', data: ['上周', '本周'] },
    yAxis: { type: 'value' },
    series: [
      { type: 'line', smooth: true, data: chartData.lineData, itemStyle: { color: '#4f7cff' }, areaStyle: { opacity: 0.12 } }
    ]
  })

  barChart.setOption({
    animationDuration: 500,
    tooltip: { trigger: 'axis' },
    grid: { left: 34, right: 16, top: 24, bottom: 34 },
    xAxis: { type: 'category', data: chartData.barData.map(i => i.name), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', barWidth: '42%', data: chartData.barData.map(i => i.value), itemStyle: { borderRadius: [6, 6, 0, 0], color: '#67c23a' } }]
  })
}

const prepareData = async () => {
  const nowWeek = getWeekRange(new Date())
  const prevWeekStart = new Date(nowWeek.start)
  prevWeekStart.setDate(prevWeekStart.getDate() - 7)
  const prevWeek = getWeekRange(prevWeekStart)

  await noteStore.fetchNotes({ page: 1, size: 400 })
  const notes = noteStore.currentPageNotes || []
  const progressRes = await learningProgressApi.getList()
  const progressList = progressRes?.data || []
  const expenseRes = await expenseApi.getList({ page: 1, size: 400 })
  const expenseList = expenseRes?.data?.list || []

  const nowNotes = notes.filter(n => inRange(n.createdTime || n.createTime || n.updatedTime, nowWeek.start, nowWeek.end))
  const prevNotes = notes.filter(n => inRange(n.createdTime || n.createTime || n.updatedTime, prevWeek.start, prevWeek.end))

  const nowProgressUpdated = progressList.filter(p => inRange(p.updatedTime || p.createdTime, nowWeek.start, nowWeek.end))
  const prevProgressUpdated = progressList.filter(p => inRange(p.updatedTime || p.createdTime, prevWeek.start, prevWeek.end))

  const nowExpense = expenseList.filter(e => inRange(e.expenseTime || e.createdTime, nowWeek.start, nowWeek.end))
  const prevExpense = expenseList.filter(e => inRange(e.expenseTime || e.createdTime, prevWeek.start, prevWeek.end))
  const nowExpenseTotal = nowExpense.reduce((sum, i) => sum + Number(i.amount || 0), 0)
  const prevExpenseTotal = prevExpense.reduce((sum, i) => sum + Number(i.amount || 0), 0)

  const nowTasks = nowNotes.length + nowProgressUpdated.filter(i => Number(i.percent || 0) >= 80).length
  const prevTasks = prevNotes.length + prevProgressUpdated.filter(i => Number(i.percent || 0) >= 80).length
  const avgPercent = progressList.length ? Math.round(progressList.reduce((s, i) => s + Number(i.percent || 0), 0) / progressList.length) : 0

  const nowPayload = {
    notesCreated: nowNotes.length,
    tasksCompleted: nowTasks,
    learningActive: nowProgressUpdated.length,
    avgPercent,
    expenseTotal: nowExpenseTotal,
    prevExpenseTotal
  }
  const prevPayload = {
    notesCreated: prevNotes.length,
    tasksCompleted: prevTasks,
    learningActive: prevProgressUpdated.length,
    avgPercent: prevProgressUpdated.length ? Math.round(prevProgressUpdated.reduce((s, i) => s + Number(i.percent || 0), 0) / prevProgressUpdated.length) : avgPercent,
    expenseTotal: prevExpenseTotal,
    prevExpenseTotal
  }

  const thisWeekScore = computeScore(nowPayload)
  const prevWeekScore = computeScore(prevPayload)
  const scoreDelta = toPercentDelta(thisWeekScore, prevWeekScore)

  const pieData = [
    { name: '笔记复盘', value: Math.max(nowNotes.length, 1) },
    { name: '学习推进', value: Math.max(nowProgressUpdated.length, 1) },
    { name: '消费记录', value: Math.max(nowExpense.length, 1) }
  ]

  const lineData = [prevWeekScore, thisWeekScore]
  const barData = [
    { name: '笔记任务', value: nowNotes.length },
    { name: '学习任务', value: nowProgressUpdated.filter(i => Number(i.percent || 0) >= 80).length },
    { name: '消费复盘', value: nowExpense.length }
  ]

  return {
    metrics: {
      thisWeekScore,
      prevWeekScore,
      scoreDelta,
      tasksCompleted: nowTasks,
      prevTasksCompleted: prevTasks,
      learningActive: nowProgressUpdated.length,
      avgPercent,
      expenseTotal: Number(nowExpenseTotal.toFixed(2)),
      prevExpenseTotal: Number(prevExpenseTotal.toFixed(2)),
      expenseDelta: toPercentDelta(nowExpenseTotal, prevExpenseTotal)
    },
    charts: { pieData, lineData, barData }
  }
}

const applyData = (data) => {
  Object.assign(metrics, data.metrics)
  renderCharts(data.charts)
  buildInsights()
  nextTick(() => {
    pieChart && pieChart.resize()
    lineChart && lineChart.resize()
    barChart && barChart.resize()
  })
}

const load = async (force = false) => {
  startedLoading.value = true
  loading.value = true
  errorMsg.value = ''
  try {
    if (!force) {
      const cache = loadFromCache()
      if (cache) {
        applyData(cache)
        loading.value = false
        return
      }
    }
    const data = await prepareData()
    applyData(data)
    saveToCache(data)
  } catch (error) {
    errorMsg.value = error?.message || '周复盘加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const reload = async () => {
  await load(true)
}

onMounted(() => {
  if ('IntersectionObserver' in window) {
    observer = new IntersectionObserver((entries) => {
      const isVisible = entries.some(e => e.isIntersecting)
      if (isVisible) {
        load()
        observer && observer.disconnect()
      }
    }, { threshold: 0.2 })
    if (cardRef.value?.$el) {
      observer.observe(cardRef.value.$el)
    }
  } else {
    load()
  }
  const resizeHandler = () => {
    pieChart && pieChart.resize()
    lineChart && lineChart.resize()
    barChart && barChart.resize()
  }
  window.addEventListener('resize', resizeHandler)
  onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeHandler)
  })
})

watch(showCompare, () => {
  nextTick(() => {
    lineChart && lineChart.resize()
  })
})

onBeforeUnmount(() => {
  observer && observer.disconnect()
  pieChart && pieChart.dispose()
  lineChart && lineChart.dispose()
  barChart && barChart.dispose()
})
</script>

<style scoped>
.weekly-card {
  border: 1px solid #e8edf9;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  transition: transform .25s ease, box-shadow .25s ease;
}

.weekly-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(34, 68, 181, 0.08);
}

.weekly-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.weekly-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2a44;
}

.weekly-subtitle {
  font-size: 12px;
  color: #8691a9;
  margin-top: 2px;
}

.placeholder-wrap {
  padding: 4px 2px;
}

.weekly-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.kpi-item {
  border-radius: 12px;
  border: 1px solid #e9eef9;
  background: linear-gradient(180deg, #fbfdff 0%, #f4f8ff 100%);
  padding: 12px;
}

.kpi-label {
  font-size: 12px;
  color: #6e7a98;
}

.kpi-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #243253;
}

.kpi-trend {
  margin-top: 6px;
  font-size: 12px;
}

.kpi-trend.up {
  color: #10b981;
}

.kpi-trend.down {
  color: #ef4444;
}

.kpi-trend.neutral {
  color: #74819e;
}

.compare-row {
  display: flex;
  justify-content: flex-end;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.chart-box {
  border: 1px solid #e9eef9;
  border-radius: 12px;
  padding: 10px 10px 4px;
  background: #fff;
}

.chart-title {
  font-size: 13px;
  color: #556381;
  margin-bottom: 6px;
}

.chart {
  width: 100%;
  height: 220px;
}

.insight-box {
  border-radius: 12px;
  border: 1px solid #dbe7ff;
  background: linear-gradient(180deg, #f4f8ff 0%, #eef5ff 100%);
  padding: 12px 14px;
}

.insight-title {
  font-size: 14px;
  font-weight: 600;
  color: #2e4da1;
  margin-bottom: 8px;
}

.insight-box ul {
  margin: 0;
  padding-left: 18px;
  color: #38435a;
  line-height: 1.7;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .chart-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .weekly-title {
    font-size: 16px;
  }
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .chart-grid {
    grid-template-columns: 1fr;
  }
  .chart {
    height: 200px;
  }
}

@media (prefers-color-scheme: dark) {
  .weekly-card {
    border-color: #27324d;
    background: #131a29;
  }
  .weekly-title {
    color: #dfe8ff;
  }
  .weekly-subtitle {
    color: #9aa7c4;
  }
  .kpi-item,
  .chart-box {
    border-color: #2c3753;
    background: #182133;
  }
  .kpi-label {
    color: #9fb0d4;
  }
  .kpi-value {
    color: #eef3ff;
  }
  .chart-title {
    color: #aebfe0;
  }
  .insight-box {
    border-color: #34426a;
    background: #1c2742;
  }
  .insight-title {
    color: #b5c8ff;
  }
  .insight-box ul {
    color: #d7e2fb;
  }
}
</style>
