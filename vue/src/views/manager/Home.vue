<template>
  <div class="home-redesign">
    <section class="hero-panel">
      <div class="hero-copy">
        <div class="eyebrow">
          <span>PLNM 工作台</span>
          <span class="pill blue">{{ selectedMonth }}</span>
        </div>
        <div class="hero-heading-row">
          <h1 class="hero-title">首页总览</h1>
          <span :class="['pill', budgetTone]">预算 {{ budgetPercent }}%</span>
        </div>
        <p>把笔记、预算、课程和 AI 协作压缩成一个可扫读的桌面面板，优先显示今天需要处理的数据和行动。</p>
        <div class="hero-snapshot-grid" aria-label="首页关键数据">
          <article>
            <span>笔记资产</span>
            <strong>{{ stats.notesTotal }}</strong>
            <small>最近更新 {{ stats.latestUpdate }}</small>
          </article>
          <article>
            <span>本月支出</span>
            <strong>￥{{ formatAmount(stats.monthExpenseTotal) }}</strong>
            <small>{{ selectedMonth }}</small>
          </article>
          <article>
            <span>预算进度</span>
            <strong>{{ budgetPercent }}%</strong>
            <small>{{ stats.budgets.global ? `预算 ￥${formatAmount(stats.budgets.global)}` : '未设置预算' }}</small>
          </article>
          <article>
            <span>活跃标签</span>
            <strong>{{ stats.topTagName || '暂无' }}</strong>
            <small>{{ stats.tagsTotal }} 个标签</small>
          </article>
        </div>
        <div class="hero-actions" aria-label="快捷操作">
          <button class="primary-action" type="button" @click="goToNewNote">新建笔记</button>
          <button type="button" @click="router.push('/life/expenses')">记账</button>
          <button type="button" @click="router.push('/learning/progress')">学习</button>
          <button type="button" @click="router.push('/ai/chat')">AI</button>
        </div>
      </div>

      <aside class="priority-stack" aria-label="今日优先级">
        <div class="stack-head">
          <strong>今日优先级</strong>
          <span>{{ priorityItems.length }} 项</span>
        </div>
        <article v-for="item in priorityItems" :key="item.title" class="priority-card">
          <span :class="['priority-dot', item.tone]"></span>
          <div>
            <header>
              <strong>{{ item.title }}</strong>
              <span :class="['pill', item.tone]">{{ item.meta }}</span>
            </header>
            <p>{{ item.text }}</p>
          </div>
        </article>
      </aside>
    </section>

    <section class="control-strip">
      <div>
        <label>分析月份</label>
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          value-format="YYYY-MM"
          format="YYYY-MM"
          placeholder="选择月份"
          @change="handleMonthChange"
        />
      </div>
      <div v-if="selectedTagId" class="filter-chip">
        已联动标签：{{ selectedTagName }}
        <button type="button" @click="clearTagFilter">清除</button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <label>{{ metric.label }}</label>
        <div class="metric-value">{{ metric.value }}</div>
        <small>{{ metric.help }}</small>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="panel weekly-panel">
        <AiWeeklyReviewCard />
      </article>

      <article class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>{{ selectedMonth }} 分类支出占比</h2>
            <p>预算、消费和分类趋势放在同一个决策区域里。</p>
          </div>
          <span :class="['pill', budgetTone]">{{ budgetPercent }}%</span>
        </div>
        <div ref="pieRef" class="chart-canvas"></div>
      </article>

      <article class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>标签笔记数 Top10</h2>
            <p>点击柱形图可以联动下方 30 天活动趋势。</p>
          </div>
          <span class="pill green">{{ stats.tagsTotal }} 个标签</span>
        </div>
        <div ref="barRef" class="chart-canvas"></div>
      </article>

      <article class="panel chart-panel wide">
        <div class="panel-head">
          <div>
            <h2>最近 30 天笔记活动</h2>
            <p>{{ selectedTagId ? `当前仅查看「${selectedTagName}」标签` : '按更新时间聚合，便于发现知识沉淀节奏。' }}</p>
          </div>
          <span class="pill blue">{{ stats.last7CreatedCount }} 篇新增</span>
        </div>
        <div ref="lineRef" class="chart-canvas line"></div>
      </article>

      <article class="panel list-panel">
        <div class="panel-head">
          <h2>浏览量最高 Top5</h2>
          <button type="button" @click="router.push('/notes/list')">查看全部</button>
        </div>
        <ul class="rank-list">
          <li v-for="(item, index) in stats.top5Views" :key="item.id" @click="viewNote(item.id)">
            <span class="rank">{{ index + 1 }}</span>
            <span class="name">{{ item.title || '无标题' }}</span>
            <span class="value">{{ item.viewCount ?? 0 }}</span>
          </li>
          <li v-if="!stats.top5Views.length" class="empty-row">暂无浏览记录</li>
        </ul>
      </article>

      <article class="panel list-panel">
        <div class="panel-head">
          <h2>{{ selectedMonth }} 消费 Top5</h2>
          <button type="button" @click="goToBudgets">预算设置</button>
        </div>
        <ul class="rank-list">
          <li v-for="(item, index) in expenseTop5" :key="item.categoryId || item.name">
            <span class="rank">{{ index + 1 }}</span>
            <span class="name">{{ item.name || `分类${item.categoryId}` }}</span>
            <span class="value">￥{{ formatAmount(item.total || 0) }}</span>
          </li>
          <li v-if="!expenseTop5.length" class="empty-row">暂无消费分类数据</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { useNoteStore } from '@/stores/note'
