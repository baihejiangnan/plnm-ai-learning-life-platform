<template>
  <div class="page-wrap">
    <section class="page-hero gradient-resource">
      <div class="hero-left">
        <el-icon class="hero-icon"><Reading /></el-icon>
        <div class="hero-text">
          <h2>学习资源库</h2>
          <p>集中管理课程相关的文章、视频、书籍</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate" class="hero-primary-btn"><el-icon><Plus /></el-icon> 新建资源</el-button>
      </div>
    </section>

    <el-card class="card-block">
      <template #header>
        <div class="card-header">
          <div class="filters">
            <el-select v-model="filterType" placeholder="类型" clearable class="type-filter">
              <el-option label="文章" value="article"/>
              <el-option label="视频" value="video"/>
              <el-option label="书籍" value="book"/>
            </el-select>
            <el-select v-model="filterCategory" placeholder="分类" clearable class="category-filter" :loading="loading.categories">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"/>
            </el-select>
            <el-input v-model="keyword" placeholder="搜索标题或来源" clearable class="search-input"/>
            <el-button text :loading="loading.list" @click="fetchList" class="refresh-btn">刷新</el-button>
          </div>
          <el-button @click="resetFilters" text class="reset-btn">重置</el-button>
        </div>
      </template>

      <el-table :data="filtered" style="width: 100%" v-loading="loading.list" class="resource-table">
        <el-table-column prop="title" label="标题"/>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row.type)" effect="light">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源"/>
        <el-table-column prop="link" label="链接">
          <template #default="{ row }">
            <a :href="row.link" target="_blank" rel="noopener" v-if="row.link">打开</a>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="edit(row)" class="table-edit">编辑</el-button>
            <el-button size="small" type="danger" text :loading="loading.deleteId===row.id" @click="remove(row)" class="table-delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑资源' : '新建资源'" class="resource-dialog">
      <el-form :model="form" label-width="84px" class="resource-form">
        <el-form-item label="标题"><el-input v-model="form.title" class="field"/></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择" class="field">
            <el-option label="文章" value="article"/>
            <el-option label="视频" value="video"/>
            <el-option label="书籍" value="book"/>
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择" clearable :loading="loading.categories" class="field">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="来源"><el-input v-model="form.source" class="field"/></el-form-item>
        <el-form-item label="链接"><el-input v-model="form.link" class="field"/></el-form-item>
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
import { Plus, Reading } from '@element-plus/icons-vue'
import { learningResourceApi, learningCourseCategoryApi } from '@/api'

const list = ref([])
const keyword = ref('')
const filterType = ref('')
const filterCategory = ref(null)

const categories = ref([])

const dialog = ref({ visible: false, isEdit: false })
const form = ref({ id: null, title: '', type: '', source: '', link: '', categoryId: null })

const loading = ref({ list: false, save: false, deleteId: null, categories: false })

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return list.value.filter(i =>
    (!filterType.value || i.type === filterType.value) &&
    (!filterCategory.value || i.categoryId === filterCategory.value) &&
    (!k || i.title?.toLowerCase().includes(k) || i.source?.toLowerCase().includes(k))
  )
})

function tagType(t){ return t==='video'?'success': t==='book'?'warning': 'info' }
function typeLabel(t){ return t==='video'?'视频': t==='book'?'书籍': '文章' }

async function fetchCategories(){
  loading.value.categories = true
  try{
    const res = await learningCourseCategoryApi.getList()
    if(res && (res.code===200 || res.code==='200')) categories.value = res.data || []
    else ElMessage.error(res?.msg || '获取分类失败')
  }catch(e){ ElMessage.error(e?.message || '获取分类失败') }
  finally{ loading.value.categories = false }
}

async function fetchList(){
  loading.value.list = true
  try{
    const params = {}
    if(filterType.value) params.type = filterType.value
    if(filterCategory.value) params.categoryId = filterCategory.value
    const res = await learningResourceApi.getList(params)
    if(res && (res.code===200 || res.code==='200')) list.value = res.data || []
    else ElMessage.error(res?.msg || '获取资源失败')
  }catch(e){ ElMessage.error(e?.message || '获取资源失败') }
  finally{ loading.value.list = false }
}

