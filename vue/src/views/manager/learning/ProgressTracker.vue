<template>
  <div class="page-wrap">
    <section class="page-hero gradient-progress">
      <div class="hero-left">
        <el-icon class="hero-icon"><TrendCharts /></el-icon>
        <div class="hero-text">
          <h2>学习进度</h2>
          <p>记录与查看课程学习进展</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate" class="hero-primary-btn"><el-icon><Plus /></el-icon> 新建进度</el-button>
      </div>
    </section>

    <el-card class="card-block">
      <template #header>
        <div class="card-header">
          <span class="header-title">进度列表</span>
          <div class="header-actions">
            <el-input v-model="keyword" placeholder="搜索课程或备注" clearable class="search-input"/>
            <el-button :loading="loading.list" @click="fetchList" text class="refresh-btn">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filtered" style="width: 100%" v-loading="loading.list" class="progress-table">
        <el-table-column prop="course" label="课程"/>
        <el-table-column label="进度">
          <template #default="{ row }">
            <el-progress :percentage="row.percent" :stroke-width="18" class="row-progress"/>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注"/>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="openEdit(row)" class="table-edit">编辑</el-button>
            <el-button size="small" text @click="increase(row)" class="table-step">+10%</el-button>
            <el-button size="small" type="danger" text :loading="loading.deleteId===row.id" @click="remove(row)" class="table-delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" title="学习进度" class="progress-dialog">
      <el-form :model="form" label-width="84px" class="progress-form">
        <el-form-item label="课程"><el-input v-model="form.course" class="field"/></el-form-item>
        <el-form-item label="进度">
          <el-slider v-model="form.percent" :step="10" :min="0" :max="100" show-stops class="field"/>
          <div class="percent-text">当前：{{ form.percent }}%</div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.note" class="field"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible=false">取消</el-button>
        <el-button type="primary" :loading="loading.save" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { learningProgressApi } from '@/api'

const keyword = ref('')
const list = ref([])
const dialog = ref({ visible: false, isEdit: false })
const form = ref({ id: null, course: '', percent: 0, note: '' })
const loading = ref({ list: false, save: false, deleteId: null })

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return list.value.filter(i => !k || i.course?.toLowerCase().includes(k) || i.note?.toLowerCase().includes(k))
})

async function fetchList(){
  loading.value.list = true
  try{
    const res = await learningProgressApi.getList()
    if(res && (res.code===200 || res.code==='200')) list.value = res.data || []
    else ElMessage.error(res?.msg || '获取进度失败')
  }catch(e){ ElMessage.error(e?.message || '获取进度失败') }
  finally{ loading.value.list = false }
}

function openCreate(){ dialog.value = { visible: true, isEdit: false }; form.value = { id:null, course:'', percent:0, note:'' } }
function openEdit(row){ dialog.value = { visible: true, isEdit: true }; form.value = { id: row.id, course: row.course, percent: row.percent, note: row.note } }

async function save(){
  if(!form.value.course?.trim()){ ElMessage.warning('请输入课程名称'); return }
  loading.value.save = true
  try{
    const payload = { course: form.value.course, percent: form.value.percent, note: form.value.note }
    let res
    if(dialog.value.isEdit){
      res = await learningProgressApi.update(form.value.id, payload)
    }else{
      res = await learningProgressApi.create(payload)
    }
    if(res && (res.code===200 || res.code==='200')){
      ElMessage.success(dialog.value.isEdit ? '更新成功' : '创建成功')
      dialog.value.visible = false
      fetchList()
    }else{
      ElMessage.error(res?.msg || '保存失败')
    }
  }catch(e){ ElMessage.error(e?.message || '保存失败') }
  finally{ loading.value.save = false }
}

async function remove(row){
  try{ await ElMessageBox.confirm(`确定删除课程“${row.course}”的学习进度吗？`, '删除确认', { type: 'warning' }) }catch{ return }
  loading.value.deleteId = row.id
  try{
    const res = await learningProgressApi.delete(row.id)
    if(res && (res.code===200 || res.code==='200')){ ElMessage.success('删除成功'); fetchList() }
    else ElMessage.error(res?.msg || '删除失败')
  }catch(e){ ElMessage.error(e?.message || '删除失败') }
  finally{ loading.value.deleteId = null }
}

async function increase(row){
  const newVal = Math.min(100, (row.percent || 0) + 10)
  try{
    const res = await learningProgressApi.update(row.id, { course: row.course, percent: newVal, note: row.note })
    if(res && (res.code===200 || res.code==='200')){ row.percent = newVal; ElMessage.success('+10% 已保存') }
    else ElMessage.error(res?.msg || '更新失败')
  }catch(e){ ElMessage.error(e?.message || '更新失败') }
}

onMounted(fetchList)
</script>

<style scoped>
.page-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: linear-gradient(180deg, #f7f8ff 0%, #eef2ff 100%);
}
.page-hero {
  color: #fff;
  padding: 18px 22px;
  border-radius: 14px;
  display:flex;
  align-items:center;
  justify-content:space-between;
  box-shadow: 0 12px 24px rgba(63, 81, 181, 0.22);
}
.gradient-progress { background: linear-gradient(90deg, #8E24AA 0%, #3F51B5 100%); }
.hero-left { display:flex; align-items:center; gap: 10px; }
.hero-icon{ font-size: 28px; }
.hero-text h2{ margin:0; font-size:20px; }
.hero-text p{ margin:2px 0 0; opacity:.92; font-size:13px; }
.hero-primary-btn {
  box-shadow: 0 6px 14px rgba(255, 255, 255, 0.25);
}
.card-block {
  border-radius: 14px;
  border: 1px solid #e3e8ff;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}
.card-block :deep(.el-card__header) {
  border-bottom: 1px solid #ecefff;
  background: #fbfcff;
}
.card-header {
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:10px;
}
.header-title {
  font-weight: 600;
  color: #313948;
}
.header-actions{
  display:flex;
  align-items:center;
  gap:8px;
}
.search-input {
  width: 240px;
}
.refresh-btn {
  color: #4f46e5;
}
.progress-table :deep(.el-table__header th) {
  background: #f7f8ff;
  color: #3f4a74;
}
.row-progress :deep(.el-progress-bar__outer) {
  background: #eceffd;
}
.table-edit {
  color: #4f46e5;
}
.table-step {
  color: #5b7cff;
}
.table-delete {
  color: #d93025;
}
.progress-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}
.progress-form :deep(.el-form-item__label) {
  color: #465061;
  font-weight: 500;
}
.field :deep(.el-input__wrapper) {
  border-radius: 10px;
}
.percent-text {
  margin-top: 6px;
  color: #5f6b8c;
  font-size: 12px;
}

@media (max-width: 768px) {
  .page-wrap {
    padding: 12px;
  }
  .page-hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .hero-actions {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-actions {
    width: 100%;
  }
  .search-input {
    width: 100%;
  }
}
</style>
