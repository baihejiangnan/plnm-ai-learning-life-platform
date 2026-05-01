<template>
  <div class="note-list-container note-workspace">
    <section class="notes-hero">
      <div class="header-left">
        <span class="title-icon-wrap">
          <el-icon><Document /></el-icon>
        </span>
        <div>
          <h1 class="page-title">我的笔记</h1>
          <p class="page-subtitle">集中管理笔记、标签、浏览量和字数，快速进入写作与复盘。</p>
        </div>
      </div>
      <div class="header-right">
        <el-button @click="toggleManageMode">
          <el-icon><Setting /></el-icon>
          {{ manageMode ? '退出管理' : '批量管理' }}
        </el-button>
        <el-button type="primary" @click="createNote" class="create-btn">
          <el-icon><Plus /></el-icon>
          新建笔记
        </el-button>
      </div>
    </section>

    <section class="note-stat-grid">
      <article v-for="item in noteStats" :key="item.label" class="note-stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.help }}</small>
      </article>
    </section>

    <section class="notes-toolbar">
      <div class="search-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标题、摘要或正文"
          :prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
          class="search-input"
        />
        <el-button type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
      </div>

      <div class="filter-row">
        <el-select
          v-model="selectedTagId"
          placeholder="全部标签"
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

        <el-select v-model="sortBy" @change="handleSort" class="sort-select">
          <el-option label="最近更新" value="updateTime" />
          <el-option label="最新创建" value="createTime" />
          <el-option label="标题排序" value="title" />
          <el-option label="浏览量" value="viewCount" />
          <el-option label="字数" value="wordCount" />
        </el-select>
        <el-select v-model="sortOrder" @change="handleSort" class="sort-order-select">
          <el-option label="降序" value="desc" />
          <el-option label="升序" value="asc" />
        </el-select>

        <el-radio-group v-model="viewMode" @change="handleViewModeChange" class="view-toggle">
          <el-radio-button label="list">
            <el-icon><List /></el-icon>
          </el-radio-button>
          <el-radio-button label="grid">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>

      <div v-if="activeFilterText" class="active-filter-row">
        <span>{{ activeFilterText }}</span>
        <el-button text @click="clearAllFilters">清除筛选</el-button>
      </div>
    </section>

    <section v-if="manageMode" class="manage-bar">
      <el-checkbox v-model="selectAll" @change="toggleSelectAll">全选当前页</el-checkbox>
      <span class="manage-count">已选 {{ selectedIds.length }} 项</span>
      <el-button type="danger" :disabled="selectedIds.length === 0" @click="confirmBatchDelete" plain>
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
    </section>

    <section class="notes-layout-grid">
      <main class="notes-main-panel" v-loading="noteLoading">
        <div class="notes-panel-head">
          <div>
            <strong>笔记列表</strong>
            <span>{{ notes.length }} / {{ noteTotal }} 篇</span>
          </div>
          <span class="sort-chip">{{ currentSortText }}</span>
        </div>

        <div v-if="notes.length === 0" class="empty-state">
          <el-empty description="暂无笔记">
            <el-button type="primary" @click="createNote">创建第一篇笔记</el-button>
          </el-empty>
        </div>

        <div v-else-if="viewMode === 'list'" class="note-list">
          <article
            v-for="note in notes"
            :key="note.id"
            class="note-item"
            @mouseenter="focusedNote = note"
            @click="viewNote(note.id)"
          >
            <div class="note-leading">
              <el-checkbox v-if="manageMode" v-model="checkedMap[note.id]" @click.stop />
              <span class="note-mark">{{ firstLetter(note.title) }}</span>
            </div>
            <div class="note-content">
              <div class="note-header">
                <h3 class="note-title">{{ note.title || '无标题' }}</h3>
                <div class="note-actions">
                  <el-button link type="primary" @click.stop="editNote(note.id)" class="action-btn">
                    <el-icon><EditPen /></el-icon>
                  </el-button>
                  <el-button link type="danger" @click.stop="deleteNote(note)" class="action-btn">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <p class="note-summary">{{ note.summary || '暂无摘要' }}</p>
              <div class="note-tags" v-if="note.tags && note.tags.length > 0">
                <el-tag v-for="tag in note.tags.slice(0, 5)" :key="tag.id" :color="tag.color" size="small" class="note-tag">
                  {{ tag.name }}
                </el-tag>
              </div>
            </div>
            <div class="note-meta-column">
              <span><el-icon><Clock /></el-icon>{{ formatDate(note.updatedTime || note.updateTime) }}</span>
              <span><el-icon><View /></el-icon>{{ note.viewCount ?? 0 }}</span>
              <span><el-icon><ChatDotRound /></el-icon>{{ note.wordCount ?? 0 }} 字</span>
            </div>
          </article>
        </div>

        <div v-else class="note-grid">
          <article
            v-for="note in notes"
            :key="note.id"
            class="note-card"
            @mouseenter="focusedNote = note"
            @click="viewNote(note.id)"
          >
            <div class="card-topline">
              <span class="note-mark">{{ firstLetter(note.title) }}</span>
              <el-checkbox v-if="manageMode" v-model="checkedMap[note.id]" @click.stop />
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
            <h3 class="card-title">{{ note.title || '无标题' }}</h3>
            <p class="card-summary">{{ note.summary || '暂无摘要' }}</p>
            <div class="card-tags" v-if="note.tags && note.tags.length > 0">
              <el-tag v-for="tag in note.tags.slice(0, 3)" :key="tag.id" :color="tag.color" size="small" class="card-tag">
                {{ tag.name }}
              </el-tag>
              <span v-if="note.tags.length > 3" class="more-tags">+{{ note.tags.length - 3 }}</span>
            </div>
            <div class="card-footer">
              <span class="footer-date">{{ formatDate(note.updatedTime || note.updateTime) }}</span>
              <div class="footer-stats">
                <span><el-icon><View /></el-icon>{{ note.viewCount ?? 0 }}</span>
                <span><el-icon><ChatDotRound /></el-icon>{{ note.wordCount ?? 0 }}</span>
              </div>
            </div>
          </article>
        </div>

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
      </main>

      <aside class="note-side-panel">
        <section class="side-card preview-card">
          <header>
            <span>当前预览</span>
            <el-button v-if="previewNote" text @click="editNote(previewNote.id)">编辑</el-button>
          </header>
          <template v-if="previewNote">
            <h3>{{ previewNote.title || '无标题' }}</h3>
            <p>{{ previewNote.summary || '暂无摘要' }}</p>
            <div class="preview-meta">
              <span><el-icon><View /></el-icon>{{ previewNote.viewCount ?? 0 }}</span>
              <span><el-icon><ChatDotRound /></el-icon>{{ previewNote.wordCount ?? 0 }} 字</span>
            </div>
          </template>
          <el-empty v-else description="暂无预览" />
        </section>

        <section class="side-card">
          <header>
            <span>标签筛选</span>
            <small>{{ tags.length }} 个</small>
          </header>
          <div class="tag-cloud">
            <button
              v-for="tag in topTags"
              :key="tag.id"
              type="button"
              :class="{ active: selectedTagId === tag.id }"
              @click="quickFilterTag(tag.id)"
            >
              <span class="tag-color" :style="{ backgroundColor: tag.color }"></span>
              {{ tag.name }}
              <em>{{ tag.noteCount ?? 0 }}</em>
            </button>
          </div>
        </section>
      </aside>
    </section>
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
const focusedNote = ref(null)

