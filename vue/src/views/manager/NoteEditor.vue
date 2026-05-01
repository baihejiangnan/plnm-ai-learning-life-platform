<template>
  <div :class="['note-editor-container', { 'is-focus-mode': isFocusMode, 'is-preview-mode': showPreview }]">
    <button v-if="isFocusMode" type="button" class="focus-exit-button" @click="toggleFocusMode">
      <el-icon><Close /></el-icon>
      退出沉浸
    </button>

    <header v-show="!isFocusMode" class="editor-header editor-commandbar">
      <div class="header-left">
        <el-button link @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="editor-title-meta">
          <span class="eyebrow">NOTE STUDIO</span>
          <strong>{{ isEdit ? '编辑笔记' : '新建笔记' }}</strong>
        </div>
      </div>

      <div class="header-right">
        <div class="doc-stats">
          <span>{{ plainTextLength }} 字</span>
          <span>约 {{ readingMinutes }} 分钟阅读</span>
        </div>
        <div class="save-status">
          <span v-if="saveStatus === 'saving'" class="status-text saving">
            <el-icon class="loading"><Loading /></el-icon>
            保存中
          </span>
          <span v-else-if="saveStatus === 'saved'" class="status-text saved">
            <el-icon><Check /></el-icon>
            已保存
          </span>
          <span v-else-if="saveStatus === 'error'" class="status-text error">
            <el-icon><Warning /></el-icon>
            保存失败
          </span>
          <span v-else-if="savedAtText" class="status-text saved-time">
            {{ savedAtText }}
          </span>
        </div>

        <el-tooltip content="大纲" placement="bottom">
          <el-button @click="showSidebar = !showSidebar" :class="{ active: showSidebar }" class="utility-btn">
            <el-icon><Operation /></el-icon>
          </el-button>
        </el-tooltip>
        <el-button @click="showPreview = !showPreview" :class="['preview-btn', { active: showPreview }]">
          <el-icon><View /></el-icon>
          {{ showPreview ? '编辑' : '预览' }}
        </el-button>
        <el-button @click="toggleFocusMode" class="focus-btn">
          <el-icon><FullScreen /></el-icon>
          沉浸
        </el-button>
        <el-button type="primary" @click="saveNote" :loading="saving" class="save-btn">
          <el-icon><DocumentAdd /></el-icon>
          {{ isEdit ? '更新' : '发布' }}
        </el-button>
      </div>
    </header>

    <div :class="['editor-body', { 'outline-open': showSidebar }]">
      <aside v-show="!isFocusMode" class="editor-side-panel meta-panel">
        <section class="side-section">
          <header>
            <el-icon><Finished /></el-icon>
            <span>写作状态</span>
          </header>
          <div class="metric-list">
            <div>
              <strong>{{ plainTextLength }}</strong>
              <span>字数</span>
            </div>
            <div>
              <strong>{{ readingMinutes }}</strong>
              <span>分钟</span>
            </div>
          </div>
        </section>

        <section class="side-section">
          <header>
            <el-icon><CollectionTag /></el-icon>
            <span>标签</span>
            <button type="button" @click="showTagManager = true">管理</button>
          </header>
          <div v-if="noteForm.tags.length" class="side-tags">
            <el-tag v-for="tag in noteForm.tags" :key="tag.id" :color="tag.color" size="small" class="note-tag">
              {{ tag.name }}
            </el-tag>
          </div>
          <button v-else type="button" class="empty-side-action" @click="showTagManager = true">添加标签</button>
        </section>

        <section class="side-section quick-section">
          <header>
            <el-icon><MagicStick /></el-icon>
            <span>快速插入</span>
          </header>
          <div class="quick-grid">
            <button type="button" @click="insertHeading('h2')">H2</button>
            <button type="button" @click="insertList('ul')">列表</button>
            <button type="button" @click="insertQuote()">引用</button>
            <button type="button" @click="insertCode()">代码</button>
          </div>
        </section>
      </aside>

      <main class="editor-content" :class="{ 'with-sidebar': showSidebar }">
        <section class="writing-page">
          <div class="writing-page-head">
            <span class="mode-chip">
              <el-icon><EditPen /></el-icon>
              {{ showPreview ? '预览' : '编辑' }}
            </span>
            <el-input
              v-model="noteForm.title"
              placeholder="请输入标题..."
              class="title-input"
              :class="{ 'title-focused': titleFocused }"
              @focus="titleFocused = true"
              @blur="titleFocused = false"
              @input="handleTitleChange"
            />
            <div v-if="noteForm.tags.length" class="editor-tag-strip">
              <el-tag v-for="tag in noteForm.tags" :key="tag.id" :color="tag.color" size="small" class="note-tag">
                {{ tag.name }}
              </el-tag>
            </div>
          </div>

          <div v-show="!isFocusMode && !showPreview" class="editor-toolbar editor-inline-toolbar">
            <div class="toolbar-section">
              <el-button-group class="format-group">
                <el-tooltip content="加粗" placement="bottom">
                  <el-button size="small" @click="formatText('bold')" :class="{ active: isFormatActive('bold') }">
                    <strong>B</strong>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="斜体" placement="bottom">
                  <el-button size="small" @click="formatText('italic')" :class="{ active: isFormatActive('italic') }">
                    <em>I</em>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="下划线" placement="bottom">
                  <el-button size="small" @click="formatText('underline')" :class="{ active: isFormatActive('underline') }">
                    <u>U</u>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除线" placement="bottom">
                  <el-button size="small" @click="formatText('strikethrough')" :class="{ active: isFormatActive('strikethrough') }">
                    <s>S</s>
                  </el-button>
                </el-tooltip>
              </el-button-group>

              <el-divider direction="vertical" />

              <el-dropdown @command="insertHeading">
                <el-button size="small">
                  标题 <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="h1">标题 1</el-dropdown-item>
                    <el-dropdown-item command="h2">标题 2</el-dropdown-item>
                    <el-dropdown-item command="h3">标题 3</el-dropdown-item>
                    <el-dropdown-item command="h4">标题 4</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <el-divider direction="vertical" />

              <el-button-group class="list-group">
                <el-tooltip content="无序列表" placement="bottom">
                  <el-button size="small" @click="insertList('ul')">
                    <el-icon><List /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-button size="small" @click="insertList('ol')">有序列表</el-button>
                <el-button size="small" @click="insertQuote()">引用</el-button>
              </el-button-group>

              <el-divider direction="vertical" />

              <el-button-group class="insert-group">
                <el-tooltip content="链接" placement="bottom">
                  <el-button size="small" @click="insertLink()">
                    <el-icon><Link /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="图片" placement="bottom">
                  <el-button size="small" @click="insertImage()">
                    <el-icon><Picture /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="表格" placement="bottom">
                  <el-button size="small" @click="insertTable()">
                    <el-icon><Grid /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-button size="small" @click="insertCode()">&lt;/&gt;</el-button>
              </el-button-group>
            </div>

            <div class="toolbar-right">
              <el-button size="small" @click="showTagManager = true">
                <el-icon><PriceTag /></el-icon>
                标签
              </el-button>
            </div>
          </div>

          <div v-show="!showPreview" class="edit-mode">
            <div
              ref="editorRef"
              class="rich-editor"
              contenteditable="true"
              @input="handleContentChange"
              @keydown="handleKeydown"
              @paste="handlePaste"
              @compositionstart="handleCompositionStart"
              @compositionupdate="handleCompositionUpdate"
              @compositionend="handleCompositionEnd"
            ></div>
          </div>

          <div v-show="showPreview" class="preview-mode">
            <div class="preview-content markdown-body" v-html="renderedContent"></div>
          </div>
        </section>
      </main>

      <aside v-show="!isFocusMode && showSidebar" class="editor-sidebar">
        <div class="sidebar-header">
          <h4>大纲</h4>
          <el-button link @click="showSidebar = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div v-if="outline.length" class="outline-content">
          <button
            v-for="heading in outline"
            :key="heading.id"
            type="button"
            :class="['outline-item', `level-${heading.level}`]"
            @click="scrollToHeading(heading.id)"
          >
            {{ heading.text }}
          </button>
        </div>
        <div v-else class="outline-empty">
          <el-icon><Memo /></el-icon>
          暂无大纲
        </div>
      </aside>
    </div>

    <!-- 标签管理弹窗 -->
    <el-dialog
      v-model="showTagManager"
      title="管理标签"
      width="500px"
      class="tag-manager-dialog"
    >
      <div class="tag-manager-content">
        <!-- 当前标签 -->
        <div class="current-tags">
          <h4>当前标签</h4>
          <div class="tag-list">
            <el-tag
              v-for="tag in noteForm.tags"
              :key="tag.id"
              :color="tag.color"
              closable
              @close="removeTag(tag.id)"
              class="note-tag"
            >
              {{ tag.name }}
            </el-tag>
            <el-button
              v-if="noteForm.tags.length === 0"
              link
              type="info"
              class="add-tag-hint"
            >
              点击下方标签添加
            </el-button>
          </div>
        </div>
        
        <!-- 可用标签 -->
        <div class="available-tags">
          <div class="tags-header">
            <h4>可用标签</h4>
            <el-button size="small" @click="showCreateTag = true">
              <el-icon><Plus /></el-icon>
              新建标签
            </el-button>
          </div>
          
          <div class="tag-search">
            <el-input
              v-model="tagSearchKeyword"
              placeholder="搜索标签..."
              prefix-icon="Search"
              size="small"
              clearable
            />
          </div>
          
          <div class="tag-list available">
            <el-tag
              v-for="tag in filteredAvailableTags"
              :key="tag.id"
              :color="tag.color"
              @click="addTag(tag)"
              class="available-tag"
            >
              {{ tag.name }}
            </el-tag>
          </div>
        </div>
        
        <!-- 创建新标签 -->
        <div v-if="showCreateTag" class="create-tag-section">
          <el-divider />
          <h4>创建新标签</h4>
          <div class="create-tag-form">
            <el-input
              v-model="newTag.name"
              placeholder="标签名称"
              size="small"
              style="margin-bottom: 12px;"
            />
            <div class="color-picker">
              <span class="color-label">颜色：</span>
              <div class="color-options">
                <div
                  v-for="color in tagColors"
                  :key="color"
                  :class="['color-option', { active: newTag.color === color }]"
                  :style="{ backgroundColor: color }"
                  @click="newTag.color = color"
                ></div>
              </div>
            </div>
            <div class="create-tag-actions">
              <el-button size="small" @click="cancelCreateTag">取消</el-button>
              <el-button size="small" type="primary" @click="createTag">创建</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 插入链接弹窗 -->
    <el-dialog v-model="showLinkDialog" title="插入链接" width="400px">
      <el-form :model="linkForm" label-width="60px">
        <el-form-item label="文本">
          <el-input v-model="linkForm.text" placeholder="链接文本" />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="linkForm.url" placeholder="https://" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLinkDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmInsertLink">确定</el-button>
      </template>
    </el-dialog>

    <!-- 插入图片弹窗 -->
    <el-dialog v-model="showImageDialog" title="插入图片" width="400px">
      <el-tabs v-model="imageTabActive">
        <el-tab-pane label="上传图片" name="upload">
          <el-upload
            class="image-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleImageSuccess"
            :before-upload="beforeImageUpload"
            accept="image/*"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              选择图片
            </el-button>
          </el-upload>
        </el-tab-pane>
        <el-tab-pane label="网络图片" name="url">
          <el-input
            v-model="imageForm.url"
            placeholder="请输入图片URL"
            style="margin-bottom: 12px;"
          />
          <el-input
            v-model="imageForm.alt"
            placeholder="图片描述（可选）"
          />
          <div style="margin-top: 12px;">
            <el-button @click="showImageDialog = false">取消</el-button>
            <el-button type="primary" @click="confirmInsertImage">插入</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNoteStore } from '@/stores/note'
