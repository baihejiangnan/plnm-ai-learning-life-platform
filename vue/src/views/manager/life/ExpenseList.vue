<template>
  <div class="expense-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><PriceTag /></el-icon>
          消费记录
        </h1>
        <p class="page-subtitle">管理与分析每月的消费数据</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="openCreate" class="create-btn">
          <el-icon><Plus /></el-icon>
          新建消费
        </el-button>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="月份">
          <el-date-picker v-model="filters.month" type="month" placeholder="选择月份" value-format="YYYY-MM" @change="handleFilterChange" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" clearable placeholder="选择分类" @change="handleFilterChange" style="min-width:200px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAll">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 内容区域 -->
    <div class="content-section">
      <el-row :gutter="16">
        <el-col :xl="15" :lg="14" :md="24" :sm="24" :xs="24">
          <el-card class="box-card">
            <template #header>
              <div class="card-header">
                <span>消费明细</span>
              </div>
            </template>

            <el-table :data="expenseList" v-loading="loading.list" style="width: 100%">
              <el-table-column prop="expenseTime" label="时间" width="180">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row.expenseTime) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="categoryId" label="分类" width="160">
                <template #default="{ row }">
                  <span>{{ findCategoryName(row.categoryId) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="金额(￥)" width="120">
                <template #default="{ row }">
                  <span class="amount">{{ formatAmount(row.amount) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="note" label="备注" />
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button size="small" class="action-edit" @click="openEdit(row)">编辑</el-button>
                  <el-button size="small" class="action-delete" type="danger" @click="confirmDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper" v-if="total > 0">
              <el-pagination
                v-model:current-page="page"
                v-model:page-size="size"
                :total="total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </el-card>
        </el-col>
        <el-col :xl="9" :lg="10" :md="24" :sm="24" :xs="24">
          <el-card class="box-card">
            <template #header>
              <div class="card-header">
                <span>月度统计</span>
              </div>
            </template>
            <div class="stats-grid">
              <div class="stat-card">
                <span class="stat-label">本月总支出</span>
                <span class="stat-value">￥{{ formatAmount(monthTotal) }}</span>
              </div>
              <div class="stat-card">
                <span class="stat-label">预算使用率</span>
                <span class="stat-value">{{ budgets.global > 0 ? `${Math.round(globalPercent)}%` : '--' }}</span>
              </div>
            </div>
            <!-- 预算预警与进度 -->
            <div class="budget-summary">
              <div class="stat-item">
                <span class="stat-label">月预算：</span>
                <span class="stat-value">{{ budgets.global > 0 ? `￥${formatAmount(budgets.global)}` : '未设置' }}</span>
              </div>
              <el-progress v-if="budgets.global > 0" :percentage="Math.min(999, Math.round(globalPercent))" :status="globalStatus === 'over' ? 'exception' : (globalStatus === 'warn' ? 'warning' : undefined)" />
              <el-alert v-if="globalStatus==='over'" type="error" show-icon title="已超出月预算" description="请检查消费或调整预算设置" class="mt8"/>
              <el-alert v-else-if="globalStatus==='warn'" type="warning" show-icon title="接近月预算" description="已达到提醒阈值，请注意控制支出" class="mt8"/>
              <div v-if="categoryWarnings.length" class="category-warnings mt8">
                <div class="warn-title">分类预警</div>
                <el-tag v-for="w in categoryWarnings" :key="w.categoryId" :type="w.status==='over'?'danger':'warning'" effect="light" class="mr6 mb6">
                  {{ w.name }}：{{ formatAmount(w.used) }}/{{ formatAmount(w.budget) }} ({{ Math.round(w.percent) }}%)
                </el-tag>
              </div>
            </div>
            <div ref="pieRef" class="pie-chart"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 编辑/新建抽屉 -->
    <el-drawer v-model="drawer.visible" :title="drawerTitle" size="40%" append-to-body class="expense-drawer">
      <div class="drawer-intro">
        <span class="intro-title">记录一次消费</span>
        <span class="intro-sub">建议按“时间 → 分类 → 金额 → 备注”的顺序填写</span>
      </div>
      <el-form :model="form" label-width="100px" class="form-body">
        <el-form-item label="时间" class="drawer-item">
          <el-date-picker v-model="form.expenseTime" type="datetime" placeholder="选择时间" value-format="YYYY-MM-DD HH:mm:ss" class="full-width" />
        </el-form-item>
        <el-form-item label="分类" class="drawer-item">
          <el-select v-model="form.categoryId" placeholder="选择分类" class="full-width">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额(￥)" class="drawer-item">
          <el-input
            v-model="form.amount"
            placeholder="0.00"
            class="full-width amount-input"
            :formatter="formatMoneyInput"
            :parser="parseMoneyInput"
            inputmode="decimal"
          />
        </el-form-item>
        <el-form-item label="备注" class="drawer-item">
          <el-input type="textarea" v-model="form.note" placeholder="填写备注" :rows="4" class="full-width" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawer.visible=false">取消</el-button>
          <el-button type="primary" :loading="loading.save" @click="save">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PriceTag, Plus } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { expenseApi, expenseCategoryApi } from '@/api'

// 状态
const categories = ref([])
const expenseList = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const monthTotal = ref(0)
const filters = reactive({ month: '', categoryId: null })
const loading = reactive({ list: false, save: false, stats: false })

// 预算与预警
const budgets = reactive({ global: 0, byCategory: new Map() })
const statsByCategory = ref([])
const warnings = reactive({ globalPercent: 0, globalStatus: 'ok', category: [] })
const globalPercent = computed(() => warnings.globalPercent)
const globalStatus = computed(() => warnings.globalStatus)
const categoryWarnings = computed(() => warnings.category)

const getThreshold = () => {
  const v = Number(localStorage.getItem('budgetThreshold') || 100)
  return Number.isFinite(v) && v > 0 ? v : 100
}

const computeWarnings = () => {
  // 全局
  if (budgets.global > 0) {
    const percent = Number(budgets.global) > 0 ? (Number(monthTotal.value || 0) / Number(budgets.global)) * 100 : 0
    warnings.globalPercent = isFinite(percent) ? percent : 0
    const threshold = getThreshold()
    warnings.globalStatus = percent >= 100 ? 'over' : (percent >= threshold ? 'warn' : 'ok')
  } else {
    warnings.globalPercent = 0
    warnings.globalStatus = 'ok'
  }
  // 分类
  const threshold = getThreshold()
  const arr = []
  for (const item of statsByCategory.value || []) {
    const catId = item.categoryId
    const used = Number(item.total || 0)
    const budget = Number(budgets.byCategory.get(catId) || 0)
    if (budget > 0) {
      const p = (used / budget) * 100
      if (p >= threshold) {
        arr.push({
          categoryId: catId,
          name: item.name || `分类${catId}`,
          used,
          budget,
          percent: p,
          status: p >= 100 ? 'over' : 'warn'
        })
      }
    }
  }
  arr.sort((a, b) => b.percent - a.percent)
  warnings.category = arr
}

const drawer = reactive({ visible: false, isEdit: false })
const form = reactive({ id: null, expenseTime: '', categoryId: null, amount: '', note: '' })

// 图表
const pieRef = ref(null)
let pieChart = null

const initChart = () => {
  if (!pieRef.value) return
  pieChart = echarts.init(pieRef.value)
  const opt = {
    tooltip: { trigger: 'item' },
    legend: { top: 'bottom' },
    series: [
      {
        name: '分类占比',
        type: 'pie',
        radius: '65%',
        data: [],
        label: { formatter: '{b}: {d}%' }
      }
    ]
  }
  pieChart.setOption(opt)
}

const updatePieData = (byCategory = []) => {
  if (!pieChart) return
  const data = (byCategory || []).map(item => ({
    name: item.name || `分类${item.categoryId}`,
    value: Number(item.total || 0)
  }))
  pieChart.setOption({ series: [{ data }] })
}

// 工具
const formatAmount = (a) => {
  try { return Number(a || 0).toFixed(2) } catch { return a }
}
const formatDateTime = (s) => {
  if (!s) return ''
  try { return new Date(s).toLocaleString() } catch { return s }
}
const formatMoneyInput = (value) => {
  if (value === undefined || value === null || value === '') return ''
  const raw = String(value).replace(/,/g, '')
  if (raw === '-' || raw === '.' || raw === '-.') return raw
  const negative = raw.startsWith('-')
  const normalized = raw.replace(/^-/, '')
  const [integerPart = '0', decimalPart = ''] = normalized.split('.')
  const digits = integerPart.replace(/\D/g, '') || '0'
  const grouped = digits.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  const decimals = decimalPart.replace(/\D/g, '').slice(0, 2)
  const prefix = negative ? '-' : ''
  return decimals ? `${prefix}${grouped}.${decimals}` : `${prefix}${grouped}`
}
const parseMoneyInput = (value) => {
  if (!value) return ''
  let sanitized = String(value).replace(/,/g, '').replace(/[^\d.-]/g, '')
  sanitized = sanitized.replace(/(?!^)-/g, '')
  const [head, ...tail] = sanitized.split('.')
  const decimal = tail.join('').slice(0, 2)
  return tail.length ? `${head}.${decimal}` : head
}
const findCategoryName = (id) => {
  const c = categories.value.find(x => x.id === id)
  return c ? c.name : `#${id ?? ''}`
}

// 加载数据
const loadCategories = async () => {
  try {
    const res = await expenseCategoryApi.getList()
    categories.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const loadExpenses = async () => {
  loading.list = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.month) params.month = filters.month
    if (filters.categoryId != null) params.categoryId = filters.categoryId
    const res = await expenseApi.getList(params)
    const pageInfo = res.data || {}
    expenseList.value = pageInfo.list || []
    total.value = pageInfo.total || 0
    page.value = pageInfo.pageNum || page.value
    size.value = pageInfo.pageSize || size.value
  } catch (e) {
    console.error(e)
  } finally {
    loading.list = false
  }
}

const loadStats = async () => {
  loading.stats = true
  try {
    const month = filters.month || new Date().toISOString().slice(0, 7)
    const res = await expenseApi.getMonthStats(month)
    const data = res.data || {}
    monthTotal.value = data.total || 0
    statsByCategory.value = data.byCategory || []
    updatePieData(data.byCategory || [])
  } catch (e) {
    console.error(e)
  } finally {
    loading.stats = false
  }
}

const loadBudgets = async () => {
  try {
    const month = filters.month || new Date().toISOString().slice(0, 7)
    const res = await expenseApi.getBudgets(month)
    const list = res.data || []
    const global = list.find(x => x.categoryId == null)
    budgets.global = global ? Number(global.amount || 0) : 0
    const map = new Map()
    for (const x of list) {
      if (x.categoryId != null) map.set(x.categoryId, Number(x.amount || 0))
    }
    budgets.byCategory = map
  } catch (e) {
    console.error(e)
  }
}

const loadAll = async () => {
  await Promise.all([loadExpenses(), loadStats(), loadBudgets()])
  computeWarnings()
}

// 交互
const openCreate = () => {
  drawer.isEdit = false
  Object.assign(form, { id: null, expenseTime: '', categoryId: null, amount: '', note: '' })
  drawer.visible = true
}

const openEdit = (row) => {
  drawer.isEdit = true
  Object.assign(form, {
    id: row.id,
    expenseTime: row.expenseTime,
    categoryId: row.categoryId,
    amount: row.amount,
    note: row.note
  })
  drawer.visible = true
}

const save = async () => {
  loading.save = true
  try {
    const payload = {
      expenseTime: form.expenseTime,
      categoryId: form.categoryId,
      amount: Number(form.amount || 0),
      note: form.note
    }
    if (drawer.isEdit && form.id) {
      const res = await expenseApi.update(form.id, payload)
      if (res.code === 200 || res.code === '200') {
        ElMessage.success('更新成功')
        drawer.visible = false
        loadAll()
      }
    } else {
      const res = await expenseApi.create(payload)
      if (res.code === 200 || res.code === '200') {
        ElMessage.success('创建成功')
        drawer.visible = false
        loadAll()
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.save = false
  }
}

const confirmDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除该条消费记录吗？`, '删除确认', { type: 'warning' })
    const res = await expenseApi.delete(row.id)
    if (res.code === 200 || res.code === '200') {
      ElMessage.success('删除成功')
      loadAll()
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleSizeChange = (val) => { size.value = val; loadExpenses() }
const handleCurrentChange = (val) => { page.value = val; loadExpenses() }
const handleFilterChange = () => { page.value = 1; loadAll() }
const resetFilters = () => { filters.month = ''; filters.categoryId = null; page.value = 1; loadAll() }

onMounted(async () => {
  // 默认月份
  filters.month = new Date().toISOString().slice(0, 7)
  initChart()
  await loadCategories()
  await loadAll()
})

// 监听尺寸变化以自适应图表
watch(() => drawer.visible, () => { if (pieChart) pieChart.resize() })
window.addEventListener('resize', () => { if (pieChart) pieChart.resize() })
</script>

<style scoped>
.expense-page {
  padding: 16px;
  background: linear-gradient(180deg, #f7fafc 0%, #edf5ff 100%);
}
.page-header {
  display:flex;
  justify-content:space-between;
  align-items:flex-start;
  padding:24px 24px 18px;
  background: linear-gradient(135deg, #34d399 0%, #0ea5e9 100%);
  color:#fff;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(14, 165, 233, 0.24);
}
.header-left { flex:1; }
.page-title { display:flex; align-items:center; gap:10px; font-size:22px; font-weight:600; margin:0 0 6px 0; }
.title-icon { font-size:28px; }
.page-subtitle { margin:0; opacity:0.9; }
.header-right { display:flex; align-items:center; }
.create-btn { box-shadow: 0 4px 12px rgba(14,165,233,0.3); }

.filter-section {
  margin-top: 14px;
  padding: 16px 18px 4px;
  border: 1px solid #e6edf9;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}
.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}
.content-section { padding: 16px 0 0; }
.box-card {
  margin-bottom: 16px;
  border: 1px solid #e6edf9;
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}
.box-card :deep(.el-card__header) {
  border-bottom: 1px solid #edf2fb;
  background: #fbfdff;
}
.card-header { display:flex; align-items:center; justify-content:space-between; }

.amount { color:#ef4444; font-weight:600; }
.action-edit {
  border-color: #cfe0ff;
  color: #245ad6;
  background: #f3f7ff;
}
.action-delete {
  border-color: #ffd8d8;
  color: #d93025;
  background: #fff4f4;
}
.pagination-wrapper { display:flex; justify-content:flex-end; padding-top: 10px; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 4px 0 16px;
}
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
  border: 1px solid #dfe8fb;
}
.stat-item { font-size:14px; color:#374151; }
.stat-label { color:#6b7280; margin-right:6px; font-size: 13px; }
.stat-value { font-weight:600; color: #1f2937; }
.pie-chart { width: 100%; height: 320px; }

.form-body { padding-right: 16px; }
.drawer-intro {
  margin: 2px 0 16px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #dbe7ff;
  background: linear-gradient(135deg, #f8fbff 0%, #eef4ff 100%);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.intro-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c4aa6;
}
.intro-sub {
  font-size: 12px;
  color: #5f6b8c;
}
.expense-drawer :deep(.el-drawer__header) {
  margin-bottom: 8px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2fb;
  font-weight: 600;
  color: #243150;
}
.expense-drawer :deep(.el-drawer__body) {
  padding-top: 10px;
}
.expense-drawer :deep(.el-drawer__footer) {
  border-top: 1px solid #edf2fb;
  padding-top: 14px;
}
.drawer-item :deep(.el-form-item__label) {
  color: #42506f;
  font-weight: 500;
}
.drawer-item :deep(.el-input__wrapper),
.drawer-item :deep(.el-textarea__inner),
.drawer-item :deep(.el-input-number),
.drawer-item :deep(.el-select__wrapper) {
  border-radius: 10px;
}
.amount-input :deep(.el-input__inner) {
  text-align: right;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: #1f2937;
  letter-spacing: 0.2px;
}
.amount-input :deep(.el-input__wrapper) {
  border-color: #d6e2ff;
}
.amount-input :deep(.el-input__wrapper.is-focus) {
  border-color: #4f8cff;
  box-shadow: 0 0 0 3px rgba(79, 140, 255, 0.16);
  background: #fafdff;
}
.full-width {
  width: 100%;
}
.drawer-footer {
  text-align:right;
}
.budget-summary { padding: 8px 0 0; }
.mt8 { margin-top: 8px; }
.mr6 { margin-right: 6px; }
.mb6 { margin-bottom: 6px; display:inline-block; }
.category-warnings { display:flex; flex-wrap: wrap; align-items: center; gap: 4px; }
.warn-title { font-size: 13px; color:#6b7280; margin-right: 6px; }

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 992px) {
  .page-header {
    flex-direction: column;
    gap: 14px;
  }
  .content-section :deep(.el-col) {
    margin-bottom: 12px;
  }
}

@media (max-width: 768px) {
  .expense-page {
    padding: 12px;
  }
  .page-header {
    padding: 18px 16px 16px;
  }
  .filter-section {
    padding: 12px 12px 2px;
  }
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .expense-drawer :deep(.el-drawer) {
    width: 92% !important;
  }
}
</style>
