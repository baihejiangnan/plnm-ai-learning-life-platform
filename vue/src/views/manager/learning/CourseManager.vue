<template>
  <div class="page-wrap">
    <section class="page-hero gradient-course">
      <div class="hero-left">
        <el-icon class="hero-icon"><Collection /></el-icon>
        <div class="hero-text">
          <h2>课程分类</h2>
          <p>管理课程分类，支持增删改查</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate" class="hero-primary-btn"><el-icon><Plus /></el-icon> 新建分类</el-button>
        <el-button text :loading="loading.init" @click="initDefault" class="hero-text-btn">初始化默认分类</el-button>
      </div>
    </section>

    <el-card class="card-block">
      <template #header>
        <div class="card-header">
          <span class="header-title">分类列表</span>
          <div class="header-right">
            <el-input v-model="keyword" placeholder="搜索分类" clearable class="search-input"/>
            <el-button text :loading="loading.list" @click="fetchList" class="refresh-btn">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filtered" style="width: 100%" v-loading="loading.list" class="course-table">
        <el-table-column prop="name" label="名称"/>
        <el-table-column prop="description" label="说明"/>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="edit(row)" class="table-edit">编辑</el-button>
            <el-button size="small" type="danger" text :loading="loading.deleteId===row.id" @click="remove(row)" class="table-delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑分类' : '新建分类'" class="course-dialog">
      <el-form :model="form" label-width="72px" class="course-form">
        <el-form-item label="名称"><el-input v-model="form.name" class="field"/></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" class="field"/></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Collection } from '@element-plus/icons-vue'
import { learningCourseCategoryApi } from '@/api'

const list = ref([])
const keyword = ref('')

const dialog = ref({ visible: false, isEdit: false })
const form = ref({ id: null, name: '', description: '', sortOrder: 0 })

const loading = ref({ list: false, save: false, deleteId: null, init: false })

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return list.value.filter(i => !k || i.name.toLowerCase().includes(k) || i.description?.toLowerCase().includes(k))
})

async function fetchList(){
  loading.value.list = true
  try{
    const res = await learningCourseCategoryApi.getList()
    if(res && (res.code===200 || res.code==='200')) list.value = res.data || []
    else ElMessage.error(res?.msg || '获取分类失败')
  }catch(e){ ElMessage.error(e?.message || '获取分类失败') }
  finally{ loading.value.list = false }
}

function openCreate(){ dialog.value = { visible:true, isEdit:false }; form.value = { id:null, name:'', description:'', sortOrder:0 } }
function edit(row){ dialog.value = { visible:true, isEdit:true }; form.value = { id: row.id, name: row.name, description: row.description ?? '', sortOrder: row.sortOrder ?? 0 } }

async function remove(row){
  try{ await ElMessageBox.confirm(`确定删除分类“${row.name}”吗？`, '删除确认', { type: 'warning' }) }catch{ return }
  loading.value.deleteId = row.id
  try{
    const res = await learningCourseCategoryApi.delete(row.id)
    if(res && (res.code===200 || res.code==='200')){ ElMessage.success('删除成功'); fetchList() }
    else ElMessage.error(res?.msg || '删除失败')
  }catch(e){ ElMessage.error(e?.message || '删除失败') }
  finally{ loading.value.deleteId = null }
}

async function save(){
  if(!form.value.name?.trim()) { ElMessage.warning('请输入分类名称'); return }
  loading.value.save = true
  try{
    const payload = { name: form.value.name.trim(), description: form.value.description?.trim() || null, sortOrder: form.value.sortOrder ?? 0 }
    let res
    if(dialog.value.isEdit && form.value.id){
      res = await learningCourseCategoryApi.update(form.value.id, payload)
    } else {
      res = await learningCourseCategoryApi.create(payload)
    }
    if(res && (res.code===200 || res.code==='200')){ ElMessage.success(dialog.value.isEdit ? '更新成功' : '创建成功'); dialog.value.visible=false; fetchList() }
    else ElMessage.error(res?.msg || '保存失败')
  }catch(e){ ElMessage.error(e?.message || '保存失败') }
  finally{ loading.value.save = false }
}

async function initDefault(){
  loading.value.init = true
  try{
    const res = await learningCourseCategoryApi.initDefault()
    if(res && (res.code===200 || res.code==='200')){ ElMessage.success('已初始化默认分类'); fetchList() }
    else ElMessage.error(res?.msg || '初始化失败')
  }catch(e){ ElMessage.error(e?.message || '初始化失败') }
  finally{ loading.value.init = false }
}

onMounted(fetchList)
</script>

<style scoped>
.page-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: linear-gradient(180deg, #fffaf4 0%, #fff3ee 100%);
}
.page-hero {
  color: #fff;
  padding: 18px 22px;
  border-radius: 14px;
  display:flex;
  align-items:center;
  justify-content:space-between;
  box-shadow: 0 12px 24px rgba(244, 67, 54, 0.22);
}
.gradient-course { background: linear-gradient(90deg, #FF9800 0%, #F44336 100%); }
.hero-left { display:flex; align-items:center; gap: 10px; }
.hero-icon{ font-size: 28px; }
.hero-text h2{ margin:0; font-size:20px; }
.hero-text p{ margin:2px 0 0; opacity:.92; font-size:13px; }
.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.hero-primary-btn {
  box-shadow: 0 6px 14px rgba(255, 255, 255, 0.25);
}
.hero-text-btn {
  color: rgba(255, 255, 255, 0.92);
}
.hero-text-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}
.card-block {
  border-radius: 14px;
  border: 1px solid #ffe2d9;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}
.card-block :deep(.el-card__header) {
  border-bottom: 1px solid #ffe8e1;
  background: #fffdfc;
}
.card-header {
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap: 10px;
}
.header-title {
  font-weight: 600;
  color: #313948;
}
.header-right{
  display:flex;
  align-items:center;
  gap:8px;
}
.search-input {
  width: 240px;
}
.refresh-btn {
  color: #e65145;
}
.course-table :deep(.el-table__header th) {
  background: #fff8f6;
  color: #5e3c38;
}
.table-edit {
  color: #ea580c;
}
.table-delete {
  color: #d93025;
}
.course-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}
.course-form :deep(.el-form-item__label) {
  color: #465061;
  font-weight: 500;
}
.field :deep(.el-input__wrapper) {
  border-radius: 10px;
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
    justify-content: flex-end;
  }
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-right {
    width: 100%;
  }
  .search-input {
    width: 100%;
  }
}
</style>