import { useUserStore } from '@/stores/user'
import { marked } from 'marked'
import {
  ArrowLeft,
  Loading,
  Check,
  Warning,
  View,
  DocumentAdd,
  // Bold, // Bold图标不存在于Element Plus Icons中，已移除
  // Italic, // 该图标在 @element-plus/icons-vue 中不存在，已移除
  // Underline, // 该图标在 @element-plus/icons-vue 中不存在，已移除
  // Strikethrough, // 该图标在 @element-plus/icons-vue 中不存在，已移除
  ArrowDown,
  List,
  // Numbered, // 该图标在 @element-plus/icons-vue 中不存在，已移除
  // ChatQuote, // 该图标在 @element-plus/icons-vue 中不存在，已移除
  Link,
  Picture,
  Grid,
  // Code, // 该图标在 @element-plus/icons-vue 中不存在，已移除
  PriceTag,
  Close,
  Plus,
  Upload,
  FullScreen,
  Operation,
  Finished,
  CollectionTag,
  MagicStick,
  EditPen,
  Memo
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const noteStore = useNoteStore()
const userStore = useUserStore()

// 编辑器引用
const editorRef = ref(null)
const initialSnapshot = ref('')
const lastSavedAt = ref(null)

// 基础状态
const isEdit = computed(() => !!route.params.id)
const noteId = computed(() => route.params.id)
const saving = ref(false)
const saveStatus = ref('') // 'saving', 'saved', 'error'
const titleFocused = ref(false)
const showPreview = ref(false)
const showSidebar = ref(false)
const isFocusMode = ref(false)

// 输入法状态管理
const isComposing = ref(false)
const compositionText = ref('')

// 笔记表单数据
const noteForm = ref({
  title: '',
  content: '',
  summary: '',
  tags: [],
  isPublic: false
})

// 标签管理
const showTagManager = ref(false)
const tagSearchKeyword = ref('')
const showCreateTag = ref(false)
const newTag = ref({
  name: '',
  color: '#1890ff'
})

const tagColors = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d',
  '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16',
  '#a0d911', '#2f54eb', '#fa541c', '#096dd9'
]