import { expenseApi } from '@/api'
import AiWeeklyReviewCard from '@/views/manager/home/AiWeeklyReviewCard.vue'

const router = useRouter()
const noteStore = useNoteStore()

const formatAmount = (a) => {
  try {
    return Number(a || 0).toFixed(2)
  } catch {
    return a
  }
}

const formatDate = (s) => {
  if (!s) return '暂无'
  try {
    const d = new Date(s)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch {
    return s
  }
}

const nowMonth = () => new Date().toISOString().slice(0, 7)

const selectedMonth = ref(nowMonth())
const selectedTagId = ref(null)
const selectedTagName = ref('')
const expenseTop5 = ref([])
const pieRef = ref(null)
const barRef = ref(null)
const lineRef = ref(null)

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

let pieChart = null
let barChart = null
let lineChart = null

const budgetPercent = computed(() => {
  const total = Number(stats.monthExpenseTotal || 0)
  const budget = Number(stats.budgets.global || 0)
  if (!budget || budget <= 0) return 0
  return Math.round((total / budget) * 100)
})

const budgetTone = computed(() => {
  if (budgetPercent.value >= 100) return 'red'
  if (budgetPercent.value >= Number(localStorage.getItem('budgetThreshold') || 80)) return 'amber'
  return 'green'
})

const metrics = computed(() => [
  { label: '笔记总数', value: stats.notesTotal, help: `最近更新：${stats.latestUpdate}` },
  { label: '标签数量', value: stats.tagsTotal, help: `热门标签：${stats.topTagName || '暂无'}` },
  { label: '本月支出', value: `￥${formatAmount(stats.monthExpenseTotal)}`, help: selectedMonth.value },
  { label: '预算使用率', value: `${budgetPercent.value}%`, help: stats.budgets.global ? `预算 ￥${formatAmount(stats.budgets.global)}` : '未设置全局预算' },
  { label: '近7天新增笔记', value: stats.last7CreatedCount, help: '用于观察近期记录活跃度' },
  { label: '平均字数', value: stats.averageWordCount, help: '按当前加载笔记估算' }
])

const priorityItems = computed(() => [
  {
    title: 'Vue 项目实践',
    meta: '+10%',
    tone: 'green',
    text: '建议今晚补充“组件通信”学习笔记，并同步到学习进度。'
  },
  {
    title: '餐饮预算',
    meta: `${budgetPercent.value || 0}%`,
    tone: budgetTone.value,
    text: budgetPercent.value >= 80 ? '本月消费接近预算阈值，需要回看分类支出。' : '预算压力正常，可以保持当前记录节奏。'
  },
  {
    title: '笔记关系',
    meta: `${stats.tagsTotal} 个`,
    tone: 'blue',
    text: stats.topTagName ? `“${stats.topTagName}” 是当前最活跃标签，可生成一次主题复盘。` : '还没有足够标签数据，建议先整理笔记分类。'
  }
])

const initCharts = () => {
  if (pieRef.value && !pieChart) pieChart = echarts.init(pieRef.value)
  if (barRef.value && !barChart) barChart = echarts.init(barRef.value)
  if (lineRef.value && !lineChart) lineChart = echarts.init(lineRef.value)

  pieChart?.setOption({
    color: ['#2563eb', '#059669', '#f59e0b', '#ef4444', '#8b5cf6'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#64748b' } },
    series: [{ type: 'pie', radius: ['42%', '70%'], data: [], label: { formatter: '{b}\n{d}%' } }]
  })

  barChart?.setOption({
    color: ['#2563eb'],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 24, bottom: 34 },
    xAxis: { type: 'category', data: [], axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
    series: [{ type: 'bar', data: [], barWidth: '46%', itemStyle: { borderRadius: [8, 8, 0, 0] } }]
  })

  barChart?.on('click', (params) => {
    const tagId = params?.data?.tagId
    if (tagId != null) {
      selectedTagId.value = tagId
      selectedTagName.value = params?.name || ''
      updateLineData(noteStore.currentPageNotes || [], tagId)
    }
  })

  lineChart?.setOption({
    color: ['#059669'],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 18, top: 24, bottom: 34 },
    xAxis: { type: 'category', data: [], axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
    series: [{ type: 'line', data: [], smooth: true, symbolSize: 7, areaStyle: { opacity: 0.12 } }]
  })
}

const updatePieData = (byCategory = []) => {
  const data = (byCategory || []).map(item => ({
    name: item.name || `分类${item.categoryId}`,
    value: Number(item.total || 0)
  }))
  pieChart?.setOption({ series: [{ data }] })
}

const updateBarData = (tags = []) => {
  const sorted = [...tags].sort((a, b) => (b.noteCount ?? b.useCount ?? 0) - (a.noteCount ?? a.useCount ?? 0)).slice(0, 10)
  const seriesData = sorted.map(t => ({ name: t.name, value: Number(t.noteCount ?? t.useCount ?? 0), tagId: t.id }))
  barChart?.setOption({
    xAxis: { data: sorted.map(t => t.name) },
    series: [{ data: seriesData }]
  })
}

const updateLineData = (notes = [], filterTagId = null) => {
  let filtered = notes || []
  if (filterTagId != null) {
    filtered = filtered.filter(n => Array.isArray(n.tags) && n.tags.some(t => t.id === filterTagId))
  }
  const days = []
  const mapDay = new Map()
  const today = new Date()
  for (let i = 29; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push(key)
    mapDay.set(key, 0)
  }
  for (const n of filtered) {
    const k = formatDate(n.updatedTime || n.updateTime || n.createdTime || n.createTime)
    if (mapDay.has(k)) mapDay.set(k, (mapDay.get(k) || 0) + 1)
  }
  lineChart?.setOption({
    xAxis: { data: days },
    series: [{ data: days.map(d => mapDay.get(d) || 0) }]
  })
}

const computeExtraMetrics = (notes = []) => {
  const now = new Date()
  const sevenDaysAgo = new Date(now)
  sevenDaysAgo.setDate(now.getDate() - 7)
  stats.last7CreatedCount = notes.filter(n => {
    const s = n.createdTime || n.createTime
    if (!s) return false
    return new Date(s) >= sevenDaysAgo
  }).length
  const counts = notes.map(n => Number(n.wordCount ?? 0))
  const sum = counts.reduce((acc, v) => acc + (Number.isFinite(v) ? v : 0), 0)
  stats.averageWordCount = counts.length > 0 ? Math.round(sum / counts.length) : 0
  stats.top5Views = [...notes]
    .sort((a, b) => Number(b.viewCount ?? 0) - Number(a.viewCount ?? 0))
    .slice(0, 5)
    .map(n => ({ id: n.id, title: n.title, viewCount: n.viewCount ?? 0 }))
}

const loadNotesAndTags = async () => {
  await Promise.all([
    noteStore.fetchNotes({ page: 1, size: 300 }),
    noteStore.fetchTags()
  ])
  const notes = noteStore.currentPageNotes || []
  const tags = noteStore.allTags || []
  stats.notesTotal = noteStore.totalNotes
  stats.tagsTotal = tags.length
  stats.topTagName = [...tags].sort((a, b) => (b.noteCount ?? b.useCount ?? 0) - (a.noteCount ?? a.useCount ?? 0))[0]?.name || ''
  const latest = notes
    .map(n => n.updatedTime || n.updateTime || n.createdTime || n.createTime)
    .filter(Boolean)
    .sort((a, b) => Date.parse(b) - Date.parse(a))[0]
  stats.latestUpdate = formatDate(latest)
  updateBarData(tags)
  updateLineData(notes)
  computeExtraMetrics(notes)
}

const loadExpenseStats = async () => {
  try {
    const month = selectedMonth.value
    const resStats = await expenseApi.getMonthStats(month)
    const data = resStats.data || {}
    stats.monthExpenseTotal = Number(data.total || 0)
    const byCategory = data.byCategory || []
    updatePieData(byCategory)
    expenseTop5.value = [...byCategory].sort((a, b) => Number(b.total || 0) - Number(a.total || 0)).slice(0, 5)

    const resBudgets = await expenseApi.getBudgets(month)
    const list = resBudgets.data || []
    const global = list.find(x => x.categoryId == null)
    stats.budgets.global = global ? Number(global.amount || 0) : 0
  } catch (error) {
    console.warn('首页消费统计加载失败:', error?.message || error)
    stats.monthExpenseTotal = 0
    stats.budgets.global = 0
    expenseTop5.value = []
    updatePieData([])
  }
}

const handleMonthChange = async () => {
  await loadExpenseStats()
}

const clearTagFilter = () => {
  selectedTagId.value = null
  selectedTagName.value = ''
  updateLineData(noteStore.currentPageNotes || [])
}

const resizeCharts = () => {
  pieChart?.resize()
  barChart?.resize()
  lineChart?.resize()
}

const goToNewNote = () => router.push('/notes/editor')
const goToBudgets = () => router.push('/life/budgets')
const viewNote = (id) => {
  if (id != null) router.push(`/notes/detail/${id}`)
}

onMounted(async () => {
  await nextTick()
  initCharts()
  await Promise.allSettled([loadNotesAndTags(), loadExpenseStats()])
  await nextTick()
  resizeCharts()
  window.requestAnimationFrame(() => resizeCharts())
  window.setTimeout(() => resizeCharts(), 250)
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  pieChart?.dispose()
  barChart?.dispose()
  lineChart?.dispose()
})
</script>

<style scoped>
.home-redesign {
  display: grid;
  gap: 18px;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.75fr);
  gap: 18px;
  border: 1px solid #d8e2ed;
  border-radius: 22px;
  padding: 24px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(239, 246, 255, 0.94)),
    radial-gradient(circle at 90% 12%, rgba(37, 99, 235, 0.14), transparent 34%);
}

