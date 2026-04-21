<template>
  <div class="note-list-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><Document /></el-icon>
          我的笔记
        </h1>
        <p class="page-subtitle">管理和查看您的所有笔记</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="createNote" class="create-btn">
          <el-icon><Plus /></el-icon>
          新建笔记
        </el-button>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-filter-section">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索笔记标题、内容..."
          prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
          class="search-input"
        />
        <el-button type="primary" @click="handleSearch" class="search-btn">
          搜索
        </el-button>
        <el-divider direction="vertical" />
        <el-button text @click="toggleManageMode">
          <el-icon><Setting /></el-icon>
          {{ manageMode ? '退出管理' : '管理' }}
        </el-button>
      </div>
      
      <div class="filter-bar">
        <!-- 标签筛选 -->
        <el-select
          v-model="selectedTagId"
          placeholder="选择标签"
          clearable
          @change="handleTagFilter"
          class="tag-filter"
        >
          <el-option
            v-for="tag in tags"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          >
            <span class="tag-option">
              <span class="tag-color" :style="{ backgroundColor: tag.color }"></span>
              {{ tag.name }}
            </span>
          </el-option>
        </el-select>
        
        <!-- 排序方式 -->
        <el-select v-model="sortBy" @change="handleSort" class="sort-select">
          <el-option label="最新创建" value="createTime" />
          <el-option label="最近更新" value="updateTime" />
          <el-option label="标题排序" value="title" />
          <el-option label="浏览量" value="viewCount" />
          <el-option label="字数" value="wordCount" />
        </el-select>
        <el-select v-model="sortOrder" @change="handleSort" class="sort-select" style="min-width: 120px;">
          <el-option label="降序" value="desc" />
          <el-option label="升序" value="asc" />
        </el-select>
        
        <!-- 视图切换 -->
        <el-radio-group v-model="viewMode" @change="handleViewModeChange" class="view-toggle">
          <el-radio-button label="list">
            <el-icon><List /></el-icon>
          </el-radio-button>
          <el-radio-button label="grid">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 管理批量操作条 -->
    <div v-if="manageMode" class="manage-bar">
      <el-checkbox v-model="selectAll" @change="toggleSelectAll">全选</el-checkbox>
      <span class="manage-count">已选 {{ selectedIds.length }} 项</span>
      <el-button type="danger" :disabled="selectedIds.length===0" @click="confirmBatchDelete" plain>
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
    </div>

    <!-- 笔记列表 -->
    <div class="notes-content" v-loading="noteLoading">
      <!-- 列表视图 -->
      <div v-if="viewMode === 'list'" class="list-view">
        <div v-if="notes.length === 0" class="empty-state">
          <el-empty description="暂无笔记">
            <el-button type="primary" @click="createNote">创建第一篇笔记</el-button>
          </el-empty>
        </div>
        
        <div v-else class="note-list">
          <div
            v-for="note in notes"
            :key="note.id"
            class="note-item"
            @click="viewNote(note.id)"
          >
            <div class="note-content">
              <div class="note-header">
                <h3 class="note-title">
                  <el-checkbox v-if="manageMode" v-model="checkedMap[note.id]" @click.stop />
                  {{ note.title || '无标题' }}
                </h3>
                <div class="note-actions">
                  <el-button
                    link
                    type="primary"
                    @click.stop="editNote(note.id)"
                    class="action-btn"
                  >
                    <el-icon><EditPen /></el-icon>
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    @click.stop="deleteNote(note)"
                    class="action-btn"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              
              <p class="note-summary">{{ note.summary || '暂无摘要' }}</p>
              
              <div class="note-tags" v-if="note.tags && note.tags.length > 0">
                <el-tag
                  v-for="tag in note.tags"
                  :key="tag.id"
                  :color="tag.color"
                  size="small"
                  class="note-tag"
                >
                  {{ tag.name }}
                </el-tag>
              </div>
              
              <div class="note-meta">
                <span class="meta-item">
                  <el-icon><Clock /></el-icon>
                  {{ formatDate(note.updatedTime || note.updateTime) }}
                </span>
                <span class="meta-item">
                  <el-icon><View /></el-icon>
                  {{ note.viewCount ?? 0 }}
                </span>
                <span class="meta-item">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ note.wordCount ?? 0 }} 字
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 网格视图 -->
      <div v-else class="grid-view">
        <div v-if="notes.length === 0" class="empty-state">
          <el-empty description="暂无笔记">
            <el-button type="primary" @click="createNote">创建第一篇笔记</el-button>
          </el-empty>
        </div>
        
        <div v-else class="note-grid">
          <div
            v-for="note in notes"
            :key="note.id"
            class="note-card"
            @click="viewNote(note.id)"
          >
            <div class="card-header">
              <h3 class="card-title">
                <el-checkbox v-if="manageMode" v-model="checkedMap[note.id]" @click.stop />
                {{ note.title || '无标题' }}
              </h3>
              <el-dropdown @command="handleNoteAction" class="card-actions">
                <el-button link class="more-btn" @click.stop>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="{ action: 'edit', note }">
                      <el-icon><EditPen /></el-icon>
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item :command="{ action: 'delete', note }" divided>
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            
            <p class="card-summary">{{ note.summary || '暂无摘要' }}</p>
            
            <div class="card-tags" v-if="note.tags && note.tags.length > 0">
              <el-tag
                v-for="tag in note.tags.slice(0, 3)"
                :key="tag.id"
                :color="tag.color"
                size="small"
                class="card-tag"
              >
                {{ tag.name }}
              </el-tag>
              <span v-if="note.tags.length > 3" class="more-tags">
                {{ note.tags.length - 3 }}
              </span>
            </div>
            
            <div class="card-footer">
              <span class="footer-date">{{ formatDate(note.updatedTime || note.updateTime) }}</span>
              <div class="footer-stats">
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ note.viewCount ?? 0 }}
                </span>
                <span class="stat-item">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ note.wordCount ?? 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="noteTotal > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="noteTotal"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNoteStore } from '@/stores/note'
