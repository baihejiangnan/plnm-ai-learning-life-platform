<template>
  <div class="audit-page">
    <section class="audit-hero">
      <div class="hero-left">
        <el-icon class="hero-icon"><DataAnalysis /></el-icon>
        <div class="hero-text">
          <h2>AI 审计日志</h2>
          <p>追踪多角色对话、执行结果、延迟和上下文调用记录</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchList">
          <el-icon><RefreshRight /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="resetFilters">
          <el-icon><Filter /></el-icon>
          重置筛选
        </el-button>
      </div>
    </section>

    <section class="audit-summary">
      <article v-for="item in summaryCards" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.help }}</small>
      </article>
    </section>

    <section class="audit-filters">
      <el-select v-model="filters.roleId" placeholder="角色" clearable>
        <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <el-select v-model="filters.success" placeholder="结果" clearable>
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-date-picker
        v-model="filters.range"
        type="datetimerange"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DD HH:mm:ss"
      />
      <el-input v-model="filters.keyword" placeholder="搜索请求、响应或 traceId" clearable @keyup.enter="applyFilters" />
      <el-button type="primary" :loading="loading" @click="applyFilters">查询</el-button>
    </section>

    <section class="audit-workbench" v-loading="loading">
      <div class="audit-list-panel">
        <div class="list-head">
          <strong>日志列表</strong>
          <span>{{ total }} 条</span>
        </div>
        <div class="audit-list">
          <button
            v-for="row in list"
            :key="row.id"
            type="button"
            :class="['audit-row', { active: selectedLog?.id === row.id }]"
            @click="selectLog(row)"
          >
            <span :class="['status-dot', row.success === 1 ? 'success' : 'danger']"></span>
            <span class="row-main">
              <span class="row-title">
                <b>{{ roleLabel(row.roleId) }}</b>
                <em>{{ row.actionType || '-' }}</em>
              </span>
              <span class="row-preview">{{ row.requestText || '无请求内容' }}</span>
            </span>
            <span class="row-side">
              <span>{{ formatShortTime(row.createdTime) }}</span>
              <strong>{{ formatLatency(row.latencyMs) }}</strong>
            </span>
          </button>
          <el-empty v-if="!list.length && !loading" description="暂无审计记录" />
        </div>
        <div class="pager">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="page"
            @size-change="onSizeChange"
            @current-change="onPageChange"
          />
        </div>
      </div>

      <aside class="audit-detail-panel">
        <template v-if="selectedLog">
          <div class="detail-head">
            <div>
              <span class="detail-kicker">Trace</span>
              <h3>{{ selectedLog.traceId || `#${selectedLog.id}` }}</h3>
            </div>
            <el-tag :type="selectedLog.success === 1 ? 'success' : 'danger'">
              {{ selectedLog.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </div>

          <div class="detail-grid">
            <div>
              <span>角色</span>
              <strong>{{ roleLabel(selectedLog.roleId) }}</strong>
            </div>
            <div>
              <span>模块</span>
              <strong>{{ selectedLog.module || '-' }}</strong>
            </div>
            <div>
              <span>类型</span>
              <strong>{{ selectedLog.actionType || '-' }}</strong>
            </div>
            <div>
              <span>耗时</span>
              <strong>{{ formatLatency(selectedLog.latencyMs) }}</strong>
            </div>
          </div>

          <section class="detail-block">
            <header>
              <strong>请求内容</strong>
              <el-button text size="small" @click="copyText(selectedLog.requestText)">复制</el-button>
            </header>
            <pre>{{ selectedLog.requestText || '-' }}</pre>
          </section>

          <section class="detail-block">
            <header>
              <strong>响应内容</strong>
              <el-button text size="small" @click="copyText(selectedLog.responseText)">复制</el-button>
            </header>
            <pre>{{ selectedLog.responseText || '-' }}</pre>
          </section>

          <div class="detail-footer">
            <span>{{ formatTime(selectedLog.createdTime) }}</span>
            <el-button text @click="copyText(selectedLog.traceId)">复制 Trace</el-button>
          </div>
        </template>
        <el-empty v-else description="选择一条日志查看详情" />
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DataAnalysis, Filter, RefreshRight } from '@element-plus/icons-vue'
import { aiApi } from '@/api'
import { ElMessage } from 'element-plus'

const list = ref([])
const roles = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedLog = ref(null)
const filters = ref({
  roleId: '',
  success: null,
  keyword: '',
  range: []
})

const roleLabel = (roleId) => {
  const r = roles.value.find(i => i.id === roleId)
  return r ? r.name : roleId || '-'
}

const successCount = computed(() => list.value.filter(i => i.success === 1).length)
const failedCount = computed(() => list.value.filter(i => i.success !== 1).length)
const avgLatency = computed(() => {
  const values = list.value.map(i => Number(i.latencyMs || 0)).filter(i => Number.isFinite(i) && i > 0)
  if (!values.length) return 0
  return Math.round(values.reduce((sum, item) => sum + item, 0) / values.length)
})
const summaryCards = computed(() => [
  { label: '当前页记录', value: list.value.length, help: `总计 ${total.value} 条` },
  { label: '成功', value: successCount.value, help: '当前筛选结果' },
  { label: '失败', value: failedCount.value, help: '需要重点复查' },
  { label: '平均耗时', value: formatLatency(avgLatency.value), help: '当前页估算' }
])

const formatTime = (value) => {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}

const formatShortTime = (value) => {
  if (!value) return '-'
  try {
    const d = new Date(value)
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch {
    return value
  }
}

const formatLatency = (value) => {
  const ms = Number(value || 0)
  if (!Number.isFinite(ms) || ms <= 0) return '-'
  if (ms >= 1000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.round(ms)}ms`
}

const buildParams = () => {
  const params = {
    page: page.value,
    size: pageSize.value
  }
  if (filters.value.roleId) params.roleId = filters.value.roleId
  if (filters.value.success !== null && filters.value.success !== undefined && filters.value.success !== '') {
    params.success = filters.value.success
  }
  if (filters.value.keyword) params.keyword = filters.value.keyword
  if (Array.isArray(filters.value.range) && filters.value.range.length === 2) {
    params.startTime = filters.value.range[0]
    params.endTime = filters.value.range[1]
  }
  return params
}

const fetchRoles = async () => {
  try {
    const res = await aiApi.getRoles()
    if (res.code === 200 || res.code === '200') {
      roles.value = res.data || []
    }
  } catch {}
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await aiApi.getAuditLogs(buildParams())
    if (res.code === 200 || res.code === '200') {
      const data = res.data || {}
      list.value = data.list || []
      total.value = data.total || 0
      selectedLog.value = list.value[0] || null
    } else {
      ElMessage.error(res.msg || '获取日志失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取日志失败')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  page.value = 1
  fetchList()
}

const selectLog = (row) => {
  selectedLog.value = row
}

const copyText = async (text) => {
  if (!text) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(String(text))
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const onSizeChange = (size) => {
  pageSize.value = size
  page.value = 1
  fetchList()
}

const onPageChange = (p) => {
  page.value = p
  fetchList()
}

const resetFilters = () => {
  filters.value = { roleId: '', success: null, keyword: '', range: [] }
  page.value = 1
  fetchList()
}

onMounted(async () => {
  await fetchRoles()
  await fetchList()
})
</script>

<style scoped>
.audit-page {
  display: grid;
  gap: 14px;
}

.audit-hero,
.audit-filters,
.audit-workbench,
.summary-card,
.audit-list-panel,
.audit-detail-panel {
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(33, 54, 86, 0.05);
}

.audit-hero {
  min-height: 86px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: inline-grid;
  place-items: center;
  color: #1f6feb;
  background: #e9f2ff;
  font-size: 22px;
}

.hero-text h2 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  line-height: 1.2;
}

.hero-text p {
  margin: 6px 0 0;
  color: #66758b;
  font-size: 13px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.audit-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.summary-card {
  min-height: 92px;
  display: grid;
  gap: 6px;
  padding: 14px;
}

.summary-card span,
.summary-card small {
  color: #66758b;
  font-size: 12px;
}

.summary-card strong {
  color: #172033;
  font-size: 26px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.audit-filters {
  display: grid;
  grid-template-columns: 170px 130px minmax(280px, 360px) minmax(220px, 1fr) auto;
  gap: 10px;
  padding: 12px;
}

.audit-workbench {
  min-height: 600px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 0;
  overflow: hidden;
}

.audit-list-panel,
.audit-detail-panel {
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.audit-list-panel {
  min-width: 0;
  border-right: 1px solid #d7e1ec;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.list-head {
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-bottom: 1px solid #e4ebf3;
}

.list-head strong {
  color: #172033;
}

.list-head span {
  color: #66758b;
  font-size: 12px;
}

.audit-list {
  min-height: 0;
  overflow-y: auto;
  padding: 10px;
}

.audit-row {
  width: 100%;
  min-height: 76px;
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) 86px;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  padding: 10px;
  background: #fbfdff;
  text-align: left;
  cursor: pointer;
}

.audit-row:hover,
.audit-row.active {
  border-color: #bed6ff;
  background: #e9f2ff;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #dc2626;
}

.status-dot.success {
  background: #16a34a;
}

.row-main,
.row-side,
.row-title {
  min-width: 0;
  display: grid;
}

.row-title {
  grid-template-columns: minmax(0, auto) auto;
  justify-content: start;
  align-items: center;
  gap: 8px;
}

.row-title b {
  color: #172033;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.row-title em {
  border-radius: 999px;
  padding: 2px 8px;
  color: #1557c0;
  background: #dbeafe;
  font-size: 11px;
  font-style: normal;
}

.row-preview {
  margin-top: 6px;
  color: #66758b;
  font-size: 13px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.row-side {
  justify-items: end;
  gap: 6px;
  color: #66758b;
  font-size: 12px;
}

.row-side strong {
  color: #172033;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 10px 12px;
  border-top: 1px solid #e4ebf3;
}

.audit-detail-panel {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 14px;
  background: #f7fafc;
  overflow-y: auto;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  padding: 12px;
  background: #ffffff;
}

.detail-kicker {
  color: #66758b;
  font-size: 12px;
  font-weight: 800;
}

.detail-head h3 {
  max-width: 290px;
  margin: 4px 0 0;
  color: #172033;
  font-size: 15px;
  line-height: 1.35;
  word-break: break-all;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.detail-grid > div,
.detail-block {
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: #ffffff;
}

.detail-grid > div {
  display: grid;
  gap: 5px;
  padding: 10px;
}

.detail-grid span {
  color: #66758b;
  font-size: 12px;
}

.detail-grid strong {
  color: #172033;
}

.detail-block {
  overflow: hidden;
}

.detail-block header,
.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid #e4ebf3;
}

.detail-block header strong {
  color: #172033;
}

.detail-block pre {
  max-height: 220px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  color: #334155;
  background: #fbfdff;
  font-family: "Microsoft YaHei", Arial, sans-serif;
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-footer {
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: #ffffff;
  color: #66758b;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .audit-summary,
  .audit-filters,
  .audit-workbench {
    grid-template-columns: 1fr;
  }

  .audit-list-panel {
    border-right: 0;
    border-bottom: 1px solid #d7e1ec;
  }
}
</style>