// 计算属性
const notes = computed(() => noteStore.currentPageNotes)
const noteTotal = computed(() => noteStore.totalNotes)
const noteLoading = computed(() => noteStore.noteLoading)
const tags = computed(() => noteStore.allTags)
const selectedIds = computed(() => Object.keys(checkedMap.value).filter(id => checkedMap.value[id]).map(id => Number(id)))
const totalWords = computed(() => notes.value.reduce((sum, note) => sum + Number(note.wordCount || 0), 0))
const totalViews = computed(() => notes.value.reduce((sum, note) => sum + Number(note.viewCount || 0), 0))
const latestNote = computed(() => notes.value[0] || null)
const previewNote = computed(() => focusedNote.value || latestNote.value)
const topTags = computed(() => [...tags.value].sort((a, b) => Number(b.noteCount || 0) - Number(a.noteCount || 0)).slice(0, 8))
const noteStats = computed(() => [
  { label: '全部笔记', value: noteTotal.value, help: `当前页 ${notes.value.length} 篇` },
  { label: '当前页字数', value: totalWords.value, help: '基于当前筛选统计' },
  { label: '当前页浏览', value: totalViews.value, help: '累计阅读热度' },
  { label: '标签数量', value: tags.value.length, help: selectedTagId.value ? '正在按标签筛选' : '全部标签' }
])
const currentSortText = computed(() => {
  const sortMap = {
    updateTime: '最近更新',
    createTime: '最新创建',
    title: '标题排序',
    viewCount: '浏览量',
    wordCount: '字数'
  }
  return `${sortMap[sortBy.value] || '最近更新'} · ${sortOrder.value === 'asc' ? '升序' : '降序'}`
})
const activeFilterText = computed(() => {
  const parts = []
  if (searchKeyword.value) {
    parts.push(`关键词：${searchKeyword.value}`)
  }
  const selectedTag = tags.value.find(tag => tag.id === selectedTagId.value)
  if (selectedTag) {
    parts.push(`标签：${selectedTag.name}`)
  }
  return parts.join(' / ')
})

