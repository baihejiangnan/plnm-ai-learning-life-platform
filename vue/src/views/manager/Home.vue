<template>
  <div class="dashboard-container">
    <!-- 工具栏：月份选择 + 标签过滤状态 + 快速入口 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          value-format="YYYY-MM"
          format="YYYY-MM"
          placeholder="选择月份"
          @change="handleMonthChange"
          style="width: 140px"
        />
        <template v-if="selectedTagId">
          <el-tag type="primary" class="ml8">已过滤标签：{{ selectedTagName }}</el-tag>
          <el-button text class="ml8" @click="clearTagFilter">清除过滤</el-button>
        </template>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="goToNewNote">新建笔记</el-button>
        <el-button @click="goToTags">标签管理</el-button>
        <el-button @click="goToBudgets">预算设置</el-button>
      </div>
    </div>

    <AiWeeklyReviewCard class="weekly-review-block" />

    <!-- 指标卡片区 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-title">笔记总数</div>
        <div class="stat-value">{{ stats.notesTotal }}</div>
        <div class="stat-sub">最近更新：{{ stats.latestUpdate }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">标签数量</div>
        <div class="stat-value">{{ stats.tagsTotal }}</div>
        <div class="stat-sub">热门标签：{{ stats.topTagName || '暂无' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">本月支出</div>
        <div class="stat-value">￥{{ formatAmount(stats.monthExpenseTotal) }}</div>
        <div class="stat-sub">{{ selectedMonth }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">预算使用率</div>
        <div class="stat-value">{{ budgetPercent }}%</div>
        <el-progress :percentage="Math.min(999, Math.round(budgetPercent))" :status="budgetStatus" />
      </div>
      <div class="stat-card">
        <div class="stat-title">近7天新增笔记数</div>
        <div class="stat-value">{{ stats.last7CreatedCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">平均字数</div>
        <div class="stat-value">{{ stats.averageWordCount }}</div>
      </div>
      <!-- 指标卡片区新增：消费分类 Top5 列表，与浏览量 Top5 一致样式 -->
      <div class="stat-card">
        <div class="stat-title">浏览量最高 Top5</div>
        <ul class="top5-list">
          <li v-for="item in stats.top5Views" :key="item.id" @click="viewNote(item.id)">
            <span class="top5-title">{{ item.title || '无标题' }}</span>
            <span class="top5-count">{{ item.viewCount ?? 0 }}</span>
          </li>
        </ul>
      </div>
      <div class="stat-card">
        <div class="stat-title">{{ selectedMonth }} 消费分类 Top5</div>
        <ul class="top5-list">
          <li v-for="item in expenseTop5" :key="item.categoryId">
            <span class="top5-title">{{ item.name || `分类${item.categoryId}` }}</span>
            <span class="top5-count">￥{{ formatAmount(item.total || 0) }}</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 图表区：插入 Top5 排行卡片 -->
    <div class="charts-grid">
      <div class="chart-card">
        <div class="chart-title">{{ selectedMonth }} 分类支出占比</div>
        <div ref="pieRef" class="chart-canvas"></div>
      </div>
      <!-- 原消费分类 Top5 排行卡片已移动到指标卡片区 -->
      <div class="chart-card">
        <div class="chart-title">标签笔记数 Top10（可点击联动）</div>
        <div ref="barRef" class="chart-canvas"></div>
      </div>
      <div class="chart-card wide">
        <div class="chart-title">最近30天笔记活动</div>
        <div ref="lineRef" class="chart-canvas"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { useNoteStore } from '@/stores/note'
import { expenseApi } from '@/api'
import AiWeeklyReviewCard from '@/views/manager/home/AiWeeklyReviewCard.vue'

// 路由
const router = useRouter()

// 工具函数
const formatAmount = (a) => { try { return Number(a || 0).toFixed(2) } catch { return a } }
const formatDate = (s) => { if (!s) return ''; try { const d = new Date(s); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` } catch { return s } }
const nowMonth = () => new Date().toISOString().slice(0, 7)

// Stores
const noteStore = useNoteStore()

// 选择月份
const selectedMonth = ref(nowMonth())
const handleMonthChange = async () => {
  await loadExpenseStats()
}

// 标签联动过滤
const selectedTagId = ref(null)
const selectedTagName = ref('')
const clearTagFilter = () => {
  selectedTagId.value = null
  selectedTagName.value = ''
  updateLineData(noteStore.currentPageNotes || [])
}

// 指标数据
const stats = reactive({
  notesTotal: 0,
  tagsTotal: 0,
  topTagName: '',
  latestUpdate: '',
  monthExpenseTotal: 0,
  budgets: { global: 0 },
  last7CreatedCount: 0,
  averageWordCount: 0,
  top5Views: []
})

// 消费分类 Top5（列表）
const expenseTop5 = ref([])

const budgetPercent = computed(() => {
  const total = Number(stats.monthExpenseTotal || 0)
  const budget = Number(stats.budgets.global || 0)
  if (!budget || budget <= 0) return 0
  const p = (total / budget) * 100
  return Number.isFinite(p) ? Math.round(p) : 0
})
const budgetStatus = computed(() => {
  const p = budgetPercent.value
  if (p >= 100) return 'exception'
  if (p >= Number(localStorage.getItem('budgetThreshold') || 100)) return 'warning'
  return undefined
})

// 图表 refs （移除 top5Ref）
const pieRef = ref(null)
const barRef = ref(null)
const lineRef = ref(null)
let pieChart = null
let barChart = null
let lineChart = null
let top5Chart = null

// 初始化图表
const initPie = () => {
  if (!pieRef.value) return
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { top: 'bottom' },
    series: [{ type: 'pie', radius: '60%', data: [], label: { formatter: '{b}: {d}%' } }]
  })
}
const initBar = () => {
  if (!barRef.value) return
  barChart = echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [], itemStyle: { color: '#409EFF' } }]
  })
  // 点击联动折线图
  barChart.on('click', (params) => {
    const tagId = params?.data?.tagId
    const tagName = params?.name
    if (tagId != null) {
      selectedTagId.value = tagId
      selectedTagName.value = tagName || ''
      updateLineData(noteStore.currentPageNotes || [], tagId)
    }
  })
}
const initLine = () => {
  if (!lineRef.value) return
  lineChart = echarts.init(lineRef.value)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value' },
    series: [{ type: 'line', data: [], smooth: true, areaStyle: {} }]
  })
}

const updatePieData = (byCategory = []) => {
  if (!pieChart) return
  const data = (byCategory || []).map(item => ({ name: item.name || `分类${item.categoryId}`, value: Number(item.total || 0) }))
  pieChart.setOption({ series: [{ data }] })
}

const updateBarData = (tags = []) => {
  if (!barChart) return
  const sorted = [...tags].sort((a, b) => (b.noteCount ?? b.useCount ?? 0) - (a.noteCount ?? a.useCount ?? 0)).slice(0, 10)
  const x = sorted.map(t => t.name)
  const seriesData = sorted.map(t => ({ name: t.name, value: Number(t.noteCount ?? t.useCount ?? 0), tagId: t.id }))
  barChart.setOption({ xAxis: { data: x }, series: [{ data: seriesData }] })
}

const updateLineData = (notes = [], filterTagId = null) => {
  if (!lineChart) return
  // 若按标签过滤
  let filtered = notes
  if (filterTagId != null) {
    filtered = (notes || []).filter(n => Array.isArray(n.tags) && n.tags.some(t => t.id === filterTagId))
  }
  // 聚合最近30天的更新数量
  const days = []
  const mapDay = new Map()
  const today = new Date()
  for (let i = 29; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    days.push(key)
    mapDay.set(key, 0)
  }
  for (const n of filtered || []) {
    const k = formatDate(n.updatedTime || n.updateTime || n.createdTime || n.createTime)
    if (mapDay.has(k)) mapDay.set(k, (mapDay.get(k) || 0) + 1)
  }
  const y = days.map(d => mapDay.get(d) || 0)
  lineChart.setOption({ xAxis: { data: days }, series: [{ data: y }] })
}

// 计算指标（近7天新增、平均字数、Top5浏览量）
const computeExtraMetrics = (notes = []) => {
  // 近7天新增
  const now = new Date()
  const sevenDaysAgo = new Date(now)
  sevenDaysAgo.setDate(now.getDate() - 7)
  stats.last7CreatedCount = (notes || []).filter(n => {
    const s = n.createdTime || n.createTime
    if (!s) return false
    const d = new Date(s)
    return d >= sevenDaysAgo
  }).length
  // 平均字数
  const arr = (notes || []).map(n => Number(n.wordCount ?? 0))
  const sum = arr.reduce((acc, v) => acc + (Number.isFinite(v) ? v : 0), 0)
  stats.averageWordCount = arr.length > 0 ? Math.round(sum / arr.length) : 0
  // 浏览量Top5
  stats.top5Views = [...(notes || [])]
    .sort((a, b) => (Number(b.viewCount ?? 0) - Number(a.viewCount ?? 0)))
    .slice(0, 5)
    .map(n => ({ id: n.id, title: n.title, viewCount: n.viewCount ?? 0 }))
}

// 加载数据
const loadNotesAndTags = async () => {
  await Promise.all([
    noteStore.fetchNotes({ page: 1, size: 300 }),
    noteStore.fetchTags()
  ])
  stats.notesTotal = noteStore.totalNotes
  stats.tagsTotal = (noteStore.allTags || []).length
  stats.topTagName = (noteStore.allTags || []).sort((a,b) => (b.noteCount ?? b.useCount ?? 0) - (a.noteCount ?? a.useCount ?? 0))[0]?.name || ''
  const latest = (noteStore.currentPageNotes || []).map(n => n.updatedTime || n.updateTime || n.createdTime || n.createTime).filter(Boolean).sort((a,b) => Date.parse(b) - Date.parse(a))[0]
  stats.latestUpdate = formatDate(latest)
  updateBarData(noteStore.allTags || [])
  updateLineData(noteStore.currentPageNotes || [])
  computeExtraMetrics(noteStore.currentPageNotes || [])
}

// 新增 Top5 图表初始化
const initTop5 = () => {
  if (!top5Ref.value) return
  top5Chart = echarts.init(top5Ref.value)
  top5Chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: [] },
    series: [{ type: 'bar', data: [], itemStyle: { color: '#67C23A' }, label: { show: true, position: 'right' } }]
  })
}

const updateTop5Data = (byCategory = []) => {
  if (!top5Chart) return
  const sorted = [...(byCategory || [])].sort((a, b) => Number(b.total || 0) - Number(a.total || 0)).slice(0, 5)
  const names = sorted.map(i => i.name || `分类${i.categoryId}`)
  const values = sorted.map(i => Number(i.total || 0))
  top5Chart.setOption({ yAxis: { data: names }, series: [{ data: values }] })
}

onMounted(async () => {
  initPie(); initBar(); initLine()
  await Promise.all([loadNotesAndTags(), loadExpenseStats()])
  // 自适应
  const resize = () => {
    pieChart && pieChart.resize();
    barChart && barChart.resize();
    lineChart && lineChart.resize();
  }
  window.addEventListener('resize', resize)
  cleanupFns.push(() => window.removeEventListener('resize', resize))
})

// 加载月份支出与分类占比、Top5 列表、预算
const loadExpenseStats = async () => {
  const m = selectedMonth.value
  const resStats = await expenseApi.getMonthStats(m)
  const data = resStats.data || {}
  stats.monthExpenseTotal = Number(data.total || 0)
  updatePieData(data.byCategory || [])
  const byCategory = data.byCategory || []
  expenseTop5.value = [...byCategory].sort((a, b) => Number(b.total || 0) - Number(a.total || 0)).slice(0, 5)
  const resBudgets = await expenseApi.getBudgets(m)
  const list = resBudgets.data || []
  const global = list.find(x => x.categoryId == null)
  stats.budgets.global = global ? Number(global.amount || 0) : 0
}

const cleanupFns = []

onBeforeUnmount(() => {
  for (const fn of cleanupFns) try { fn() } catch {}
  try { pieChart && pieChart.dispose() } catch {}
  try { barChart && barChart.dispose() } catch {}
  try { lineChart && lineChart.dispose() } catch {}
})

// 快速入口与交互
const goToNewNote = () => router.push('/notes/editor')
const goToTags = () => router.push('/notes/tags')
const goToBudgets = () => router.push('/life/budgets')
const viewNote = (id) => { if (id != null) router.push(`/notes/detail/${id}`) }
</script>

<style scoped>
.dashboard-container { padding: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.toolbar-left { display: flex; align-items: center; }
.toolbar-right { display: flex; gap: 8px; }
.ml8 { margin-left: 8px; }
.weekly-review-block { margin-bottom: 16px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.stat-card { background: var(--el-color-white); border-radius: 8px; padding: 16px; box-shadow: var(--el-box-shadow-light); }
.stat-title { font-size: 14px; color: var(--el-text-color-secondary); }
.stat-value { font-size: 24px; font-weight: 600; margin-top: 4px; }
.stat-sub { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 6px; }
.top5-list { list-style: none; padding: 0; margin: 8px 0 0; }
.top5-list li { display: flex; justify-content: space-between; padding: 4px 0; cursor: pointer; }
.top5-title { max-width: 70%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.top5-count { color: var(--el-text-color-secondary); }
.charts-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.chart-card { background: var(--el-color-white); border-radius: 8px; padding: 16px; box-shadow: var(--el-box-shadow-light); }
.chart-card.wide { grid-column: 1 / -1; }
.chart-title { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.chart-canvas { width: 100%; height: 320px; }
@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .charts-grid { grid-template-columns: 1fr; }
}
</style>