import {
  Document,
  Plus,
  Search,
  List,
  Grid,
  EditPen,
  Delete,
  Clock,
  View,
  ChatDotRound,
  MoreFilled,
  Setting
} from '@element-plus/icons-vue'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

const router = useRouter()
const route = useRoute()
const noteStore = useNoteStore()

// 响应式数据
const searchKeyword = ref('')
const selectedTagId = ref(null)
const sortBy = ref('updateTime')
const sortOrder = ref('desc')
const viewMode = ref('grid')
const currentPage = ref(1)
const pageSize = ref(20)
const manageMode = ref(false)
const selectAll = ref(false)
const checkedMap = ref({})

// 计算属性
const notes = computed(() => noteStore.currentPageNotes)
const noteTotal = computed(() => noteStore.totalNotes)
const noteLoading = computed(() => noteStore.noteLoading)
const tags = computed(() => noteStore.allTags)
const selectedIds = computed(() => Object.keys(checkedMap.value).filter(id => checkedMap.value[id]).map(id => Number(id)))

// 方法
const createNote = () => {
  router.push('/notes/editor')
}

const viewNote = (id) => {
  router.push(`/notes/detail/${id}`)
}

const editNote = (id) => {
  router.push(`/notes/editor/${id}`)
}

const deleteNote = async (note) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除笔记「${note.title || '无标题'}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const success = await noteStore.deleteNote(note.id)
    if (success) {
      ElMessage.success('笔记删除成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除笔记失败:', error)
    }
  }
}

const handleSearch = () => {
  noteStore.searchNotes(searchKeyword.value)
  currentPage.value = 1
}

const handleTagFilter = () => {
  noteStore.filterByTag(selectedTagId.value)
  currentPage.value = 1
  // 同步到路由query，确保后续分页/排序/刷新等操作不会丢失当前标签筛选
  const q = { ...route.query }
  if (selectedTagId.value == null || selectedTagId.value === '') {
    // 移除tagId参数
    delete q.tagId
    delete q.tagName
  } else {
    q.tagId = Number(selectedTagId.value)
  }
  router.replace({ query: q })
}

const handleSort = () => {
  noteStore.setSort(sortBy.value, sortOrder.value)
  loadNotes()
}

const handleViewModeChange = () => {
  // 保存视图模式到本地存储
  localStorage.setItem('noteViewMode', viewMode.value)
}

const handleSizeChange = (size) => {
  pageSize.value = size
  noteStore.setPageSize(size)
  loadNotes()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  noteStore.setPage(page)
  loadNotes()
}

const handleNoteAction = ({ action, note }) => {
  if (action === 'edit') {
    editNote(note.id)
  } else if (action === 'delete') {
    deleteNote(note)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    return formatDistanceToNow(date, { addSuffix: true, locale: zhCN })
  } catch (error) {
    return dateString
  }
}

const toggleManageMode = () => {
  manageMode.value = !manageMode.value
  if (!manageMode.value) {
    checkedMap.value = {}
    selectAll.value = false
  }
}

const toggleSelectAll = () => {
  const value = selectAll.value
  const map = {}
  notes.value.forEach(n => {
    map[n.id] = value
  })
  checkedMap.value = map
}

const confirmBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的笔记')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 篇笔记吗？`, '批量删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const { success, fail } = await noteStore.deleteNotesBatch(selectedIds.value)
    if (fail === 0) {
      ElMessage.success(`已删除 ${success} 篇笔记`)
    } else {
      ElMessage.warning(`成功 ${success} 条，失败 ${fail} 条`)
    }
    checkedMap.value = {}
    selectAll.value = false
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const loadNotes = async () => {
  await noteStore.fetchNotes({
    page: currentPage.value,
    size: pageSize.value,
    keyword: searchKeyword.value,
    tagId: selectedTagId.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value
  })
}

// 监听路由查询参数
watch(
  () => route.query,
  (newQuery) => {
    let changed = false

    // 关键词
    if (Object.prototype.hasOwnProperty.call(newQuery, 'keyword')) {
      const kw = newQuery.keyword || ''
      if (searchKeyword.value !== kw) {
        searchKeyword.value = kw
        changed = true
      }
    }

    // 标签ID（来自标签管理页的跳转）
    if (Object.prototype.hasOwnProperty.call(newQuery, 'tagId')) {
      const tid = newQuery.tagId ? Number(newQuery.tagId) : null
      if (selectedTagId.value !== tid) {
        selectedTagId.value = tid
        changed = true
      }
    }

    if (changed) {
      currentPage.value = 1
      loadNotes()
    }
  },
  { immediate: true }
)

// 组件挂载时的操作
onMounted(async () => {
  // 恢复视图模式
  const savedViewMode = localStorage.getItem('noteViewMode')
  if (savedViewMode) {
    viewMode.value = savedViewMode
  }
  
  // 加载数据
  await Promise.all([
    noteStore.fetchTags(),
    loadNotes()
  ])
})
</script>

<style scoped>
.note-list-container {
  padding: 0;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 32px 32px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-left {
  flex: 1;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: white;
}

.title-icon {
  font-size: 32px;
}

.page-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.create-btn {
  background-color: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  font-weight: 500;
  padding: 12px 24px;
  border-radius: 8px;
  transition: all 0.2s;
}

.create-btn:hover {
  background-color: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateY(-1px);
}

/* 搜索筛选区域 */
.search-filter-section {
  padding: 24px 32px;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  transition: all 0.2s;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: #9ca3af;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.1);
}

.search-btn {
  border-radius: 8px;
  padding: 0 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.tag-filter,
.sort-select {
  min-width: 140px;
}

.tag-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.view-toggle {
  margin-left: auto;
  display: inline-flex;
  gap: 10px;
}

.view-toggle :deep(.el-radio-button__inner) {
  padding: 8px 12px;
  border-radius: 6px;
}

/* 管理条 */
.manage-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 32px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff7f7;
}
.manage-count {
  color: #6b7280;
}

/* 笔记内容区域 */
.notes-content {
  min-height: 400px;
  padding: 24px 32px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

/* 列表视图 */
.note-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.note-item {
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
}

.note-item:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
  transform: translateY(-2px);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.note-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.note-item:hover .note-actions {
  opacity: 1;
}

.action-btn {
  padding: 4px;
  border-radius: 4px;
}

.note-summary {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.note-tag {
  border: none;
  font-size: 12px;
}

.note-meta {
  display: flex;
  gap: 16px;
  color: #9ca3af;
  font-size: 13px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 网格视图 */
.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.note-card {
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
  height: fit-content;
}

.note-card:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  line-height: 1.4;
  flex: 1;
  margin-right: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.note-card:hover .card-actions {
  opacity: 1;
}

.more-btn {
  padding: 4px;
  color: #9ca3af;
}

.card-summary {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 63px;
}

.card-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
  min-height: 24px;
}

.card-tag {
  border: none;
  font-size: 11px;
}

.more-tags {
  color: #9ca3af;
  font-size: 12px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.footer-date {
  color: #9ca3af;
  font-size: 12px;
}

.footer-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #9ca3af;
  font-size: 12px;
}

/* 分页 */
.pagination-wrapper {
  padding: 24px 32px;
  border-top: 1px solid #e2e8f0;
  background-color: #f8fafc;
  display: flex;
  justify-content: center;
}

.pagination :deep(.el-pagination) {
  gap: 8px;
}

.pagination :deep(.el-pager li) {
  border-radius: 6px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    padding: 24px 20px;
  }
  
  .search-filter-section {
    padding: 20px;
  }
  
  .notes-content {
    padding: 20px;
  }
  
  .pagination-wrapper {
    padding: 20px;
  }
  
  .note-grid {
    grid-template-columns: 1fr;
  }
  
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .view-toggle {
    margin-left: 0;
    align-self: flex-end;
  }
}
</style>