// 方法
const syncFocusedNote = () => {
  if (!notes.value.some(note => note.id === focusedNote.value?.id)) {
    focusedNote.value = notes.value[0] || null
  }
}

const firstLetter = (title = '') => {
  const text = String(title || '记').trim()
  return text ? text.slice(0, 1).toUpperCase() : '记'
}

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

const handleSearch = async () => {
  currentPage.value = 1
  await noteStore.searchNotes(searchKeyword.value)
  syncFocusedNote()
}

const handleTagFilter = async () => {
  currentPage.value = 1
  await noteStore.filterByTag(selectedTagId.value)
  syncFocusedNote()
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

const clearAllFilters = async () => {
  searchKeyword.value = ''
  selectedTagId.value = null
  currentPage.value = 1
  noteStore.clearSearch()
  router.replace({ query: {} })
  await loadNotes()
}

const quickFilterTag = (tagId) => {
  selectedTagId.value = selectedTagId.value === tagId ? null : tagId
  handleTagFilter()
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
  syncFocusedNote()
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
  syncFocusedNote()
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

/* 笔记页工作台重构 */
.note-workspace {
  display: grid;
  gap: 14px;
  padding: 0;
  background: transparent;
  border-radius: 0;
  box-shadow: none;
  overflow: visible;
}

.notes-hero,
.notes-toolbar,
.manage-bar,
.notes-main-panel,
.note-side-panel,
.note-stat-card,
.side-card {
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(33, 54, 86, 0.05);
}

.notes-hero {
  min-height: 96px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px;
  background:
    linear-gradient(120deg, rgba(31, 111, 235, 0.08), rgba(8, 158, 112, 0.08)),
    #ffffff;
}

.notes-hero .header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.notes-hero .header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.title-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #1f6feb;
  background: #e9f2ff;
  font-size: 22px;
}

.notes-hero .page-title {
  display: block;
  margin: 0;
  color: #172033;
  font-size: 26px;
  line-height: 1.2;
  font-weight: 850;
}

.notes-hero .page-subtitle {
  margin: 6px 0 0;
  color: #66758b;
  font-size: 13px;
  opacity: 1;
}

.notes-hero .create-btn {
  background: #1f6feb;
  border-color: #1f6feb;
  color: #ffffff;
  border-radius: 8px;
  font-weight: 800;
  padding: 0 18px;
  transform: none;
}

.notes-hero .create-btn:hover {
  background: #1557c0;
  border-color: #1557c0;
  color: #ffffff;
  transform: none;
}

.note-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.note-stat-card {
  min-height: 92px;
  display: grid;
  gap: 6px;
  padding: 14px;
}

.note-stat-card span,
.note-stat-card small {
  color: #66758b;
  font-size: 12px;
}

.note-stat-card strong {
  color: #172033;
  font-size: 26px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.notes-toolbar {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  gap: 10px;
}

.notes-toolbar .search-input {
  max-width: none;
}

.filter-row {
  display: grid;
  grid-template-columns: 170px 150px 120px auto;
  gap: 10px;
  align-items: center;
}

.sort-order-select {
  min-width: 120px;
}

.notes-toolbar .view-toggle {
  margin-left: 0;
  justify-self: end;
}

.active-filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-radius: 8px;
  padding: 8px 10px;
  color: #1557c0;
  background: #e9f2ff;
  font-size: 13px;
  font-weight: 750;
}

.notes-layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 14px;
  align-items: start;
}