.eyebrow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  color: #047857;
  font-size: 12px;
  font-weight: 800;
}

.pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border-radius: 999px;
  padding: 2px 10px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.pill.blue {
  background: #dbeafe;
  color: #1d4ed8;
}

.pill.green {
  background: #dcfce7;
  color: #047857;
}

.pill.amber {
  background: #fef3c7;
  color: #b45309;
}

.pill.red {
  background: #fee2e2;
  color: #b91c1c;
}

.hero-copy .hero-title {
  max-width: 980px;
  margin: 14px 0 12px;
  color: #0b1220;
  font-size: clamp(38px, 4.4vw, 64px);
  line-height: 1.02;
  letter-spacing: 0;
  text-wrap: balance;
}

.hero-title span {
  display: block;
}

.hero-title em {
  position: relative;
  display: inline-block;
  margin-right: 0.18em;
  color: #1d4ed8;
  font-style: normal;
  isolation: isolate;
}

.hero-title em::after {
  content: '';
  position: absolute;
  left: -0.04em;
  right: -0.04em;
  bottom: 0.06em;
  z-index: -1;
  height: 0.22em;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.16);
}

.hero-copy p {
  max-width: 760px;
  margin: 0;
  color: #526173;
  font-size: 15px;
  line-height: 1.85;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.hero-actions button,
.panel-head button,
.filter-chip button {
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #ffffff;
  color: #0f172a;
  cursor: pointer;
  font: inherit;
  font-weight: 700;
}

.hero-actions button {
  min-height: 42px;
  padding: 0 16px;
}

.hero-actions .primary-action {
  border-color: #0f172a;
  background: #0f172a;
  color: #ffffff;
}

.priority-stack {
  display: grid;
  gap: 10px;
}

.priority-card {
  border: 1px solid #d8e2ed;
  border-radius: 16px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.82);
}