// 弹窗状态
const showLinkDialog = ref(false)
const showImageDialog = ref(false)
const imageTabActive = ref('upload')

// 表单数据
const linkForm = ref({
  text: '',
  url: ''
})

const imageForm = ref({
  url: '',
  alt: ''
})

// 上传配置
const uploadUrl = computed(() => `${import.meta.env.VITE_BASE_URL}/files/upload`)
const uploadHeaders = computed(() => ({
   'Authorization': `Bearer ${userStore.token}`
 }))

// 计算属性
const allTags = computed(() => noteStore.allTags)
const filteredAvailableTags = computed(() => {
  const currentTagIds = noteForm.value.tags.map(tag => tag.id)
  return allTags.value
    .filter(tag => !currentTagIds.includes(tag.id))
    .filter(tag => 
      !tagSearchKeyword.value || 
      tag.name.toLowerCase().includes(tagSearchKeyword.value.toLowerCase())
    )
})

const outline = computed(() => {
  // 从内容中提取标题生成大纲
  const content = noteForm.value.content
  const headings = []
  const headingRegex = /<h([1-6]).*?id="([^"]*?)".*?>(.*?)<\/h[1-6]>/gi
  let match
  
  while ((match = headingRegex.exec(content)) !== null) {
    headings.push({
      id: match[2],
      level: parseInt(match[1]),
      text: match[3].replace(/<[^>]*>/g, '')
    })
  }
  
  return headings
})

const renderedContent = computed(() => {
  const content = noteForm.value.content || ''
  if (!content) return '<p class="empty-content">开始书写内容后，这里会实时预览</p>'
  if (/<[^>]+>/.test(content)) return sanitizeHtml(content)
  try {
    return sanitizeHtml(marked.parse(content, { gfm: true, breaks: true }))
  } catch (error) {
    console.error('Markdown渲染错误:', error)
    return sanitizeHtml(content)
  }
})

const plainTextLength = computed(() => {
  const html = noteForm.value.content || ''
  const text = html.replace(/<[^>]*>/g, '').replace(/\s+/g, '')
  return text.length
})

const readingMinutes = computed(() => {
  const count = plainTextLength.value
  if (count === 0) return 0
  return Math.max(1, Math.ceil(count / 350))
})

const savedAtText = computed(() => {
  if (!lastSavedAt.value) return ''
  const date = new Date(lastSavedAt.value)
  const hh = `${date.getHours()}`.padStart(2, '0')
  const mm = `${date.getMinutes()}`.padStart(2, '0')
  return `${hh}:${mm}`
})

const sanitizeHtml = (content = '') => {
  return String(content)
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
}

const sanitizeEditorContent = (content = '') => {
  return sanitizeHtml(content)
    .replace(/null/g, '')
    .replace(/undefined/g, '')
}

const snapshotContent = () => {
  const title = (noteForm.value.title || '').trim()
  const content = sanitizeEditorContent(noteForm.value.content || '').trim()
  return JSON.stringify({
    title,
    content,
    tags: (noteForm.value.tags || []).map(tag => tag.id).sort((a, b) => a - b)
  })
}

const markSnapshot = () => {
  initialSnapshot.value = snapshotContent()
}

// 自动保存
let autoSaveTimer = null
const startAutoSave = () => {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  autoSaveTimer = setTimeout(() => {
    if (noteForm.value.title || noteForm.value.content) {
      autoSaveNote()
    }
  }, 3000) // 3秒后自动保存
}

const autoSaveNote = async () => {
  if (saving.value) return

  await persistNote({ silent: true })
}

const persistNote = async ({ silent = false } = {}) => {
  try {
    saving.value = true
    saveStatus.value = 'saving'
    const noteData = {
      ...noteForm.value,
      content: sanitizeEditorContent(noteForm.value.content || ''),
      summary: generateSummary(noteForm.value.content || '')
    }

    let result
    if (isEdit.value) {
      result = await noteStore.updateNote(noteId.value, noteData)
      if (result && !silent) {
        ElMessage.success('笔记更新成功')
      }
    } else {
      result = await noteStore.createNote(noteData)
      if (result && !silent) {
        ElMessage.success('笔记创建成功')
      }
      if (result && result.id) {
        router.replace(`/notes/editor/${result.id}`)
      }
    }
    
    if (result && (isEdit.value || (!isEdit.value && result.id))) {
      saveStatus.value = 'saved'
      noteForm.value.content = noteData.content
      if (editorRef.value && editorRef.value.innerHTML !== noteData.content) {
        editorRef.value.innerHTML = noteData.content
      }
      markSnapshot()
      lastSavedAt.value = Date.now()
      setTimeout(() => {
        saveStatus.value = ''
      }, 2000)
    } else {
      // 后端已给出错误提示（store 内部），这里仅更新状态
      saveStatus.value = 'error'
      setTimeout(() => {
        saveStatus.value = ''
      }, 3000)
    }
  } catch (error) {
    console.error('保存失败:', error)
    saveStatus.value = 'error'
    if (!silent) {
      ElMessage.error('保存失败，请重试')
    }
    setTimeout(() => {
      saveStatus.value = ''
    }, 3000)
  } finally {
    saving.value = false
  }
}

// 方法
const goBack = () => {
  if (hasUnsavedChanges()) {
    ElMessageBox.confirm(
      '您有未保存的更改，确定要离开吗？',
      '确认离开',
      {
        confirmButtonText: '离开',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      router.back()
    }).catch(() => {})
  } else {
    router.back()
  }
}

