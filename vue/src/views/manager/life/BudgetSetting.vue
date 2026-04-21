<template>
  <div class="budget-setting">
    <!-- 渐变页头 -->
    <div class="page-header budget-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><PriceTag /></el-icon>
          预算设置
        </h1>
        <p class="page-subtitle">设置每月总预算与分类预算</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="save" class="create-btn">保存设置</el-button>
      </div>
    </div>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>预算设置</span>
          <span class="card-desc">按月份维护预算，便于后续统计预警</span>
        </div>
      </template>

      <el-form :model="form" label-width="120px" class="budget-form">
        <div class="form-grid">
          <el-form-item label="月份">
            <el-date-picker v-model="form.month" type="month" placeholder="选择月份" value-format="YYYY-MM" @change="onMonthChange" class="field-control" />
          </el-form-item>
          <el-form-item label="全局月预算(￥)">
            <el-input-number v-model="form.globalBudget" :min="0" :precision="2" :step="100" placeholder="0.00" controls-position="right" class="field-control" />
          </el-form-item>
          <el-form-item label="超支提醒阈值(%)" class="full-row">
            <el-input-number v-model="form.threshold" :min="50" :max="200" :step="5" class="field-control threshold-input" />
            <span class="hint">当支出达到该百分比时提醒，默认100%</span>
          </el-form-item>
        </div>
      </el-form>

      <el-divider />

      <div class="budget-sub-header">
        <span>按分类预算</span>
        <span class="budget-count">已配置 {{ categoryBudgets.length }} 项</span>
        <el-button type="primary" @click="addCategoryBudget">新增分类预算</el-button>
      </div>
      <el-table :data="categoryBudgets" class="budget-table">
        <el-table-column label="分类" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.categoryId" placeholder="选择分类" filterable class="category-select" @change="() => syncCategoryName(row)">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" :disabled="selectedCategoryIds.has(c.id) && c.id !== row.categoryId" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="预算(￥)" min-width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.budget" :min="0" :precision="2" :step="50" controls-position="right" class="budget-input" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ $index }">
            <el-button size="small" class="delete-btn" @click="removeCategoryBudget($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="footer-actions">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { PriceTag } from '@element-plus/icons-vue'
import { expenseCategoryApi, expenseApi } from '@/api'

const nowMonth = () => new Date().toISOString().slice(0, 7)
const loadThreshold = () => {
  const v = Number(localStorage.getItem('budgetThreshold') || 100)
  return Number.isFinite(v) && v > 0 ? v : 100
}

const form = reactive({ month: '', globalBudget: 0, threshold: loadThreshold() })
const categoryBudgets = ref([]) // [{categoryId, categoryName, budget}]
const categories = ref([])

const selectedCategoryIds = computed(() => new Set(categoryBudgets.value.filter(b => b.categoryId != null).map(b => b.categoryId)))

const syncCategoryName = (row) => {
  const c = categories.value.find(x => x.id === row.categoryId)
  row.categoryName = c ? c.name : ''
}

const addCategoryBudget = () => {
  categoryBudgets.value.push({ categoryId: null, categoryName: '', budget: 0 })
}

const removeCategoryBudget = (idx) => {
  categoryBudgets.value.splice(idx, 1)
}

const reset = async () => {
  form.month = nowMonth()
  form.globalBudget = 0
  // 阈值保留用户设定
  categoryBudgets.value = []
  await loadBudgets()
}

const loadCategories = async () => {
  try {
    const res = await expenseCategoryApi.getList()
    categories.value = res?.data || []
  } catch (e) {
    console.error(e)
  }
}