.priority-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.priority-card p {
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.7;
  font-size: 13px;
}

.control-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid #d8e2ed;
  border-radius: 18px;
  padding: 14px;
  background: #ffffff;
}

.control-strip > div:first-child {
  display: flex;
  align-items: center;
  gap: 10px;
}

.control-strip label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.filter-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.filter-chip button,
.panel-head button {
  min-height: 32px;
  padding: 0 10px;
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.panel {
  border: 1px solid #d8e2ed;
  border-radius: 18px;
  background: #ffffff;
}

.metric-card {
  min-height: 118px;
  padding: 16px;
}

.metric-card label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.metric-value {
  margin-top: 10px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 850;
  font-variant-numeric: tabular-nums;
}

.metric-card small {
  display: block;
  margin-top: 8px;
  color: #64748b;
  line-height: 1.5;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 16px;
}

.panel {
  padding: 16px;
  overflow: hidden;
}

.weekly-panel {
  grid-column: span 12;
  padding: 0;
}

.weekly-panel :deep(.weekly-card) {
  border: 0;
  border-radius: 18px;
  box-shadow: none;
}

.chart-panel {
  grid-column: span 6;
}

.chart-panel.wide {
  grid-column: span 12;
}

.list-panel {
  grid-column: span 6;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.panel-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.panel-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.chart-canvas {
  width: 100%;
  height: 300px;
}

.chart-canvas.line {
  height: 320px;
}

.rank-list {
  list-style: none;
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
}

.rank-list li {
  min-height: 42px;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  padding: 0 10px;
  background: #f8fafc;
  color: #334155;
}

.rank-list li:not(.empty-row) {
  cursor: pointer;
}

.rank {
  width: 24px;
  height: 24px;
  display: inline-grid;
  place-items: center;
  border-radius: 8px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
  font-weight: 800;
}

.name {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.value {
  color: #0f172a;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.empty-row {
  grid-template-columns: 1fr !important;
  color: #94a3b8;
}

@media (max-width: 1280px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .metric-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .chart-panel,
  .chart-panel.wide,
  .list-panel {
    grid-column: auto;
  }
}

/* image-reference dashboard pass */
.home-redesign {
  gap: 14px;
}

.hero-panel {
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 14px;
  border-radius: 8px;
  padding: 16px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(33, 54, 86, 0.05);
}

.eyebrow {
  color: #4f6075;
  font-size: 12px;
  letter-spacing: 0;
}

.hero-heading-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.hero-copy .hero-title {
  margin: 0;
  color: #172033;
  font-size: 28px;
  line-height: 1.18;
  font-weight: 850;
}

.hero-copy p {
  max-width: 780px;
  margin-top: 10px;
  color: #66758b;
  font-size: 14px;
  line-height: 1.7;
}

.hero-actions {
  margin-top: 14px;
  gap: 8px;
}

.hero-actions button {
  min-height: 34px;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
}

.hero-actions .primary-action {
  border-color: #1f6feb;
  background: #1f6feb;
}

.priority-stack {
  align-content: start;
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  padding: 12px;
  background: #f7fafc;
}

.stack-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #172033;
  font-size: 13px;
}

.stack-head span {
  color: #66758b;
  font-size: 12px;
}

.priority-card {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr);
  gap: 10px;
  border-radius: 8px;
  padding: 10px;
  background: #ffffff;
}

.priority-dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: #1f6feb;
}

