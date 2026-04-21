<template>
  <div class="tag-manager-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><PriceTag /></el-icon>
          标签管理
        </h1>
        <p class="page-subtitle">管理您的笔记标签，让内容更有条理</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showCreateDialog = true" class="create-btn">
          <el-icon><Plus /></el-icon>
          新建标签
        </el-button>
      </div>
    </div>

    <!-- 搜索和统计区域 -->
    <div class="search-stats-section">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标签名称..."
          prefix-icon="Search"
          clearable
          @input="handleSearch"
          class="search-input"
        />
      </div>
      
      <div class="stats-info">
        <div class="stat-item">
          <span class="stat-number">{{ totalTags }}</span>
          <span class="stat-label">总标签数</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">{{ usedTags }}</span>
          <span class="stat-label">已使用</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">{{ unusedTags }}</span>
          <span class="stat-label">未使用</span>
        </div>
      </div>
    </div>

    <!-- 标签列表 -->
    <div class="tags-content" v-loading="tagLoading">
      <div v-if="filteredTags.length === 0" class="empty-state">
        <el-empty :description="searchKeyword ? '未找到匹配的标签' : '暂无标签'">
          <el-button type="primary" @click="showCreateDialog = true">创建第一个标签</el-button>
        </el-empty>
      </div>
      
      <div v-else class="tag-grid">
        <div
          v-for="tag in filteredTags"
          :key="tag.id"
          class="tag-card"
          :style="{ borderColor: tag.color }"
        >
          <div class="tag-header">
            <div class="tag-info">
              <div class="tag-color" :style="{ backgroundColor: tag.color }"></div>
              <div class="tag-details">
                <h3 class="tag-name">{{ tag.name }}</h3>
                <p class="tag-description" v-if="tag.description">
                  {{ tag.description }}
                </p>
              </div>
            </div>
            
            <el-dropdown @command="handleTagAction" class="tag-actions">
              <el-button link class="more-btn">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="{ action: 'edit', tag }">
                    <el-icon><EditPen /></el-icon>
                    编辑
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'view', tag }">
                    <el-icon><View /></el-icon>
                    查看笔记
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'delete', tag }" divided>
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          
          <div class="tag-stats">
            <div class="stat-row">
              <span class="stat-label">关联笔记</span>
              <span class="stat-value">{{ tag.noteCount || 0 }} 篇</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">创建时间</span>
              <span class="stat-value">{{ formatDate(tag.createTime) }}</span>
            </div>
            <div class="stat-row" v-if="tag.lastUsedTime">
              <span class="stat-label">最近使用</span>
              <span class="stat-value">{{ formatDate(tag.lastUsedTime) }}</span>
            </div>
          </div>
          
          <div class="tag-footer">
            <el-button
              link
              type="primary"
              @click="viewTagNotes(tag)"
              :disabled="!tag.noteCount"
              class="view-notes-btn"
            >
              查看相关笔记
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="totalTags > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 48, 96]"
        :total="totalTags"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </div>

    <!-- 创建/编辑标签弹窗 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingTag ? '编辑标签' : '创建标签'"
      width="500px"
      class="tag-dialog"
      @close="resetForm"
    >
      <el-form
        ref="tagFormRef"
        :model="tagForm"
        :rules="tagFormRules"
        label-width="80px"
        class="tag-form"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input
            v-model="tagForm.name"
            placeholder="请输入标签名称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="标签颜色" prop="color">
          <div class="color-picker-section">
            <div class="preset-colors">
              <div
                v-for="color in presetColors"
                :key="color"
                :class="['color-option', { active: tagForm.color === color }]"
                :style="{ backgroundColor: color }"
                @click="tagForm.color = color"
              ></div>
            </div>
            <el-color-picker
              v-model="tagForm.color"
              show-alpha
              :predefine="presetColors"
              class="custom-color-picker"
            />
          </div>
        </el-form-item>
        
        <el-form-item label="标签描述">
          <el-input
            v-model="tagForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入标签描述（可选）"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="标签图标">
          <el-select
            v-model="tagForm.icon"
            placeholder="选择图标（可选）"
            clearable
            class="icon-select"
          >
            <el-option
              v-for="icon in tagIcons"
              :key="icon.value"
              :label="icon.label"
              :value="icon.value"
            >
              <div class="icon-option">
                <el-icon><component :is="icon.component" /></el-icon>
                <span>{{ icon.label }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button
            type="primary"
            @click="submitForm"
            :loading="submitting"
          >
            {{ editingTag ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量操作弹窗 -->
    <el-dialog
      v-model="showBatchDialog"
      title="批量操作"
      width="400px"
    >
      <div class="batch-content">
        <p>已选择 {{ selectedTags.length }} 个标签</p>
        <div class="batch-actions">
          <el-button
            type="danger"
            @click="batchDeleteTags"
            :loading="batchDeleting"
          >
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNoteStore } from '@/stores/note'
import {
  PriceTag,
  Plus,
  Search,
  MoreFilled,
  EditPen,
  View,
  Delete,
  ArrowRight,
  Document,
  Folder,
  Star,
  Flag,
  // BookmarkOne, // 该图标在 @element-plus/icons-vue 中不存在，已移除
  // Tag, // 该图标在 @element-plus/icons-vue 中不存在，已移除
  Collection
} from '@element-plus/icons-vue'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

const router = useRouter()
const noteStore = useNoteStore()

// 表单引用
const tagFormRef = ref(null)

// 响应式数据
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(24)
const showCreateDialog = ref(false)
const showBatchDialog = ref(false)
const editingTag = ref(null)
const submitting = ref(false)
const batchDeleting = ref(false)
const selectedTags = ref([])

// 标签表单
const tagForm = ref({
  name: '',
  color: '#1890ff',
  description: '',
  icon: ''
})

// 表单验证规则
const tagFormRules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 1, max: 20, message: '标签名称长度在 1 到 20 个字符', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请选择标签颜色', trigger: 'change' }
  ]
}