const loadBudgets = async () => {
  try {
    const res = await expenseApi.getBudgets(form.month || nowMonth())
    const list = res?.data || []
    // 全局预算：categoryId == null
    const global = list.find(x => x.categoryId == null)
    form.globalBudget = global ? Number(global.amount || 0) : 0
    // 分类预算
    const items = list.filter(x => x.categoryId != null).map(x => ({
      categoryId: x.categoryId,
      categoryName: (categories.value.find(c => c.id === x.categoryId)?.name) || '',
      budget: Number(x.amount || 0)
    }))
    categoryBudgets.value = items
  } catch (e) {
    console.error(e)
  }
}

const onMonthChange = async () => {
  await loadBudgets()
}

const save = async () => {
  try {
    // 持久化阈值到本地
    localStorage.setItem('budgetThreshold', String(form.threshold))
    const month = form.month || nowMonth()
    // 保存全局预算
    await expenseApi.setBudget({ month, budget: Number(form.globalBudget || 0).toFixed(2) })
    // 保存分类预算（去重，仅保留最后一条）
    const map = new Map()
    for (const b of categoryBudgets.value) {
      if (b.categoryId == null) continue
      map.set(b.categoryId, Number(b.budget || 0))
    }
    for (const [categoryId, budget] of map.entries()) {
      await expenseApi.setBudget({ month, categoryId, budget: Number(budget).toFixed(2) })
    }
    ElMessage.success('预算已保存')
    await loadBudgets()
  } catch (e) {
    console.error(e)
    ElMessage.error('保存预算失败')
  }
}

onMounted(async () => {
  form.month = nowMonth()
  await loadCategories()
  await loadBudgets()
})
</script>

<style scoped>
.budget-setting {
  padding: 16px;
  background: linear-gradient(180deg, #fff9f3 0%, #fff4f7 100%);
}
.page-header {
  display:flex;
  justify-content:space-between;
  align-items:flex-start;
  padding:24px 24px 16px;
  color:#fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 12px 24px rgba(239, 68, 68, 0.2);
}
.header-left { flex:1; }
.page-title { display:flex; align-items:center; gap:10px; font-size:22px; font-weight:600; margin:0 0 6px 0; }
.title-icon { font-size:28px; }
.page-subtitle { margin:0; opacity:0.9; }
.header-right { display:flex; align-items:center; }
.create-btn { box-shadow: 0 4px 12px rgba(239,68,68,0.25); }
/* 不同页面的渐变色 */
.budget-header { background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%); }

.box-card {
  margin-top: 16px;
  margin-bottom: 16px;
  border: 1px solid #fee2e2;
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}
.box-card :deep(.el-card__header) {
  border-bottom: 1px solid #ffebeb;
  background: #fffdfd;
}
.card-header {
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap: 12px;
}
.card-desc {
  color: #8c4f4f;
  font-size: 12px;
}
.budget-form {
  max-width: 100%;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
.full-row {
  grid-column: 1 / -1;
}
.field-control {
  width: 100%;
}
.threshold-input {
  max-width: 180px;
}
.budget-sub-header {
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap: 10px;
}
.budget-count {
  margin-left: auto;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #f9caca;
  background: #fff5f5;
  color: #a04d4d;
  font-size: 12px;
}
.budget-table {
  width: 100%;
  margin-top: 10px;
}
.budget-table :deep(.el-table__header th) {
  background: #fff8f8;
  color: #694646;
}
.category-select {
  width: 220px;
}
.budget-input {
  width: 170px;
}
.delete-btn {
  border-color: #ffd8d8;
  color: #d93025;
  background: #fff4f4;
}
.footer-actions {
  text-align:right;
  margin-top: 14px;
}
.hint { margin-left: 8px; color: #909399; font-size: 12px; }

@media (max-width: 992px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .budget-setting {
    padding: 12px;
  }
  .page-header {
    flex-direction: column;
    gap: 12px;
    padding: 18px 16px;
  }
  .header-right {
    width: 100%;
    justify-content: flex-end;
  }
  .budget-sub-header {
    flex-wrap: wrap;
  }
  .budget-count {
    margin-left: 0;
  }
}
</style>