.priority-dot.green {
  background: #16a34a;
}

.priority-dot.amber {
  background: #eab308;
}

.priority-dot.red {
  background: #ef4444;
}

.priority-card p {
  font-size: 12px;
  line-height: 1.65;
}

.control-strip,
.metric-card,
.panel {
  border-color: #d7e1ec;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(33, 54, 86, 0.04);
}

.control-strip {
  min-height: 58px;
  padding: 10px 12px;
}

.metric-grid {
  grid-template-columns: repeat(6, minmax(150px, 1fr));
  gap: 10px;
}

.metric-card {
  min-height: 102px;
  padding: 14px;
}

.metric-card label {
  color: #66758b;
  font-size: 12px;
}

.metric-value {
  margin-top: 8px;
  color: #172033;
  font-size: 25px;
}

.metric-card small {
  color: #66758b;
  font-size: 12px;
}

.dashboard-grid {
  gap: 14px;
}

.panel {
  padding: 14px;
}

.weekly-panel {
  border-radius: 8px;
}

.weekly-panel :deep(.weekly-card),
.weekly-panel :deep(.el-card) {
  border-radius: 8px !important;
}

.panel-head h2 {
  color: #172033;
  font-size: 16px;
}

.panel-head p {
  color: #66758b;
  font-size: 12px;
}