const hasUnsavedChanges = () => {
  return snapshotContent() !== initialSnapshot.value
}

const toggleFocusMode = async () => {
  isFocusMode.value = !isFocusMode.value
  if (isFocusMode.value) {
    showSidebar.value = false
  }
  await nextTick()
  editorRef.value?.focus()
}

const handleEditorGlobalKeydown = (event) => {
  if (event.key === 'Escape' && isFocusMode.value) {
    event.preventDefault()
    toggleFocusMode()
    return
  }

  if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key.toLowerCase() === 'f') {
    event.preventDefault()
    toggleFocusMode()
  }
}

const handleTitleChange = () => {
  startAutoSave()
}

const handleContentChange = (event) => {
  // 如果正在输入法组合输入，不处理内容变化
  if (isComposing.value) {
    return
  }
  
  // 过滤输入法结束后的浏览器默认提交触发的重复input
  if (event && event.isComposing === true) {
    return
  }
  
  // 保存当前光标位置
  const selection = window.getSelection()
  let savedRange = null
  if (selection.rangeCount > 0) {
    savedRange = selection.getRangeAt(0).cloneRange()
  }
  
  // 获取编辑器内容并清理
  const content = editorRef.value?.innerHTML || ''
  
  // 清理可能导致显示异常的内容
  const cleanContent = sanitizeEditorContent(content)
  
  // 如果内容发生了清理，尽量避免重设innerHTML（会破坏光标），仅在必须时更新并恢复光标
  if (content !== cleanContent) {
    // 尽量不动DOM，只有在确实出现异常内容时才修正
    if (editorRef.value && /null|undefined|<script|javascript:/i.test(content)) {
      editorRef.value.innerHTML = cleanContent
      
      // 恢复光标位置
      if (savedRange) {
        try {
          selection.removeAllRanges()
          selection.addRange(savedRange)
        } catch (error) {
          // 如果恢复失败，将光标移到末尾
          const range = document.createRange()
          range.selectNodeContents(editorRef.value)
          range.collapse(false)
          selection.removeAllRanges()
          selection.addRange(range)
        }
      }
    }
  }
  
  noteForm.value.content = cleanContent
  startAutoSave()
}

const handleKeydown = (event) => {
  // 如果正在输入法组合输入，不处理按键事件
  if (isComposing.value || event.isComposing) {
    return
  }
  
  // 处理快捷键
  if (event.ctrlKey || event.metaKey) {
    switch (event.key) {
      case 's':
        event.preventDefault()
        saveNote()
        break
      case 'b':
        event.preventDefault()
        formatText('bold')
        break
      case 'i':
        event.preventDefault()
        formatText('italic')
        break
      case 'u':
        event.preventDefault()
        formatText('underline')
        break
    }
  }
  
  // 防止某些特殊字符导致的显示问题
  if (event.key === 'Enter') {
    // 确保回车后的内容正常
    setTimeout(() => {
      if (editorRef.value) {
        const content = editorRef.value.innerHTML
        const cleanContent = sanitizeEditorContent(content)
        
        if (content !== cleanContent) {
          editorRef.value.innerHTML = cleanContent
        }
      }
    }, 0)
  }
}

const handlePaste = (event) => {
  // 处理粘贴事件，使用现代API
  event.preventDefault()
  
  const clipboardData = event.clipboardData || window.clipboardData
  const pastedText = clipboardData.getData('text/plain')
  
  // 清理粘贴的文本
  const cleanText = pastedText
    .replace(/null/g, '') // 移除null文本
    .replace(/undefined/g, '') // 移除undefined文本
    .trim()
  
  if (cleanText) {
    // 使用现代API插入文本
    const selection = window.getSelection()
    if (selection.rangeCount > 0) {
      const range = selection.getRangeAt(0)
      range.deleteContents()
      const textNode = document.createTextNode(cleanText)
      range.insertNode(textNode)
      range.setStartAfter(textNode)
      range.setEndAfter(textNode)
      selection.removeAllRanges()
      selection.addRange(range)
    }
    
    // 触发内容变化事件
    handleContentChange()
  }
}

// 输入法事件处理
const handleCompositionStart = (event) => {
  isComposing.value = true
  compositionText.value = ''
}

const handleCompositionUpdate = (event) => {
  compositionText.value = event.data || ''
}

const handleCompositionEnd = (event) => {
  // 结束组合输入，不手动插入文本，避免与浏览器默认行为重复
  isComposing.value = false
  compositionText.value = ''
  
  // 等待浏览器将组合文本提交到DOM后再统一处理内容
  setTimeout(() => {
    handleContentChange(event)
  }, 0)
}

const formatText = (command) => {
  try {
    document.execCommand(command, false, null)
    if (editorRef.value) {
      const content = editorRef.value.innerHTML
      const cleanContent = sanitizeEditorContent(content)
      if (content !== cleanContent) {
        editorRef.value.innerHTML = cleanContent
      }
    }
    editorRef.value?.focus()
  } catch (error) {
    console.warn('格式化命令执行失败:', error)
  }
}

const isFormatActive = (command) => {
  return document.queryCommandState(command)
}

const insertHeading = (level) => {
  const selection = window.getSelection()
  if (selection.rangeCount > 0) {
    const range = selection.getRangeAt(0)
    const headingId = `heading-${Date.now()}`
    const heading = document.createElement(level)
    heading.id = headingId
    heading.textContent = selection.toString() || `标题 ${level.charAt(1)}`
    
    range.deleteContents()
    range.insertNode(heading)
    
    // 移动光标到标题后
    range.setStartAfter(heading)
    range.collapse(true)
    selection.removeAllRanges()
    selection.addRange(range)
  }
  editorRef.value?.focus()
}

const insertList = (type) => {
  document.execCommand(type === 'ul' ? 'insertUnorderedList' : 'insertOrderedList')
  editorRef.value?.focus()
}

const insertQuote = () => {
  const selection = window.getSelection()
  if (selection.rangeCount > 0) {
    const range = selection.getRangeAt(0)
    const blockquote = document.createElement('blockquote')
    blockquote.textContent = selection.toString() || '引用内容'
    
    range.deleteContents()
    range.insertNode(blockquote)
  }
  editorRef.value?.focus()
}

const insertLink = () => {
  const selection = window.getSelection()
  linkForm.value.text = selection.toString()
  showLinkDialog.value = true
}