// 预设颜色
const presetColors = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d',
  '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16',
  '#a0d911', '#2f54eb', '#fa541c', '#096dd9',
  '#87d068', '#108ee9', '#f50', '#87ceeb',
  '#ff69b4', '#ba55d3', '#cd853f', '#ffc0cb'
]

// 标签图标选项
const tagIcons = [
  { label: '文档', value: 'document', component: Document },
  { label: '文件夹', value: 'folder', component: Folder },
  { label: '星标', value: 'star', component: Star },
  { label: '旗帜', value: 'flag', component: Flag },
  { label: '书签', value: 'bookmark', component: Star }, // 使用 Star 替代 BookmarkOne
  { label: '标签', value: 'tag', component: Flag }, // 使用 Flag 替代 Tag
  { label: '收藏', value: 'collection', component: Collection }
]

// 计算属性
const allTags = computed(() => noteStore.allTags)
const tagLoading = computed(() => noteStore.tagLoading)

const totalTags = computed(() => allTags.value.length)
const usedTags = computed(() => allTags.value.filter(tag => tag.noteCount > 0).length)
const unusedTags = computed(() => allTags.value.filter(tag => !tag.noteCount || tag.noteCount === 0).length)

const filteredTags = computed(() => {
  let tags = allTags.value
  
  // 搜索过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    tags = tags.filter(tag => 
      tag.name.toLowerCase().includes(keyword) ||
      (tag.description && tag.description.toLowerCase().includes(keyword))
    )
  }
  
  // 分页
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return tags.slice(start, end)
})