.chart-canvas {
  height: 280px;
}

.chart-canvas.line {
  height: 300px;
}

.rank-list li {
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  background: #f8fbfd;
}

.rank {
  border-radius: 6px;
  background: #e9f2ff;
  color: #1557c0;
}

.pill {
  border-radius: 999px;
}

@media (max-width: 1280px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }
}

/* richer home preview panel */
.hero-panel {
  position: relative;
  overflow: hidden;
  min-height: 360px;
  grid-template-columns: minmax(0, 1.35fr) 520px;
  align-items: stretch;
  padding: 18px;
  border-color: rgba(130, 156, 196, 0.5);
  background:
    linear-gradient(135deg, rgba(12, 22, 40, 0.96) 0%, rgba(24, 49, 86, 0.94) 45%, rgba(7, 83, 91, 0.90) 100%),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.05) 0 1px, transparent 1px 52px),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.04) 0 1px, transparent 1px 52px);
  box-shadow: 0 18px 44px rgba(33, 54, 86, 0.16);
}

.hero-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, transparent 0 36%, rgba(74, 222, 128, 0.10) 36% 37%, transparent 37%),
    linear-gradient(145deg, transparent 0 63%, rgba(96, 165, 250, 0.14) 63% 64%, transparent 64%);
  pointer-events: none;
}