function openCreate(){ dialog.value = { visible:true, isEdit:false }; form.value = { id:null, title:'', type:'', source:'', link:'', categoryId: null } }
function edit(row){ dialog.value = { visible:true, isEdit:true }; form.value = { id: row.id, title: row.title, type: row.type, source: row.source ?? '', link: row.link ?? '', categoryId: row.categoryId ?? null } }

async function remove(row){
  try{ await ElMessageBox.confirm(`确定删除资源“${row.title}”吗？`, '删除确认', { type: 'warning' }) }catch{ return }
  loading.value.deleteId = row.id
  try{
    const res = await learningResourceApi.delete(row.id)
    if(res && (res.code===200 || res.code==='200')){ ElMessage.success('删除成功'); fetchList() }
    else ElMessage.error(res?.msg || '删除失败')
  }catch(e){ ElMessage.error(e?.message || '删除失败') }
  finally{ loading.value.deleteId = null }
}

async function save(){
  if(!form.value.title?.trim()) { ElMessage.warning('请输入标题'); return }
  if(!form.value.type) { ElMessage.warning('请选择类型'); return }
  loading.value.save = true
  try{
    const payload = { title: form.value.title.trim(), type: form.value.type, source: form.value.source?.trim() || null, link: form.value.link?.trim() || null, categoryId: form.value.categoryId || null }
    let res
    if(dialog.value.isEdit && form.value.id){ res = await learningResourceApi.update(form.value.id, payload) }
    else { res = await learningResourceApi.create(payload) }
    if(res && (res.code===200 || res.code==='200')){ ElMessage.success(dialog.value.isEdit ? '更新成功' : '创建成功'); dialog.value.visible=false; fetchList() }
    else ElMessage.error(res?.msg || '保存失败')
  }catch(e){ ElMessage.error(e?.message || '保存失败') }
  finally{ loading.value.save = false }
}

function resetFilters(){ filterType.value=''; filterCategory.value=null; keyword.value='' }

onMounted(() => { fetchCategories(); fetchList() })
</script>

<style scoped>
.page-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: linear-gradient(180deg, #f2fffb 0%, #ecfff5 100%);
}
.page-hero {
  color: #fff;
  padding: 18px 22px;
  border-radius: 14px;
  display:flex;
  align-items:center;
  justify-content:space-between;
  box-shadow: 0 12px 24px rgba(76, 175, 80, 0.2);
}
.gradient-resource { background: linear-gradient(90deg, #00BCD4 0%, #4CAF50 100%); }
.hero-left { display:flex; align-items:center; gap: 10px; }
.hero-icon{ font-size: 28px; }
.hero-text h2{ margin:0; font-size:20px; }
.hero-text p{ margin:2px 0 0; opacity:.92; font-size:13px; }
.hero-primary-btn {
  box-shadow: 0 6px 14px rgba(255, 255, 255, 0.25);
}
.card-block {
  border-radius: 14px;
  border: 1px solid #d8f5e8;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}
.card-block :deep(.el-card__header) {
  border-bottom: 1px solid #e8f8f0;
  background: #fbfffd;
}
.card-header {
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap: 10px;
}
.filters {
  display:flex;
  align-items:center;
  gap: 8px;
}
.type-filter {
  width: 120px;
}
.category-filter {
  width: 160px;
}
.search-input {
  width: 240px;
}
.refresh-btn {
  color: #0e9f6e;
}
.reset-btn {
  color: #0f766e;
}
.resource-table :deep(.el-table__header th) {
  background: #f4fff8;
  color: #285f55;
}
.resource-table :deep(a) {
  color: #0ea5a3;
  text-decoration: none;
}
.resource-table :deep(a:hover) {
  text-decoration: underline;
}
.table-edit {
  color: #0f766e;
}
.table-delete {
  color: #d93025;
}
.resource-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}
.resource-form :deep(.el-form-item__label) {
  color: #465061;
  font-weight: 500;
}
.field :deep(.el-input__wrapper),
.field :deep(.el-select__wrapper) {
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
    display: flex;
    justify-content: flex-end;
  }
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .filters {
    width: 100%;
    flex-wrap: wrap;
  }
  .type-filter,
  .category-filter,
  .search-input {
    width: 100%;
  }
}
</style>
