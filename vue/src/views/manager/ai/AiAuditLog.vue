<template>
  <div class="page-wrap">
    <section class="page-hero gradient-ai">
      <div class="hero-left">
        <el-icon class="hero-icon"><DataAnalysis /></el-icon>
        <div class="hero-text">
          <h2>AI 审计日志</h2>
          <p>追踪多角色对话与执行记录</p>
        </div>
      </div>
    </section>

    <el-card class="card-block">
      <template #header>
        <div class="card-header">
          <div class="filters">
            <el-select v-model="filters.roleId" placeholder="角色" clearable style="width: 160px">
              <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
            </el-select>
            <el-select v-model="filters.success" placeholder="结果" clearable style="width: 120px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
            <el-date-picker
              v-model="filters.range"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 320px"
            />
            <el-input v-model="filters.keyword" placeholder="搜索请求/响应" clearable style="max-width: 200px" />
            <el-button text :loading="loading" @click="fetchList">刷新</el-button>
          </div>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" class="audit-table">
        <el-table-column prop="createdTime" label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column prop="roleId" label="角色" width="160">
          <template #default="{ row }">{{ roleLabel(row.roleId) }}</template>
        </el-table-column>
        <el-table-column prop="actionType" label="类型" width="150" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column label="请求" min-width="220">
          <template #default="{ row }">{{ row.requestText || '-' }}</template>
        </el-table-column>
        <el-table-column label="响应" min-width="220">
          <template #default="{ row }">{{ row.responseText || '-' }}</template>
        </el-table-column>
        <el-table-column prop="latencyMs" label="耗时(ms)" width="120" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

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
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import { aiApi } from '@/api'
import { ElMessage } from 'element-plus'

const list = ref([])
const roles = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
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

const formatTime = (value) => {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
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
    } else {
      ElMessage.error(res.msg || '获取日志失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取日志失败')
  } finally {
    loading.value = false
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
.page-wrap { display: flex; flex-direction: column; gap: 12px; padding: 16px; }
.page-hero { color: #fff; padding: 16px 20px; border-radius: 12px; display:flex; align-items:center; justify-content:space-between; }
.gradient-ai { background: linear-gradient(90deg, #5b7cff 0%, #6bc4ff 100%); }
.hero-left { display:flex; align-items:center; gap: 10px; }
.hero-icon{ font-size: 26px; }
.hero-text h2{ margin:0; font-size:18px; }
.hero-text p{ margin:0; opacity:.9; font-size:13px; }
.card-block { border-radius: 12px; }
.card-header { display:flex; align-items:center; justify-content:space-between; gap: 10px; }
.filters { display:flex; align-items:center; gap: 8px; flex-wrap: wrap; }
.audit-table :deep(.el-table__header th) { background: #f6f9ff; color: #3a4b6a; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; gap: 8px; }
  .filters { width: 100%; }
}
</style>
