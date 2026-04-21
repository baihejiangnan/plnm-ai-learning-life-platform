<template>
  <div class="note-detail">
    <!-- 页面头部 -->
    <div class="detail-header">
      <div class="header-left">
        <el-button 
          type="text" 
          :icon="ArrowLeft" 
          @click="goBack"
          class="back-btn"
        >
          返回
        </el-button>
        <div class="note-info">
          <h1 class="note-title">{{ note.title || '无标题' }}</h1>
          <div class="note-meta">
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatTime(note.updatedTime || note.createdTime || note.updatedAt || note.createdAt || note.updateTime) }}
            </span>
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ displayAuthor || '未知作者' }}
            </span>
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ note.viewCount || 0 }} 次查看
            </span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button 
          type="primary" 
          :icon="Edit" 
          @click="editNote"
        >
          编辑
        </el-button>
        <el-dropdown @command="handleCommand">
          <el-button :icon="MoreFilled" circle />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="share">
                <el-icon><Share /></el-icon>
                分享
              </el-dropdown-item>
              <el-dropdown-item command="export">
                <el-icon><Download /></el-icon>
                导出
              </el-dropdown-item>
              <el-dropdown-item command="delete" divided>
                <el-icon><Delete /></el-icon>
                删除
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 标签区域 -->
    <div class="tags-section" v-if="note.tags && note.tags.length > 0">
      <el-tag 
        v-for="tag in note.tags" 
        :key="tag.id"
        :color="tag.color"
        class="note-tag"
      >
        {{ tag.name }}
      </el-tag>
    </div>

    <!-- 笔记内容 -->
    <div class="note-content">
      <div class="content-wrapper" v-html="renderedContent"></div>
    </div>

    <!-- 评论区域 -->
    <div class="comments-section">
      <div class="comments-header">
        <h3>评论 ({{ comments.length }})</h3>
      </div>
      
      <!-- 添加评论 -->
      <div class="add-comment">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="写下你的想法..."
          class="comment-input"
        />
        <div class="comment-actions">
          <el-button type="primary" @click="addComment" :disabled="!newComment.trim()">
            发表评论
          </el-button>
        </div>
      </div>

      <!-- 评论列表 -->
      <div class="comments-list">
        <div 
          v-for="comment in comments" 
          :key="comment.id"
          class="comment-item"
        >
          <div class="comment-avatar">
            <el-avatar :src="comment.avatar" :size="32">
              {{ comment.author.charAt(0) }}
            </el-avatar>
          </div>
          <div class="comment-content">
            <div class="comment-header">
              <span class="comment-author">{{ comment.author }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <div class="comment-text">{{ comment.content }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import {
  ArrowLeft,
  Edit,
  MoreFilled,
  Share,
  Download,
  Delete,
  Clock,
  User,
  View
} from '@element-plus/icons-vue'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
  // import { useNotesStore } from '@/stores/notes'
  import { useNoteStore } from '@/stores/note'
  import { useUserStore } from '@/stores/user'
 
 const route = useRoute()
 const router = useRouter()
 const noteStore = useNoteStore()
 const userStore = useUserStore()

// 响应式数据
const note = ref({
  id: '',
  title: '',
  content: '',
  author: '',
  createdAt: '',
  updatedAt: '',
  viewCount: 0,
  tags: []
})

const comments = ref([
  {
    id: 1,
    author: '张三',
    avatar: '',
    content: '这篇笔记写得很好，学到了很多！',
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000)
  },
  {
    id: 2,
    author: '李四',
    avatar: '',
    content: '有些地方还可以补充更多细节。',
    createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000)
  }
])

const newComment = ref('')

// 计算属性
const formatTime = (time) => {
  if (!time) return ''
  let d
  if (typeof time === 'string') {
  // 兼容 'YYYY-MM-DD HH:mm:ss' 与 'YYYY-MM-DDTHH:mm:ss' 两种格式
  const normalized = time.includes('T') ? time : time.replace(' ', 'T')
  const ts = Date.parse(normalized)
  d = isNaN(ts) ? new Date(time) : new Date(ts)
  } else {
  d = new Date(time)
  }
  if (isNaN(d.getTime())) return ''
  return formatDistanceToNow(d, { addSuffix: true, locale: zhCN })
}

const displayAuthor = computed(() => {
  const n = note.value || {}
  return (n.user && (n.user.username || n.user.name))
    || n.author
    || (userStore?.userInfo?.username)
    || (userStore?.userInfo?.name)
    || ''
})

const sanitizeHtml = (content = '') => {
  return String(content)
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
}

const isLikelyHtml = (content = '') => /<[^>]+>/.test(content)

const renderedContent = computed(() => {
  const content = note.value?.content || ''
  if (!content) return '<p class="empty-content">暂无内容</p>'
  if (isLikelyHtml(content)) return sanitizeHtml(content)
  try {
    return sanitizeHtml(marked.parse(content, { gfm: true, breaks: true }))
  } catch (error) {
    console.error('Markdown渲染失败:', error)
    return sanitizeHtml(content)
  }
})

// 方法
const loadNoteDetail = async () => {
  try {
    const noteId = route.params.id
    const data = await noteStore.fetchNoteById(noteId)
    if (data) {
      note.value = data
    } else {
      ElMessage.error('未获取到笔记详情')
    }
  } catch (error) {
    ElMessage.error('加载笔记详情失败')
    console.error('Load note detail error:', error)
  }
}