const confirmInsertLink = () => {
  if (!linkForm.value.url) {
    ElMessage.warning('请输入链接地址')
    return
  }
  
  const link = `<a href="${linkForm.value.url}" target="_blank">${linkForm.value.text || linkForm.value.url}</a>`
  document.execCommand('insertHTML', false, link)
  
  showLinkDialog.value = false
  linkForm.value = { text: '', url: '' }
  editorRef.value?.focus()
}

const insertImage = () => {
  showImageDialog.value = true
}

const confirmInsertImage = () => {
  if (!imageForm.value.url) {
    ElMessage.warning('请输入图片地址')
    return
  }
  
  const img = `<img src="${imageForm.value.url}" alt="${imageForm.value.alt || ''}" style="max-width: 100%; height: auto;" />`
  document.execCommand('insertHTML', false, img)
  
  showImageDialog.value = false
  imageForm.value = { url: '', alt: '' }
  editorRef.value?.focus()
}

const handleImageSuccess = (response) => {
  // 后端返回通用结构：{ code: "200", msg: string, data: string | { url: string } }
  const ok = response && (response.code === '200' || response.code === 200)
  if (ok) {
    const url = typeof response.data === 'string' ? response.data : (response.data && response.data.url) || ''
    if (url) {
      const img = `<img src="${url}" alt="${imageForm.value.alt || ''}" style="max-width: 100%; height: auto;" />`
      document.execCommand('insertHTML', false, img)
      showImageDialog.value = false
      editorRef.value?.focus()
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error('图片上传失败：响应未包含URL')
    }
  } else {
    ElMessage.error(response?.msg || '图片上传失败')
  }
}

const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const insertTable = () => {
  const table = `
    <table border="1" style="border-collapse: collapse; width: 100%;">
      <tr>
        <th>标题1</th>
        <th>标题2</th>
        <th>标题3</th>
      </tr>
      <tr>
        <td>内容1</td>
        <td>内容2</td>
        <td>内容3</td>
      </tr>
    </table>
  `
  document.execCommand('insertHTML', false, table)
  editorRef.value?.focus()
}

const insertCode = () => {
  const code = '<pre><code>// 在这里输入代码</code></pre>'
  document.execCommand('insertHTML', false, code)
  editorRef.value?.focus()
}

const scrollToHeading = (headingId) => {
  const element = document.getElementById(headingId)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
  }
}

const addTag = (tag) => {
  if (!noteForm.value.tags.find(t => t.id === tag.id)) {
    noteForm.value.tags.push(tag)
    startAutoSave()
  }
}

const removeTag = (tagId) => {
  noteForm.value.tags = noteForm.value.tags.filter(tag => tag.id !== tagId)
  startAutoSave()
}

const createTag = async () => {
  if (!newTag.value.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  
  try {
    const tag = await noteStore.createTag(newTag.value)
    if (tag) {
      ElMessage.success('标签创建成功')
      addTag(tag)
      cancelCreateTag()
    }
  } catch (error) {
    console.error('创建标签失败:', error)
    ElMessage.error('创建标签失败')
  }
}

const cancelCreateTag = () => {
  showCreateTag.value = false
  newTag.value = { name: '', color: '#1890ff' }
}

const saveNote = async () => {
  if (saving.value) return
  
  if (!noteForm.value.title.trim()) {
    ElMessage.warning('请输入笔记标题')
    return
  }
  
  await persistNote({ silent: false })
}

const generateSummary = (content) => {
  // 从内容中生成摘要
  const text = content.replace(/<[^>]*>/g, '').trim()
  return text.length > 200 ? text.substring(0, 200) + '...' : text
}

const loadNote = async () => {
  if (!isEdit.value) return
  
  try {
    const note = await noteStore.fetchNoteById(noteId.value)
    if (note) {
      noteForm.value = {
        title: note.title || '',
        content: note.content || '',
        summary: note.summary || '',
        tags: note.tags || [],
        isPublic: note.isPublic || false
      }
      
      // 设置编辑器内容，确保内容安全
      await nextTick()
      if (editorRef.value) {
        const safeContent = sanitizeEditorContent(note.content || '')
        
        editorRef.value.innerHTML = safeContent
        
        // 将光标放在内容末尾
        const selection = window.getSelection()
        const range = document.createRange()
        range.selectNodeContents(editorRef.value)
        range.collapse(false)
        selection.removeAllRanges()
        selection.addRange(range)
      }
      markSnapshot()
    }
  } catch (error) {
    console.error('加载笔记失败:', error)
    ElMessage.error('加载笔记失败')
    router.back()
  }
}

// 生命周期
onMounted(async () => {
  window.addEventListener('keydown', handleEditorGlobalKeydown)

  // 加载标签数据
  await noteStore.fetchTags()
  
  // 如果是编辑模式，加载笔记数据
  if (isEdit.value) {
    await loadNote()
  }
  
  // 设置编辑器焦点和光标位置
  await nextTick()
  if (editorRef.value) {
    editorRef.value.focus()
    
    // 确保光标在编辑器末尾
    const selection = window.getSelection()
    const range = document.createRange()
    
    // 如果编辑器有内容，将光标移到末尾
    if (editorRef.value.childNodes.length > 0) {
      range.selectNodeContents(editorRef.value)
      range.collapse(false) // 移到末尾
    } else {
      // 如果编辑器为空，创建一个文本节点并将光标放在其中
      const textNode = document.createTextNode('')
      editorRef.value.appendChild(textNode)
      range.setStart(textNode, 0)
      range.setEnd(textNode, 0)
    }
    
    selection.removeAllRanges()
    selection.addRange(range)
  }
  markSnapshot()
})

onUnmounted(() => {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  window.removeEventListener('keydown', handleEditorGlobalKeydown)
  document.body.classList.remove('note-editor-focus')
})

watch(isFocusMode, (value) => {
  document.body.classList.toggle('note-editor-focus', value)
})

// 监听路由变化
watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      loadNote()
    } else {
      // 新建模式，重置表单
      noteForm.value = {
        title: '',
        content: '',
        summary: '',
        tags: [],
        isPublic: false
      }
      if (editorRef.value) {
        editorRef.value.innerHTML = ''
        // 清除可能的异常内容
        editorRef.value.textContent = ''
      }
      markSnapshot()
    }
  }
)
</script>

<style scoped>
.note-editor-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f8fbff 0%, #eef3ff 100%);
}

/* 编辑器头部 */
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #dbe7ff;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 20;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.back-btn {
  color: #6b7280;
  font-size: 14px;
}