.hero-copy,
.priority-stack {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: grid;
  align-content: center;
  min-height: 320px;
  border: 1px solid rgba(224, 236, 255, 0.12);
  border-radius: 8px;
  padding: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.10), rgba(255, 255, 255, 0.04)),
    rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
}

.hero-copy .eyebrow {
  color: #9ae6b4;
}

.hero-copy .pill.blue {
  background: rgba(96, 165, 250, 0.18);
  color: #bfdbfe;
}

.hero-heading-row {
  align-items: flex-start;
}

.hero-copy .hero-title {
  color: #ffffff;
  font-size: clamp(38px, 3.2vw, 58px);
  line-height: 1;
  text-shadow: 0 10px 28px rgba(0, 0, 0, 0.24);
}

.hero-copy p {
  max-width: 860px;
  color: #dbeafe;
  font-size: 15px;
}

.hero-heading-row > .pill {
  min-height: 34px;
  border: 1px solid rgba(187, 247, 208, 0.28);
  background: rgba(187, 247, 208, 0.16);
  color: #bbf7d0;
  font-weight: 900;
}

.hero-snapshot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 22px;
}

.hero-snapshot-grid article {
  min-height: 96px;
  display: grid;
  align-content: space-between;
  gap: 8px;
  border: 1px solid rgba(219, 234, 254, 0.16);
  border-radius: 8px;
  padding: 13px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.06));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

.hero-snapshot-grid span {
  color: #bfdbfe;
  font-size: 12px;
  font-weight: 800;
}

.hero-snapshot-grid strong {
  min-width: 0;
  overflow: hidden;
  color: #ffffff;
  font-size: 24px;
  line-height: 1.1;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-snapshot-grid small {
  min-width: 0;
  overflow: hidden;
  color: #93c5fd;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-actions {
  margin-top: 18px;
}

.hero-actions button {
  min-height: 40px;
  border-color: rgba(219, 234, 254, 0.24);
  background: rgba(255, 255, 255, 0.10);
  color: #eff6ff;
  backdrop-filter: blur(8px);
}

.hero-actions button:hover {
  border-color: rgba(147, 197, 253, 0.72);
  background: rgba(96, 165, 250, 0.18);
}

.hero-actions .primary-action {
  border-color: #60a5fa;
  background: linear-gradient(135deg, #2563eb, #0891b2);
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.28);
}

.priority-stack {
  align-content: stretch;
  min-height: 320px;
  border-color: rgba(219, 234, 254, 0.16);
  padding: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
  backdrop-filter: blur(12px);
}

.stack-head {
  min-height: 38px;
  color: #ffffff;
}

.stack-head span {
  color: #bfdbfe;
}

.priority-card {
  min-height: 82px;
  border: 1px solid rgba(219, 234, 254, 0.16);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
  color: #ffffff;
}

.priority-card header strong {
  color: #ffffff;
}

.priority-card p {
  color: #cbd5e1;
}

.priority-card .pill {
  background: rgba(255, 255, 255, 0.14);
  color: #e0f2fe;
}

.priority-dot {
  box-shadow: 0 0 0 5px rgba(96, 165, 250, 0.14);
}

.priority-dot.green {
  box-shadow: 0 0 0 5px rgba(34, 197, 94, 0.14);
}

.priority-dot.amber {
  box-shadow: 0 0 0 5px rgba(234, 179, 8, 0.14);
}

.priority-dot.red {
  box-shadow: 0 0 0 5px rgba(239, 68, 68, 0.14);
}

.metric-card {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, #ffffff, #f4f9ff) !important;
}

.metric-card::after {
  content: '';
  position: absolute;
  inset: auto 14px 0;
  height: 3px;
  border-radius: 999px 999px 0 0;
  background: linear-gradient(90deg, #2563eb, #14b8a6, #22c55e);
}

@media (max-width: 1440px) {
  .hero-panel {
    grid-template-columns: minmax(0, 1fr) 470px;
  }

  .hero-snapshot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1280px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }
}
</style>
