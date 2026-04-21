<template>
  <div class="category-page">
    <!-- 渐变页头 -->
    <div class="page-header category-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><PriceTag /></el-icon>
          分类管理
        </h1>
        <p class="page-subtitle">维护你的消费分类，合理组织支出</p>
      </div>
      <div class="header-right">
        <el-button plain @click="initDefaults" :loading="loading.init" class="init-btn">
          初始化默认
        </el-button>
        <el-button type="primary" @click="openCreate" class="create-btn">
          <el-icon><Plus /></el-icon>
          新建分类
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="list-card">
      <div class="summary-row">
        <div class="summary-item">
          <span class="summary-label">分类总数</span>
          <span class="summary-value">{{ list.length }}</span>
        </div>
      </div>
      <el-empty v-if="!loading.list && (!list || list.length === 0)" description="暂无分类，您可以点击上方按钮初始化默认分类或新建分类">
        <el-button type="primary" @click="openCreate">新建分类</el-button>
        <el-button class="ml8" @click="initDefaults">初始化默认</el-button>
      </el-empty>

      <el-table v-else :data="list" border stripe v-loading="loading.list" class="category-table">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="createdTime" label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)"><el-icon><Edit /></el-icon>编辑</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)" :loading="loading.deleteId === row.id"><el-icon><Delete /></el-icon>删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑分类' : '新增分类'" width="520px" destroy-on-close class="category-dialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="86px" class="category-form">
        <el-form-item label="名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入分类名称" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model.trim="form.description" placeholder="可填写分类说明" maxlength="120" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" controls-position="right" />
          <div class="tip">数值越小越靠前，默认 0</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="loading.save" @click="onSubmit">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { expenseCategoryApi } from '@/api/index'
// 修正导入：合并为一条并移除重复的 Plus
import { Edit, Delete, Plus, PriceTag } from '@element-plus/icons-vue'

const list = ref([])
const loading = reactive({ list: false, save: false, init: false, deleteId: null })

const dialog = reactive({ visible: false, isEdit: false })
const formRef = ref(null)
const form = reactive({ id: null, name: '', description: '', sortOrder: 0 })

const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }, { min: 1, max: 30, message: '长度 1-30 个字符', trigger: 'blur' }] }

const resetForm = () => { form.id = null; form.name = ''; form.description = ''; form.sortOrder = 0 }

const fetchList = async () => {
  loading.list = true
  try {
    const res = await expenseCategoryApi.getList()
    if (res && (res.code === 200 || res.code === '200')) list.value = res.data || []
    else ElMessage.error(res?.msg || '获取分类失败')
  } catch (e) { ElMessage.error(e?.message || '获取分类失败') } finally { loading.list = false }
}

const openCreate = () => { resetForm(); dialog.isEdit = false; dialog.visible = true }
const openEdit = (row) => { form.id = row.id; form.name = row.name; form.description = row.description; form.sortOrder = row.sortOrder ?? 0; dialog.isEdit = true; dialog.visible = true }

const onSubmit = async () => {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  loading.save = true
  try {
    const payload = { name: form.name, description: form.description, sortOrder: form.sortOrder }
    let res
    if (dialog.isEdit && form.id) res = await expenseCategoryApi.update(form.id, payload)
    else res = await expenseCategoryApi.create(payload)
    if (res && (res.code === 200 || res.code === '200')) {
      ElMessage.success(dialog.isEdit ? '更新成功' : '创建成功'); dialog.visible = false; fetchList()
    } else ElMessage.error(res?.msg || '保存失败')
  } catch (e) { ElMessage.error(e?.message || '保存失败') } finally { loading.save = false }
}

const onDelete = async (row) => {
  try { await ElMessageBox.confirm(`确定删除分类“${row.name}”吗？`, '删除确认', { type: 'warning' }) } catch { return }
  loading.deleteId = row.id
  try {
    const res = await expenseCategoryApi.delete(row.id)
    if (res && (res.code === 200 || res.code === '200')) { ElMessage.success('删除成功'); fetchList() }
    else ElMessage.error(res?.msg || '删除失败')
  } catch (e) { ElMessage.error(e?.message || '删除失败') } finally { loading.deleteId = null }
}

const initDefaults = async () => {
  try { await ElMessageBox.confirm('此操作会在当前用户下创建一组常用分类（若重名将跳过），是否继续？', '初始化默认分类', { type: 'warning' }) } catch { return }
  loading.init = true
  try {
    const res = await expenseCategoryApi.initDefault()
    if (res && (res.code === 200 || res.code === '200')) { ElMessage.success('已初始化默认分类'); fetchList() }
    else ElMessage.error(res?.msg || '初始化失败')
  } catch (e) { ElMessage.error(e?.message || '初始化失败') } finally { loading.init = false }
}

const formatTime = (ts) => { if (!ts) return '-'; try { return new Date(ts).toLocaleString() } catch { return '-' } }

onMounted(fetchList)
</script>

<style scoped>
.category-page {
  padding: 16px;
  background: linear-gradient(180deg, #f8f9ff 0%, #eff4ff 100%);
}
.list-card {
  margin-top: 14px;
  border: 1px solid #e4e9fb;
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}
.tip { margin-left: 12px; color: #909399; font-size: 12px; }
.ml8 { margin-left: 8px; }
.page-header {
  display:flex;
  justify-content:space-between;
  align-items:flex-start;
  padding:24px 24px 16px;
  color:#fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 12px 24px rgba(79, 70, 229, 0.26);
}
.header-left { flex:1; }
.page-title { display:flex; align-items:center; gap:10px; font-size:22px; font-weight:600; margin:0 0 6px 0; }
.title-icon { font-size:28px; }
.page-subtitle { margin:0; opacity:0.9; }
.header-right { display:flex; align-items:center; gap: 10px; }
.init-btn {
  border-color: rgba(255, 255, 255, 0.65);
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}
.init-btn:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.2);
}
.create-btn { box-shadow: 0 4px 12px rgba(59,130,246,0.25); }
.category-header { background: linear-gradient(135deg, #8b5cf6 0%, #3b82f6 100%); }

.summary-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  border: 1px solid #dfe7ff;
  background: #f7f9ff;
}
.summary-label {
  font-size: 13px;
  color: #5b6479;
}
.summary-value {
  font-size: 18px;
  font-weight: 700;
  color: #2d4cc8;
}
.category-table :deep(.el-table__header th) {
  background: #f8faff;
  color: #3d4b66;
}
.category-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}
.category-form :deep(.el-input__wrapper),
.category-form :deep(.el-textarea__inner),
.category-form :deep(.el-input-number) {
  border-radius: 8px;
}

@media (max-width: 768px) {
  .category-page {
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
}
</style>