.back-btn:hover {
  color: #1890ff;
}

.title-section {
  flex: 1;
  max-width: 600px;
}

.title-input {
  font-size: 24px;
  font-weight: 600;
}

.title-input :deep(.el-input__wrapper) {
  border: none;
  box-shadow: none;
  padding: 8px 0;
  background-color: transparent;
}

.title-input :deep(.el-input__inner) {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.title-input.title-focused :deep(.el-input__wrapper) {
  border-bottom: 2px solid #1890ff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid #dbe7ff;
  background: #f6f9ff;
  color: #567;
  font-size: 12px;
}

.save-status {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.status-text {
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-text.saving {
  color: #1890ff;
}

.status-text.saved {
  color: #52c41a;
}

.status-text.error {
  color: #f5222d;
}

.status-text.saved-time {
  color: #7a869f;
}

.loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.preview-btn {
  border-radius: 8px;
  border-color: #cfdcff;
  background: #f8fbff;
}

.preview-btn.active {
  border-color: #7aa2ff;
  color: #2e63da;
  background: #edf3ff;
}

/* 工具栏 */
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #dbe7ff;
  background: rgba(251, 253, 255, 0.92);
}

.toolbar-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.format-group,
.heading-group,
.list-group,
.insert-group {
  display: flex;
}

.format-group .el-button,
.list-group .el-button,
.insert-group .el-button {
  border-radius: 4px;
  padding: 6px 8px;
}

.format-group .el-button.active {
  background: linear-gradient(135deg, #4f8cff 0%, #2f6ff3 100%);
  color: white;
  border-color: #3f79f6;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 编辑器主体 */
.editor-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.editor-sidebar {
  width: 240px;
  border-right: 1px solid #e2e8f0;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.sidebar-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.outline-content {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
}

.outline-item {
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  color: #6b7280;
  transition: all 0.2s;
}

.outline-item:hover {
  background-color: #e5e7eb;
  color: #374151;
}

.outline-item.level-1 {
  font-weight: 600;
  padding-left: 12px;
}

.outline-item.level-2 {
  padding-left: 24px;
}

.outline-item.level-3 {
  padding-left: 36px;
}

.outline-item.level-4 {
  padding-left: 48px;
}

.editor-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-content.with-sidebar {
  border-left: 1px solid #e2e8f0;
}

.edit-mode,
.preview-mode {
  flex: 1;
  overflow-y: auto;
}

.rich-editor {
  min-height: 100%;
  margin: 24px auto;
  width: min(960px, calc(100% - 48px));
  padding: 32px 36px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e5edff;
  box-shadow: 0 12px 30px rgba(41, 78, 176, 0.12);
  font-size: 16px;
  line-height: 1.75;
  color: #2d3652;
  outline: none;
}

.rich-editor:focus {
  outline: none;
}

.rich-editor h1,
.rich-editor h2,
.rich-editor h3,
.rich-editor h4,
.rich-editor h5,
.rich-editor h6 {
  margin: 24px 0 16px 0;
  font-weight: 600;
  line-height: 1.3;
}

.rich-editor h1 {
  font-size: 32px;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 8px;
}

.rich-editor h2 {
  font-size: 24px;
}

.rich-editor h3 {
  font-size: 20px;
}

.rich-editor h4 {
  font-size: 18px;
}

.rich-editor p {
  margin: 16px 0;
}

.rich-editor blockquote {
  margin: 16px 0;
  padding: 16px;
  background-color: #f8fafc;
  border-left: 4px solid #1890ff;
  color: #6b7280;
  font-style: italic;
}

.rich-editor ul,
.rich-editor ol {
  margin: 16px 0;
  padding-left: 24px;
}

.rich-editor li {
  margin: 8px 0;
}

.rich-editor table {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.rich-editor th,
.rich-editor td {
  border: 1px solid #e2e8f0;
  padding: 8px 12px;
  text-align: left;
}

.rich-editor th {
  background-color: #f8fafc;
  font-weight: 600;
}

.rich-editor pre {
  background: linear-gradient(135deg, #1f2a44 0%, #273a70 50%, #2b4b97 100%);
  border: 1px solid #2a3d7c;
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0;
  overflow-x: auto;
  box-shadow: 0 12px 24px rgba(36, 57, 114, 0.28);
}

.rich-editor code {
  background: linear-gradient(90deg, #ffe3f2, #ffd6ea);
  color: #a80a5a;
  padding: 2px 8px;
  border-radius: 6px;
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 14px;
}

.rich-editor pre code {
  background: transparent;
  color: #e4ecff;
  padding: 0;
}

.rich-editor a {
  color: #1890ff;
  text-decoration: none;
}

.rich-editor a:hover {
  text-decoration: underline;
}

.rich-editor img {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 16px 0;
}

.preview-content {
  margin: 24px auto;
  width: min(960px, calc(100% - 48px));
  padding: 32px 36px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e5edff;
  box-shadow: 0 12px 30px rgba(41, 78, 176, 0.12);
  font-size: 16px;
  line-height: 1.75;
  color: #2d3652;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3),
.preview-content :deep(h4) {
  margin: 22px 0 12px;
  line-height: 1.35;
  color: #1f2d5a;
}

.preview-content :deep(h1) { font-size: 30px; }
.preview-content :deep(h2) { font-size: 24px; }
.preview-content :deep(h3) { font-size: 20px; }

.preview-content :deep(p) {
  margin: 14px 0;
}

.preview-content :deep(a) {
  color: #3478f6;
  text-decoration: none;
  border-bottom: 1px solid rgba(52, 120, 246, 0.35);
}

.preview-content :deep(blockquote) {
  margin: 16px 0;
  padding: 10px 14px;
  border-left: 4px solid #8aa8ff;
  background: linear-gradient(90deg, rgba(83, 131, 255, 0.1), rgba(83, 131, 255, 0.02));
  color: #455075;
  border-radius: 8px;
}

.preview-content :deep(ul),
.preview-content :deep(ol) {
  margin: 14px 0;
  padding-left: 24px;
}

.preview-content :deep(li) {
  margin: 6px 0;
}

.preview-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border-radius: 10px;
  overflow: hidden;
}

.preview-content :deep(th),
.preview-content :deep(td) {
  border: 1px solid #e7ecff;
  padding: 10px 12px;
  text-align: left;
}

.preview-content :deep(th) {
  background: #f4f7ff;
}

.preview-content :deep(pre) {
  margin: 16px 0;
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid #2a3d7c;
  background: linear-gradient(135deg, #1f2a44 0%, #273a70 50%, #2b4b97 100%);
  color: #e4ecff;
  overflow-x: auto;
  box-shadow: 0 12px 24px rgba(36, 57, 114, 0.28);
}

.preview-content :deep(pre code) {
  font-size: 14px;
  line-height: 1.65;
  font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
  background: transparent;
  color: inherit;
  padding: 0;
}

.preview-content :deep(code) {
  background: linear-gradient(90deg, #ffe3f2, #ffd6ea);
  color: #a80a5a;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 0.92em;
  font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
}

.preview-content :deep(hr) {
  border: none;
  border-top: 1px solid #e6ebf9;
  margin: 22px 0;
}

.preview-content :deep(img) {
  max-width: 100%;
  border-radius: 10px;
}

.preview-content :deep(.empty-content) {
  color: #8c95b2;
}

/* 标签管理弹窗 */
.tag-manager-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.tag-manager-content h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.current-tags {
  margin-bottom: 24px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  align-items: center;
}

.note-tag {
  border: none;
  font-size: 12px;
}

.add-tag-hint {
  color: #9ca3af;
  font-size: 12px;
}

.available-tags {
  margin-bottom: 24px;
}

.tags-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tag-search {
  margin-bottom: 12px;
}

.tag-list.available {
  max-height: 200px;
  overflow-y: auto;
}

.available-tag {
  cursor: pointer;
  border: none;
  font-size: 12px;
  transition: all 0.2s;
}

.available-tag:hover {
  transform: scale(1.05);
}

.create-tag-section h4 {
  margin-bottom: 16px;
}

.create-tag-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.color-picker {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-label {
  font-size: 14px;
  color: #374151;
}

.color-options {
  display: flex;
  gap: 8px;
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.active {
  border-color: #1890ff;
  transform: scale(1.1);
}

.create-tag-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 图片上传 */
.image-uploader {
  display: flex;
  justify-content: center;
  padding: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .editor-header {
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }
  
  .header-left,
  .header-right {
    width: 100%;
  }
  
  .header-right {
    justify-content: flex-end;
  }

  .doc-stats {
    display: none;
  }
  
  .editor-toolbar {
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px 16px;
  }
  
  .toolbar-section {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .editor-sidebar {
    display: none;
  }
  
  .rich-editor {
    width: calc(100% - 24px);
    margin: 12px auto;
    padding: 20px 16px;
  }
  
  .preview-content {
    width: calc(100% - 24px);
    margin: 12px auto;
    padding: 20px 16px;
  }
}

/* writing studio redesign */
:global(body.note-editor-focus) {
  overflow: hidden;
}

:global(body.note-editor-focus .plnm-rail),
:global(body.note-editor-focus .workspace-topbar),
:global(body.note-editor-focus .back-to-top) {
  display: none !important;
}

:global(body.note-editor-focus .plnm-shell) {
  grid-template-columns: minmax(0, 1fr) !important;
  height: 100vh;
  overflow: hidden;
  background: #f3f7fb;
}

:global(body.note-editor-focus .plnm-main) {
  grid-template-rows: minmax(0, 1fr) !important;
  min-height: 100vh;
}

:global(body.note-editor-focus .workspace-content) {
  height: 100vh;
  padding: 0 !important;
  overflow: hidden !important;
}

:global(body.note-editor-focus .content-canvas) {
  width: 100% !important;
  max-width: none !important;
  height: 100% !important;
  padding: 0 !important;
}

.note-editor-container {
  --editor-bg: #edf3f8;
  --editor-surface: #ffffff;
  --editor-soft: #f7fafc;
  --editor-ink: #172033;
  --editor-muted: #66758b;
  --editor-line: #d7e1ec;
  --editor-primary: #1f6feb;
  height: calc(100vh - 112px);
  min-height: 680px;
  overflow: hidden;
  border: 1px solid var(--editor-line);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(31, 111, 235, 0.08), rgba(22, 163, 74, 0.06)),
    var(--editor-bg);
  color: var(--editor-ink);
}

.note-editor-container.is-focus-mode {
  position: fixed;
  inset: 0;
  z-index: 1000;
  height: 100vh;
  min-height: 0;
  border: 0;
  border-radius: 0;
  background: #f5f8fb;
}

.editor-commandbar {
  min-height: 72px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--editor-line);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.editor-title-meta {
  display: grid;
  gap: 2px;
}

.editor-title-meta .eyebrow {
  color: #16845a;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
}

.editor-title-meta strong {
  color: var(--editor-ink);
  font-size: 17px;
  line-height: 1.2;
}

.editor-commandbar .header-right {
  gap: 8px;
}

.editor-commandbar .el-button {
  border-radius: 8px;
  font-weight: 800;
}

.doc-stats {
  min-width: 110px;
  border-radius: 8px;
  border-color: var(--editor-line);
  background: var(--editor-soft);
  color: var(--editor-muted);
}

.save-status {
  min-width: 86px;
  min-height: 36px;
  justify-content: center;
  border: 1px solid var(--editor-line);
  border-radius: 8px;
  background: #ffffff;
  font-size: 13px;
  font-weight: 800;
}

.utility-btn.active,
.preview-btn.active,
.focus-btn:hover {
  border-color: #bed6ff;
  color: #1557c0;
  background: #e9f2ff;
}

.save-btn {
  background: var(--editor-primary);
  border-color: var(--editor-primary);
}

.focus-exit-button {
  position: fixed;
  top: 18px;
  right: 22px;
  z-index: 1010;
  height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--editor-line);
  border-radius: 8px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.94);
  color: var(--editor-ink);
  box-shadow: 0 14px 36px rgba(33, 54, 86, 0.12);
  cursor: pointer;
  font-weight: 850;
}

.editor-toolbar {
  min-height: 52px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--editor-line);
  background: rgba(248, 251, 255, 0.95);
  overflow-x: auto;
}

.editor-inline-toolbar {
  position: static !important;
  top: auto !important;
  z-index: 4;
  width: 100%;
  min-height: 50px;
  border-width: 0 0 1px !important;
  border-radius: 0 !important;
  padding: 9px 34px !important;
  background: #fbfdff !important;
  box-shadow: none !important;
}

.toolbar-section {
  gap: 8px;
}

.editor-toolbar .el-divider--vertical {
  height: 24px;
  border-left-color: var(--editor-line);
}

.editor-toolbar .el-button {
  border-radius: 8px;
  font-weight: 800;
}

.format-group .el-button,
.list-group .el-button,
.insert-group .el-button {
  min-width: 34px;
  padding: 6px 9px;
}

.format-group .el-button em {
  font-style: italic;
  font-weight: 900;
}

.format-group .el-button.active {
  background: var(--editor-primary);
  border-color: var(--editor-primary);
  color: #ffffff;
}

.editor-body {
  min-height: 0;
  flex: 1;
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
  overflow: hidden;
}

.editor-body.outline-open {
  grid-template-columns: 232px minmax(0, 1fr) 284px;
}

.note-editor-container.is-focus-mode .editor-body {
  height: 100vh;
  grid-template-columns: minmax(0, 1fr);
  padding: 0;
}

.editor-side-panel,
.editor-sidebar {
  min-width: 0;
  width: auto;
  border: 1px solid var(--editor-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 30px rgba(33, 54, 86, 0.05);
  overflow: hidden;
}

.editor-side-panel {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 10px;
}

.side-section {
  display: grid;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #e4ebf3;
}

.side-section:last-child {
  border-bottom: 0;
}

.side-section header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--editor-muted);
  font-size: 13px;
  font-weight: 850;
}

.side-section header button {
  margin-left: auto;
  border: 0;
  background: transparent;
  color: var(--editor-primary);
  cursor: pointer;
  font-weight: 850;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.metric-list div {
  display: grid;
  gap: 4px;
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  padding: 10px;
  background: var(--editor-soft);
}

.metric-list strong {
  color: var(--editor-ink);
  font-size: 24px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.metric-list span {
  color: var(--editor-muted);
  font-size: 12px;
}

.side-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.empty-side-action {
  min-height: 36px;
  border: 1px dashed #bed6ff;
  border-radius: 8px;
  background: #f4f9ff;
  color: var(--editor-primary);
  cursor: pointer;
  font-weight: 850;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.quick-grid button {
  min-height: 34px;
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  background: #ffffff;
  color: var(--editor-ink);
  cursor: pointer;
  font-weight: 850;
}

.quick-grid button:hover {
  border-color: #bed6ff;
  background: #e9f2ff;
}

.editor-content {
  min-width: 0;
  min-height: 0;
  display: block;
  overflow: hidden;
}

.editor-content.with-sidebar {
  border-left: 0;
}

.writing-page {
  height: 100%;
  overflow-y: auto;
  border: 1px solid var(--editor-line);
  border-radius: 8px;
  background: var(--editor-surface);
  box-shadow: 0 16px 40px rgba(33, 54, 86, 0.07);
}

.note-editor-container.is-focus-mode .writing-page {
  width: min(980px, 100%);
  margin: 0 auto;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.writing-page-head {
  position: sticky;
  top: 0;
  z-index: 5;
  display: grid;
  gap: 8px;
  padding: 22px 34px 14px;
  border-bottom: 1px solid #edf2f7;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
}

.mode-chip {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #cfe0ff;
  border-radius: 999px;
  padding: 4px 10px;
  background: #e9f2ff;
  color: #1557c0;
  font-size: 12px;
  font-weight: 900;
}

.title-section {
  max-width: none;
}

.title-input {
  width: 100%;
}

.title-input :deep(.el-input__wrapper) {
  min-height: 52px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
  background: transparent;
}

.title-input :deep(.el-input__inner) {
  height: auto;
  color: var(--editor-ink);
  font-size: 34px;
  font-weight: 900;
  line-height: 1.2;
}

.title-input.title-focused :deep(.el-input__wrapper) {
  border-bottom: 0;
}

.editor-tag-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.note-editor-container.is-focus-mode .writing-page-head {
  padding-top: 52px;
}

.note-editor-container.is-focus-mode .mode-chip,
.note-editor-container.is-focus-mode .editor-tag-strip {
  display: none;
}

.edit-mode,
.preview-mode {
  display: block;
  min-height: 0;
  overflow: visible;
}

.rich-editor,
.preview-content {
  width: min(860px, calc(100% - 68px));
  min-height: 520px;
  margin: 0 auto;
  padding: 30px 0 72px;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  color: #223047;
  font-size: 17px;
  line-height: 1.82;
}

.note-editor-container.is-focus-mode .rich-editor,
.note-editor-container.is-focus-mode .preview-content {
  width: min(860px, calc(100% - 84px));
  min-height: calc(100vh - 160px);
}

.rich-editor:empty::before {
  content: '开始写下正文...';
  color: #9aa8ba;
}

.rich-editor h1,
.rich-editor h2,
.rich-editor h3,
.rich-editor h4,
.rich-editor h5,
.rich-editor h6,
.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3),
.preview-content :deep(h4) {
  color: var(--editor-ink);
  font-weight: 900;
  letter-spacing: 0;
}

.rich-editor h1,
.preview-content :deep(h1) {
  border-bottom: 1px solid #e4ebf3;
  padding-bottom: 10px;
  font-size: 30px;
}

.rich-editor blockquote,
.preview-content :deep(blockquote) {
  border-left: 4px solid var(--editor-primary);
  border-radius: 8px;
  background: #f4f9ff;
  color: #526276;
}

.rich-editor pre,
.preview-content :deep(pre) {
  border: 1px solid #172033;
  border-radius: 8px;
  background: #0f172a;
  box-shadow: none;
}

.rich-editor code,
.preview-content :deep(code) {
  border-radius: 6px;
  background: #eef6ff;
  color: #1557c0;
}

.rich-editor pre code,
.preview-content :deep(pre code) {
  background: transparent;
  color: #e2e8f0;
}

.rich-editor table,
.preview-content :deep(table) {
  border-radius: 8px;
  overflow: hidden;
}

.editor-sidebar {
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  min-height: 52px;
  padding: 0 12px;
  border-bottom: 1px solid #e4ebf3;
}

.sidebar-header h4 {
  color: var(--editor-ink);
  font-size: 14px;
  font-weight: 900;
}

.outline-content {
  display: grid;
  gap: 4px;
  padding: 10px;
}

.outline-item {
  width: 100%;
  min-height: 34px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #526276;
  cursor: pointer;
  text-align: left;
  font-size: 13px;
  font-weight: 750;
}

.outline-item:hover {
  background: #e9f2ff;
  color: var(--editor-primary);
}

.outline-empty {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 180px;
  color: #8a97aa;
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 1200px) {
  .editor-body,
  .editor-body.outline-open {
    grid-template-columns: minmax(0, 1fr);
  }

  .editor-side-panel,
  .editor-sidebar {
    display: none !important;
  }
}
</style>