.notes-main-panel {
  min-width: 0;
  overflow: hidden;
}

.notes-panel-head {
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px;
  border-bottom: 1px solid #e4ebf3;
}

.notes-panel-head > div {
  display: grid;
  gap: 2px;
}

.notes-panel-head strong {
  color: #172033;
}

.notes-panel-head span {
  color: #66758b;
  font-size: 12px;
}

.sort-chip {
  border: 1px solid #d7e1ec;
  border-radius: 999px;
  padding: 4px 10px;
  background: #fbfdff;
  color: #526276;
  font-size: 12px;
  font-weight: 750;
  white-space: nowrap;
}

.note-list {
  display: grid;
  gap: 8px;
  padding: 10px;
}

.note-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 160px;
  gap: 12px;
  align-items: start;
  margin: 0;
  padding: 12px;
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  background: #fbfdff;
  cursor: pointer;
  transform: none;
}

.note-item:hover {
  border-color: #bed6ff;
  background: #f4f9ff;
  box-shadow: none;
  transform: none;
}

.note-leading {
  display: grid;
  gap: 8px;
  justify-items: center;
}

.note-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #1f6feb;
  background: #dbeafe;
  font-weight: 850;
}

.note-content {
  min-width: 0;
}

.note-title,
.card-title {
  color: #172033;
}

.note-summary,
.card-summary {
  color: #66758b;
  font-size: 13px;
  line-height: 1.7;
}

.note-tags,
.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.note-meta-column {
  display: grid;
  justify-items: end;
  gap: 8px;
  color: #66758b;
  font-size: 12px;
}

.note-meta-column span,
.footer-stats span,
.preview-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
  padding: 10px;
}

.note-card {
  min-height: 210px;
  display: grid;
  grid-template-rows: auto auto 1fr auto auto;
  gap: 10px;
  padding: 14px;
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  background: #fbfdff;
  transform: none;
}

.note-card:hover {
  border-color: #bed6ff;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(33, 54, 86, 0.07);
  transform: translateY(-1px);
}

.card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-title {
  margin: 0;
  font-size: 17px;
  font-weight: 850;
}

.card-summary,
.note-summary {
  margin: 0;
}

.more-tags {
  color: #66758b;
  font-size: 12px;
}

.card-footer {
  border-top: 1px solid #e4ebf3;
  padding-top: 10px;
}

.note-side-panel {
  display: grid;
  gap: 14px;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.side-card {
  padding: 14px;
}

.side-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  color: #66758b;
  font-size: 13px;
  font-weight: 800;
}

.preview-card h3 {
  margin: 0 0 8px;
  color: #172033;
  font-size: 18px;
  line-height: 1.35;
}

.preview-card p {
  margin: 0;
  color: #66758b;
  line-height: 1.7;
}

.preview-meta {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  color: #66758b;
  font-size: 12px;
}

.tag-cloud {
  display: grid;
  gap: 8px;
}

.tag-cloud button {
  min-height: 36px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  padding: 0 10px;
  background: #fbfdff;
  color: #172033;
  cursor: pointer;
  font-weight: 750;
}

.tag-cloud button.active,
.tag-cloud button:hover {
  border-color: #bed6ff;
  background: #e9f2ff;
}

.tag-cloud em {
  margin-left: auto;
  color: #66758b;
  font-size: 12px;
  font-style: normal;
}

.manage-bar {
  padding: 10px 12px;
  background: #ffffff;
}

.pagination-wrapper {
  padding: 12px;
  border-top: 1px solid #e4ebf3;
  background: #ffffff;
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .notes-layout-grid {
    grid-template-columns: 1fr;
  }

  .note-side-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .note-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .notes-hero,
  .search-row,
  .filter-row {
    grid-template-columns: 1fr;
  }

  .notes-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .note-item {
    grid-template-columns: 1fr;
  }

  .note-meta-column {
    justify-items: start;
  }

  .note-side-panel {
    grid-template-columns: 1fr;
  }
}
</style>