// 方法
const handleSearch = () => {
  currentPage.value = 1
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

const handleTagAction = ({ action, tag }) => {
  switch (action) {
    case 'edit':
      editTag(tag)
      break
    case 'view':
      viewTagNotes(tag)
      break
    case 'delete':
      deleteTag(tag)
      break
  }
}

const editTag = (tag) => {
  editingTag.value = tag
  tagForm.value = {
    name: tag.name,
    color: tag.color,
    description: tag.description || '',
    icon: tag.icon || ''
  }
  showCreateDialog.value = true
}

const deleteTag = async (tag) => {
  if (tag.noteCount > 0) {
    ElMessageBox.confirm(
      `标签「${tag.name}」还有 ${tag.noteCount} 篇关联笔记，删除后这些笔记将失去该标签。确定要删除吗？`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      await performDeleteTag(tag)
    }).catch(() => {})
  } else {
    ElMessageBox.confirm(
      `确定要删除标签「${tag.name}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      await performDeleteTag(tag)
    }).catch(() => {})
  }
}

const performDeleteTag = async (tag) => {
  try {
    const success = await noteStore.deleteTag(tag.id)
    if (success) {
      ElMessage.success('标签删除成功')
    }
  } catch (error) {
    console.error('删除标签失败:', error)
    ElMessage.error('删除标签失败')
  }
}

const viewTagNotes = (tag) => {
  // 跳转到笔记列表页面，并筛选该标签的笔记
  router.push({
    path: '/notes/list',
    query: { tagId: tag.id, tagName: tag.name }
  })
}

const submitForm = async () => {
  if (!tagFormRef.value) return
  
  try {
    await tagFormRef.value.validate()
    
    submitting.value = true
    
    const tagData = { ...tagForm.value }
    
    let success
    if (editingTag.value) {
      success = await noteStore.updateTag(editingTag.value.id, tagData)
      if (success) {
        ElMessage.success('标签更新成功')
      }
    } else {
      success = await noteStore.createTag(tagData)
      if (success) {
        ElMessage.success('标签创建成功')
      }
    }
    
    if (success) {
      showCreateDialog.value = false
      resetForm()
    }
  } catch (error) {
    console.error('提交表单失败:', error)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  editingTag.value = null
  tagForm.value = {
    name: '',
    color: '#1890ff',
    description: '',
    icon: ''
  }
  if (tagFormRef.value) {
    tagFormRef.value.clearValidate()
  }
}

const batchDeleteTags = async () => {
  if (selectedTags.value.length === 0) {
    ElMessage.warning('请选择要删除的标签')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedTags.value.length} 个标签吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    batchDeleting.value = true
    
    const deletePromises = selectedTags.value.map(tag => 
      noteStore.deleteTag(tag.id)
    )
    
    await Promise.all(deletePromises)
    
    ElMessage.success('批量删除成功')
    selectedTags.value = []
    showBatchDialog.value = false
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  } finally {
    batchDeleting.value = false
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

// 生命周期
onMounted(async () => {
  await noteStore.fetchTags()
})

// 监听搜索关键词变化
watch(searchKeyword, () => {
  currentPage.value = 1
})
</script>

<style scoped>
.tag-manager-container {
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

/* 搜索统计区域 */
.search-stats-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.search-bar {
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

.stats-info {
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

/* 标签内容区域 */
.tags-content {
  min-height: 400px;
  padding: 24px 32px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.tag-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.tag-card {
  padding: 20px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background-color: #ffffff;
  transition: all 0.2s;
  position: relative;
}

.tag-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.tag-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.tag-info {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
}

.tag-color {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  margin-top: 2px;
}

.tag-details {
  flex: 1;
  min-width: 0;
}

.tag-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
  line-height: 1.3;
  word-break: break-word;
}

.tag-description {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tag-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.tag-card:hover .tag-actions {
  opacity: 1;
}

.more-btn {
  padding: 4px;
  color: #9ca3af;
  border-radius: 4px;
}

.more-btn:hover {
  background-color: #f3f4f6;
  color: #374151;
}

.tag-stats {
  margin-bottom: 16px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.stat-row:last-child {
  margin-bottom: 0;
}

.stat-row .stat-label {
  color: #6b7280;
}

.stat-row .stat-value {
  color: #374151;
  font-weight: 500;
}

.tag-footer {
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.view-notes-btn {
  font-size: 13px;
  padding: 0;
  height: auto;
}

.view-notes-btn:disabled {
  color: #d1d5db;
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

/* 弹窗样式 */
.tag-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.tag-form {
  margin-top: 8px;
}

.color-picker-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.preset-colors {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
  position: relative;
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.active {
  border-color: #1890ff;
  transform: scale(1.1);
}

.color-option.active::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 8px;
  height: 8px;
  background-color: white;
  border-radius: 50%;
}

.custom-color-picker {
  flex-shrink: 0;
}

.icon-select {
  width: 100%;
}

.icon-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.batch-content {
  text-align: center;
  padding: 20px 0;
}

.batch-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    padding: 24px 20px;
  }
  
  .search-stats-section {
    flex-direction: column;
    gap: 16px;
    padding: 20px;
    align-items: stretch;
  }
  
  .stats-info {
    justify-content: space-around;
    gap: 16px;
  }
  
  .tags-content {
    padding: 20px;
  }
  
  .pagination-wrapper {
    padding: 20px;
  }
  
  .tag-grid {
    grid-template-columns: 1fr;
  }
  
  .preset-colors {
    max-width: 200px;
  }
  
  .color-picker-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .tag-card {
    padding: 16px;
  }
  
  .tag-header {
    margin-bottom: 12px;
  }
  
  .tag-stats {
    margin-bottom: 12px;
  }
  
  .tag-footer {
    padding-top: 12px;
  }
}
</style>