const goBack = () => {
  router.back()
}

const editNote = () => {
  router.push(`/notes/editor/${note.value.id}`)
}

const handleCommand = async (command) => {
  switch (command) {
    case 'share':
      // 分享功能
      navigator.clipboard.writeText(window.location.href)
      ElMessage.success('链接已复制到剪贴板')
      break
    case 'export':
      // 导出功能
      ElMessage.info('导出功能开发中...')
      break
    case 'delete':
      try {
        await ElMessageBox.confirm(
          '确定要删除这篇笔记吗？删除后无法恢复。',
          '确认删除',
          {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        // 这里应该调用删除API
        // await notesStore.deleteNote(note.value.id)
        ElMessage.success('笔记已删除')
        router.push('/notes/list')
      } catch {
        // 用户取消删除
      }
      break
  }
}

const addComment = async () => {
  if (!newComment.value.trim()) return
  
  try {
    // 这里应该调用API添加评论
    const comment = {
      id: Date.now(),
      author: '当前用户',
      avatar: '',
      content: newComment.value,
      createdAt: new Date()
    }
    
    comments.value.unshift(comment)
    newComment.value = ''
    ElMessage.success('评论发表成功')
  } catch (error) {
    ElMessage.error('发表评论失败')
    console.error('Add comment error:', error)
  }
}

// 生命周期
onMounted(() => {
  loadNoteDetail()
})
</script>

<style scoped>
.note-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
  min-height: calc(100vh - 60px);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  flex: 1;
}

.back-btn {
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;
}

.back-btn:hover {
  color: #409eff;
}

.note-title {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 12px 0;
  line-height: 1.3;
}

.note-meta {
  display: flex;
  gap: 24px;
  color: #8c8c8c;
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.tags-section {
  margin-bottom: 24px;
}

.note-tag {
  margin-right: 8px;
  margin-bottom: 8px;
  border: none;
  font-size: 12px;
}

.note-content {
  margin-bottom: 48px;
}

.content-wrapper {
  padding: 28px 32px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #edf2ff;
  box-shadow: 0 12px 28px rgba(64, 114, 255, 0.08);
  font-size: 16px;
  line-height: 1.8;
  color: #303444;
}

.content-wrapper :deep(h1),
.content-wrapper :deep(h2),
.content-wrapper :deep(h3),
.content-wrapper :deep(h4) {
  margin: 22px 0 12px;
  line-height: 1.35;
  color: #1f2d5a;
}

.content-wrapper :deep(h1) {
  font-size: 30px;
}

.content-wrapper :deep(h2) {
  font-size: 24px;
}

.content-wrapper :deep(h3) {
  font-size: 20px;
}

.content-wrapper :deep(p) {
  margin: 14px 0;
}

.content-wrapper :deep(a) {
  color: #3478f6;
  text-decoration: none;
  border-bottom: 1px solid rgba(52, 120, 246, 0.35);
}

.content-wrapper :deep(blockquote) {
  margin: 16px 0;
  padding: 10px 14px;
  border-left: 4px solid #8aa8ff;
  background: linear-gradient(90deg, rgba(83, 131, 255, 0.1), rgba(83, 131, 255, 0.02));
  color: #455075;
  border-radius: 8px;
}

.content-wrapper :deep(ul),
.content-wrapper :deep(ol) {
  margin: 14px 0;
  padding-left: 24px;
}

.content-wrapper :deep(li) {
  margin: 6px 0;
}

.content-wrapper :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border-radius: 10px;
  overflow: hidden;
}

.content-wrapper :deep(th),
.content-wrapper :deep(td) {
  border: 1px solid #e7ecff;
  padding: 10px 12px;
  text-align: left;
}

.content-wrapper :deep(th) {
  background: #f4f7ff;
}

.content-wrapper :deep(pre) {
  margin: 16px 0;
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid #2a3d7c;
  background: linear-gradient(135deg, #1f2a44 0%, #273a70 50%, #2b4b97 100%);
  color: #e4ecff;
  overflow-x: auto;
  box-shadow: 0 12px 24px rgba(36, 57, 114, 0.28);
}

.content-wrapper :deep(pre code) {
  font-size: 14px;
  line-height: 1.65;
  font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
  background: transparent;
  color: inherit;
}

.content-wrapper :deep(code) {
  background: linear-gradient(90deg, #ffe3f2, #ffd6ea);
  color: #a80a5a;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 0.92em;
  font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
}

.content-wrapper :deep(hr) {
  border: none;
  border-top: 1px solid #e6ebf9;
  margin: 22px 0;
}

.content-wrapper :deep(img) {
  max-width: 100%;
  border-radius: 10px;
}

.content-wrapper :deep(.empty-content) {
  color: #8c95b2;
}

.comments-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 32px;
}

.comments-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 24px 0;
}

.add-comment {
  margin-bottom: 32px;
}

.comment-input {
  margin-bottom: 12px;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}

.comments-list {
  space-y: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 500;
  color: #262626;
  font-size: 14px;
}

.comment-time {
  color: #8c8c8c;
  font-size: 12px;
}

.comment-text {
  color: #595959;
  line-height: 1.6;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .note-detail {
    padding: 16px;
  }
  
  .detail-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .note-meta {
    flex-direction: column;
    gap: 8px;
  }
  
  .note-title {
    font-size: 24px;
  }
  
  .content-wrapper {
    padding: 20px 16px;
    font-size: 15px;
  }
}
</style>
