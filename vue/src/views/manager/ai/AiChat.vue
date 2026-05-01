<template>
  <div
    :class="[
      'ai-chat-page',
      {
        'sessions-collapsed': isSessionPanelCollapsed,
        'context-collapsed': isContextPanelCollapsed
      }
    ]"
  >
    <aside :class="['chat-sidebar', { collapsed: isSessionPanelCollapsed }]">
      <div class="sidebar-head">
        <template v-if="!isSessionPanelCollapsed">
          <el-button type="primary" class="new-chat-btn" @click="startNewConversation">
            <el-icon><Plus /></el-icon>
            新对话
          </el-button>
          <el-tooltip content="收起对话列表" placement="right">
            <button class="panel-icon-btn" type="button" @click="isSessionPanelCollapsed = true">
              <el-icon><Fold /></el-icon>
            </button>
          </el-tooltip>
        </template>
        <template v-else>
          <el-tooltip content="展开对话列表" placement="right">
            <button class="panel-icon-btn is-large" type="button" @click="isSessionPanelCollapsed = false">
              <el-icon><Expand /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="新对话" placement="right">
            <button class="panel-icon-btn is-large is-primary" type="button" @click="startNewConversation">
              <el-icon><Plus /></el-icon>
            </button>
          </el-tooltip>
        </template>
      </div>
      <div v-if="!isSessionPanelCollapsed" class="session-list">
        <div
          v-for="session in activeRoleSessions"
          :key="session.id"
          :class="['session-item', { active: session.id === activeConversationId }]"
          @click="switchConversation(session.id)"
        >
          <div class="session-main">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">{{ formatConversationTime(session.updatedAt) }}</div>
          </div>
          <div class="session-actions">
            <el-button text class="session-delete" @click.stop="removeConversation(session.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      <div v-else class="collapsed-panel-summary">
        <span>{{ activeRoleSessions.length }}</span>
        <small>对话</small>
      </div>
    </aside>

    <section class="chat-main">
      <header class="chat-topbar">
        <div class="topbar-left">
          <el-button class="mobile-session-btn" circle @click="mobileSessionDrawer = true">
            <el-icon><Operation /></el-icon>
          </el-button>
          <div class="bot-meta">
            <div class="bot-name">{{ activeRoleName }}</div>
            <div class="bot-subtitle">{{ activeRoleDesc }}</div>
          </div>
        </div>
        <div class="topbar-actions">
          <el-tooltip :content="isSessionPanelCollapsed ? '展开对话列表' : '收起对话列表'" placement="bottom">
            <el-button text @click="isSessionPanelCollapsed = !isSessionPanelCollapsed">
              <el-icon><component :is="isSessionPanelCollapsed ? Expand : Fold" /></el-icon>
              历史
            </el-button>
          </el-tooltip>
          <el-tooltip :content="isContextPanelCollapsed ? '展开上下文面板' : '收起上下文面板'" placement="bottom">
            <el-button text @click="isContextPanelCollapsed = !isContextPanelCollapsed">
              <el-icon><component :is="isContextPanelCollapsed ? Expand : Fold" /></el-icon>
              上下文
            </el-button>
          </el-tooltip>
          <el-button text @click="settingsVisible = true">
            <el-icon><Setting /></el-icon>
            设置
          </el-button>
          <el-button text @click="goAudit">
            <el-icon><DataAnalysis /></el-icon>
            审计
          </el-button>
          <el-button text @click="exportConversation">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
          <el-button text @click="startNewConversation">
            <el-icon><RefreshRight /></el-icon>
            新对话
          </el-button>
          <el-button text @click="clearCurrentConversation">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
        </div>
      </header>

      <div class="role-select-wrap">
        <div class="role-select-card">
          <span class="role-label">当前角色</span>
          <el-select
            class="role-select"
            :model-value="activeRoleId"
            :teleported="false"
            @change="switchRole"
          >
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            >
              <div class="role-option">
                <span>{{ role.name }}</span>
                <small>{{ role.streaming ? '流式' : '非流式' }} · {{ role.description }}</small>
              </div>
            </el-option>
          </el-select>
        </div>
        <div class="conversation-tools">
          <span>{{ activeMessages.length }} 条消息</span>
          <span>{{ conversationWordCount }} 字</span>
          <span>{{ activeRoleStreaming ? (useWebSocket ? 'WebSocket' : 'SSE') : '非流式' }}</span>
        </div>
      </div>

      <div ref="messageScrollRef" class="messages-panel">
        <div class="messages-inner">
          <div
            v-for="msg in activeMessages"
            :key="msg.id"
            :class="['message-row', msg.role === 'user' ? 'row-user' : 'row-assistant']"
          >
            <img v-if="msg.role !== 'user'" class="msg-avatar" :src="assistantAvatar" alt="assistant-avatar" />
            <div class="bubble-wrap">
              <div :class="['msg-bubble', msg.role === 'user' ? 'bubble-user' : 'bubble-assistant']">
                <template v-if="msg.loading">
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                </template>
                <template v-else-if="msg.role === 'assistant'">
                  <div class="ai-markdown" v-html="renderAssistantMessage(msg.text)"></div>
                </template>
                <template v-else>
                  <div class="user-text">{{ msg.text }}</div>
                </template>
              </div>
              <div class="msg-time">{{ formatTime(msg.time) }}</div>
              <div v-if="msg.role === 'assistant' && !msg.loading" class="msg-actions">
                <el-button text size="small" class="msg-action-btn" @click="copyMessage(msg)">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
                <el-button text size="small" class="msg-action-btn" @click="saveMessageToNote(msg)">
                  <el-icon><DocumentAdd /></el-icon>
                  转笔记
                </el-button>
              </div>
              <div v-if="msg.citations?.length" class="citation-list">
                <span v-for="c in msg.citations" :key="`${c.type}-${c.id}`" class="citation-tag">
                  {{ c.title || c.id }}
                </span>
              </div>
            </div>
            <img v-if="msg.role === 'user'" class="msg-avatar" :src="userAvatar" alt="user-avatar" />
          </div>
        </div>
      </div>

      <footer class="chat-input-panel">
        <div class="input-toolbar">
          <el-button text @click="toggleVoiceInput">
            <el-icon><Microphone v-if="!recognizing" /><Loading v-else /></el-icon>
            {{ recognizing ? '语音识别中' : '语音输入' }}
          </el-button>
          <el-button text @click="scrollToBottom(true)">
            <el-icon><RefreshRight /></el-icon>
            到底部
          </el-button>
          <el-button v-if="sending" text class="danger-tool" @click="stopGeneration">
            <el-icon><Delete /></el-icon>
            停止生成
          </el-button>
          <el-switch
            v-model="useWebSocket"
            inline-prompt
            active-text="WebSocket"
            inactive-text="SSE"
            :disabled="!activeRoleStreaming"
          />
          <el-switch
            v-model="allowCrossRoleReference"
            inline-prompt
            active-text="跨角色引用"
            inactive-text="隔离上下文"
          />
          <el-switch
            v-model="autoScroll"
            inline-prompt
            active-text="自动滚动"
            inactive-text="手动滚动"
          />
          <span class="stream-badge">{{ activeRoleStreaming ? (useWebSocket ? '流式通道：WebSocket' : '流式通道：SSE') : '当前角色：非流式回答' }}</span>
        </div>
        <div class="input-row">
          <el-input
            v-model="draft"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            resize="none"
            placeholder="输入内容，Enter发送，Shift+Enter换行"
            @keydown="handleInputKeydown"
          />
          <el-button type="primary" class="send-btn" :loading="sending" @click="sendMessage">
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </footer>
    </section>

    <aside :class="['context-panel', { collapsed: isContextPanelCollapsed }]">
      <div v-if="isContextPanelCollapsed" class="context-collapsed-tools">
        <el-tooltip content="展开上下文面板" placement="left">
          <button class="panel-icon-btn is-large" type="button" @click="isContextPanelCollapsed = false">
            <el-icon><Expand /></el-icon>
          </button>
        </el-tooltip>
        <span>上下文</span>
      </div>
      <template v-else>
      <div class="context-card">
        <div class="context-card-head">
          <span>上下文连接</span>
          <div class="context-head-actions">
            <strong>{{ allowCrossRoleReference ? '开启' : '隔离' }}</strong>
            <button class="panel-icon-btn" type="button" @click="isContextPanelCollapsed = true">
              <el-icon><Fold /></el-icon>
            </button>
          </div>
        </div>
        <div class="context-map">
          <div class="context-row">
            <span>笔记库</span>
            <b>摘要 / 标签 / 复盘</b>
          </div>
          <div class="context-row">
            <span>生活中心</span>
            <b>账单 / 预算 / 分类</b>
          </div>
          <div class="context-row">
            <span>学习中心</span>
            <b>课程 / 进度 / 资源</b>
          </div>
        </div>
      </div>

      <div class="context-card">
        <div class="context-card-head">
          <span>执行策略</span>
          <strong>{{ activeRoleStreaming ? '流式' : '非流式' }}</strong>
        </div>
        <p>AI 回答会保留引用和审计记录。涉及跨模块数据时，先解释判断，再给出可执行结果。</p>
      </div>

      <div class="context-card prompt-card">
        <div class="context-card-head">
          <span>快捷提示</span>
          <strong>3 条</strong>
        </div>
        <button type="button" @click="draft = '总结我最近的笔记，并列出三个最值得复盘的主题。'">总结最近笔记</button>
        <button type="button" @click="draft = '分析本月消费结构，告诉我哪些分类接近预算阈值。'">分析预算压力</button>
        <button type="button" @click="draft = '把 Vue 学习进度整理成下周计划，按优先级输出。'">生成学习计划</button>
      </div>
      </template>
    </aside>

    <el-drawer v-model="mobileSessionDrawer" direction="ltr" size="280px" title="对话列表">
      <el-button type="primary" class="drawer-new-btn" @click="startNewConversation">
        <el-icon><Plus /></el-icon>
        新对话
      </el-button>
      <div class="drawer-session-list">
        <div
          v-for="session in activeRoleSessions"
          :key="session.id"
          :class="['session-item', { active: session.id === activeConversationId }]"
          @click="switchConversation(session.id)"
        >
          <div class="session-main">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">{{ formatConversationTime(session.updatedAt) }}</div>
          </div>
          <div class="session-actions">
            <el-button text class="session-delete" @click.stop="removeConversation(session.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="settingsVisible" title="对话设置" width="460px">
      <el-form label-width="90px">
        <el-form-item label="模型">
          <el-select v-model="chatSettings.model" style="width: 100%">
            <el-option label="deepseek-v4-pro" value="deepseek-v4-pro" />
          </el-select>
        </el-form-item>
        <el-form-item label="系统指令">
          <el-input v-model="chatSettings.systemPrompt" type="textarea" :rows="3" resize="none" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Operation, Setting, Download, RefreshRight, Microphone, Loading, Promotion, DataAnalysis, CopyDocument, DocumentAdd, Delete, Fold, Expand } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'
import { aiApi } from '@/api'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import { useNoteStore } from '@/stores/note'

const LOCAL_CONVERSATION_KEY = 'ai-chat-conversations-role-v2'
const LOCAL_SETTINGS_KEY = 'ai-chat-settings-v2'
const DEFAULT_CHAT_MODEL = 'deepseek-v4-pro'
const assistantAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64"%3E%3Crect width="64" height="64" rx="16" fill="%230f172a"/%3E%3Ctext x="32" y="39" text-anchor="middle" font-family="Arial" font-size="18" font-weight="700" fill="white"%3EAI%3C/text%3E%3C/svg%3E'

const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)
const userAvatar = computed(() => userInfo.value?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')
const storageUserKey = computed(() => {
  const user = userInfo.value || {}
  const identity = user.id ?? user.username
  return identity ? String(identity) : ''
})
const conversationStorageKey = computed(() => storageUserKey.value ? `${LOCAL_CONVERSATION_KEY}:${storageUserKey.value}` : '')
const settingsStorageKey = computed(() => storageUserKey.value ? `${LOCAL_SETTINGS_KEY}:${storageUserKey.value}` : '')
const router = useRouter()
const noteStore = useNoteStore()

const roles = ref([])
const activeRoleId = ref('system_operator')
const roleConversations = ref({})
const roleActiveConversationId = ref({})
const messageScrollRef = ref(null)
const draft = ref('')
const sending = ref(false)
const recognizing = ref(false)
const mobileSessionDrawer = ref(false)
const settingsVisible = ref(false)
const isSessionPanelCollapsed = ref(false)
const isContextPanelCollapsed = ref(false)
const allowCrossRoleReference = ref(false)
const useWebSocket = ref(false)
const autoScroll = ref(true)
const stopRequested = ref(false)
const chatSettings = ref({
  model: DEFAULT_CHAT_MODEL,
  systemPrompt: ''
})
const wsPreferredRoles = ['note_assistant', 'life_hub_butler', 'learning_center_mentor', 'creative_partner']

const activeRole = computed(() => roles.value.find(r => r.id === activeRoleId.value) || null)
const activeRoleName = computed(() => activeRole.value?.name || 'AI助手')
const activeRoleDesc = computed(() => activeRole.value?.description || chatSettings.value.model)
const activeRoleStreaming = computed(() => !!activeRole.value?.streaming)
const activeRoleSessions = computed(() => roleConversations.value[activeRoleId.value] || [])
const activeConversationId = computed(() => roleActiveConversationId.value[activeRoleId.value] || '')
const activeConversation = computed(() => activeRoleSessions.value.find(c => c.id === activeConversationId.value) || null)
const activeMessages = computed(() => activeConversation.value?.messages || [])
const conversationWordCount = computed(() => activeMessages.value.reduce((sum, msg) => sum + String(msg.text || '').length, 0))

let speechRecognition = null
let activeAbortController = null
let activeSocket = null

const buildMessage = (role, text, loading = false, citations = null) => ({
  id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
  role,
  text,
  loading,
  citations: citations || [],
  time: Date.now()
})

const buildConversation = (greeting = '你好，我是你的 AI 助手。') => {
  const now = Date.now()
  return {
    id: `chat-${now}-${Math.random().toString(16).slice(2, 8)}`,
    title: '新对话',
    createdAt: now,
    updatedAt: now,
    messages: [buildMessage('assistant', greeting)]
  }
}

const ensureRoleContext = (roleId) => {
  if (!roleConversations.value[roleId]) {
    const first = buildConversation(`你好，我是${roles.value.find(r => r.id === roleId)?.name || 'AI助手'}。`)
    roleConversations.value[roleId] = [first]
    roleActiveConversationId.value[roleId] = first.id
  }
}

const saveState = () => {
  if (!conversationStorageKey.value) return
  localStorage.setItem(conversationStorageKey.value, JSON.stringify({
    roleConversations: roleConversations.value,
    roleActiveConversationId: roleActiveConversationId.value,
    activeRoleId: activeRoleId.value
  }))
}

const saveSettings = () => {
  if (settingsStorageKey.value) {
    localStorage.setItem(settingsStorageKey.value, JSON.stringify(chatSettings.value))
  }
  settingsVisible.value = false
  ElMessage.success('设置已保存')
}

const loadSettings = () => {
  chatSettings.value = { model: DEFAULT_CHAT_MODEL, systemPrompt: '' }
  if (!settingsStorageKey.value) return
  const raw = localStorage.getItem(settingsStorageKey.value)
  if (!raw) return
  try {
    const parsed = JSON.parse(raw)
    chatSettings.value = {
      model: DEFAULT_CHAT_MODEL,
      systemPrompt: parsed.systemPrompt || ''
    }
  } catch {
    chatSettings.value = { model: DEFAULT_CHAT_MODEL, systemPrompt: '' }
  }
}

const loadRoles = async () => {
  try {
    const res = await aiApi.getRoles()
    if ((res.code === 200 || res.code === '200') && Array.isArray(res.data) && res.data.length > 0) {
      roles.value = res.data
    } else {
      roles.value = fallbackRoles()
    }
  } catch {
    roles.value = fallbackRoles()
  }
  roles.value.forEach(role => ensureRoleContext(role.id))
  syncStreamTransport(activeRoleId.value)
}

const fallbackRoles = () => ([
  { id: 'system_operator', name: '系统操作员', streaming: false, description: '执行指令型角色' },
  { id: 'note_assistant', name: '笔记助手', streaming: true, description: '基于笔记库回答' },
  { id: 'life_hub_butler', name: '生活中心管家', streaming: true, description: '基于生活数据建议' },
  { id: 'learning_center_mentor', name: '学习中心导师', streaming: true, description: '基于学习数据规划' },
  { id: 'creative_partner', name: '创作搭档', streaming: true, description: '高活跃创意对话' }
])

const loadConversationState = () => {
  roleConversations.value = {}
  roleActiveConversationId.value = {}
  activeRoleId.value = 'system_operator'
  if (!conversationStorageKey.value) return
  const raw = localStorage.getItem(conversationStorageKey.value)
  if (!raw) return
  try {
    const parsed = JSON.parse(raw)
    roleConversations.value = parsed.roleConversations || {}
    roleActiveConversationId.value = parsed.roleActiveConversationId || {}
    if (parsed.activeRoleId) {
      activeRoleId.value = parsed.activeRoleId
    }
  } catch {
    roleConversations.value = {}
    roleActiveConversationId.value = {}
  }
}

const sortSessions = (roleId) => {
  const sessions = roleConversations.value[roleId] || []
  sessions.sort((a, b) => b.updatedAt - a.updatedAt)
}

const switchRole = (roleId) => {
  const exists = roles.value.some(r => r.id === roleId)
  if (!exists) {
    ElMessage.warning('角色连接失败，已保持当前角色')
    return
  }
  ensureRoleContext(roleId)
  activeRoleId.value = roleId
  syncStreamTransport(roleId)
  nextTick(scrollToBottom)
}

const switchConversation = (id) => {
  roleActiveConversationId.value[activeRoleId.value] = id
  mobileSessionDrawer.value = false
  nextTick(scrollToBottom)
}

const startNewConversation = () => {
  const roleId = activeRoleId.value
  ensureRoleContext(roleId)
  const greeting = `你好，我是${activeRoleName.value}。`
  const session = buildConversation(greeting)
  roleConversations.value[roleId].unshift(session)
  roleActiveConversationId.value[roleId] = session.id
  mobileSessionDrawer.value = false
  nextTick(scrollToBottom)
}

const removeConversation = async (sessionId) => {
  const roleId = activeRoleId.value
  const sessions = roleConversations.value[roleId] || []
  const idx = sessions.findIndex(i => i.id === sessionId)
  if (idx < 0) return
  try {
    await ElMessageBox.confirm('确认删除这条历史对话吗？删除后无法恢复。', '删除对话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  sessions.splice(idx, 1)
  if (sessions.length === 0) {
    const fresh = buildConversation(`你好，我是${activeRoleName.value}。`)
    roleConversations.value[roleId] = [fresh]
    roleActiveConversationId.value[roleId] = fresh.id
  } else if (activeConversationId.value === sessionId) {
    roleActiveConversationId.value[roleId] = sessions[Math.max(0, idx - 1)].id
  }
  ElMessage.success('对话已删除')
  nextTick(scrollToBottom)
}

const clearCurrentConversation = async () => {
  const session = activeConversation.value
  if (!session) return
  try {
    await ElMessageBox.confirm('确认清空当前对话内容吗？', '清空对话', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  session.messages = [buildMessage('assistant', `你好，我是${activeRoleName.value}。`)]
  session.updatedAt = Date.now()
  ElMessage.success('当前对话已清空')
  nextTick(() => scrollToBottom(true))
}

const updateConversationTitle = (session, text) => {
  if (!session) return
  const userCount = session.messages.filter(m => m.role === 'user').length
  if (userCount <= 1) {
    session.title = text.length > 18 ? `${text.slice(0, 18)}...` : text
  }
}

const replaceMessage = (session, messageId, updater) => {
  const idx = session.messages.findIndex(i => i.id === messageId)
  if (idx < 0) return
  session.messages[idx] = { ...session.messages[idx], ...updater }
}

const streamTextToMessage = async (session, msgId, fullText, citations) => {
  const chunks = fullText.split('')
  let current = ''
  for (let i = 0; i < chunks.length; i += 1) {
    current += chunks[i]
    replaceMessage(session, msgId, { text: current, loading: false, citations, time: Date.now() })
    if (i % 12 === 0) {
      await new Promise(resolve => setTimeout(resolve, 12))
      scrollToBottom()
    }
  }
}

const buildCrossRoleContext = () => {
  if (!allowCrossRoleReference.value) return ''
  const lines = []
  Object.keys(roleConversations.value).forEach(roleId => {
    if (roleId === activeRoleId.value) return
    const sessions = roleConversations.value[roleId] || []
    if (!sessions.length) return
    const latest = sessions[0]
    const latestMsg = [...latest.messages].reverse().find(i => i.role === 'assistant' && i.text)
    if (latestMsg) {
      lines.push(`角色[${roleId}]最近结论：${latestMsg.text.slice(0, 120)}`)
    }
  })
  return lines.join('\n')
}

const sanitizeHtml = (content = '') => {
  return String(content)
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
}

const toMarkdownHtml = (content = '') => {
  if (!content) return ''
  try {
    return sanitizeHtml(marked.parse(content, { gfm: true, breaks: true }))
  } catch {
    return sanitizeHtml(content)
  }
}

const extractSection = (text = '', label) => {
  const regex = new RegExp(`${label}[:：]\\s*([\\s\\S]*?)(?=\\n\\s*(思考过程|结论)[:：]|$)`, 'i')
  const match = text.match(regex)
  return match ? match[1].trim() : ''
}

const stripSection = (text = '', label) => {
  const regex = new RegExp(`${label}[:：][\\s\\S]*?(?=\\n\\s*(思考过程|结论)[:：]|$)`, 'ig')
  return text.replace(regex, '')
}

const renderAssistantMessage = (text = '') => {
  const content = String(text || '')
  const thinking = extractSection(content, '思考过程')
  const conclusion = extractSection(content, '结论')
  let main = stripSection(stripSection(content, '思考过程'), '结论').trim()
  if (!main && (thinking || conclusion)) {
    main = ''
  }
  const blocks = []
  if (main) {
    blocks.push(`<div class="ai-main">${toMarkdownHtml(main)}</div>`)
  }
  if (thinking) {
    blocks.push(`<div class="ai-section"><div class="ai-section-title">思考过程</div><div class="ai-section-body">${toMarkdownHtml(thinking)}</div></div>`)
  }
  if (conclusion) {
    blocks.push(`<div class="ai-section highlight"><div class="ai-section-title">结论</div><div class="ai-section-body">${toMarkdownHtml(conclusion)}</div></div>`)
  }
  if (!blocks.length) {
    return toMarkdownHtml(content)
  }
  return `<div class="ai-sections">${blocks.join('')}</div>`
}

const copyMessage = async (msg) => {
  const text = msg?.text || ''
  if (!text) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
      ElMessage.success('已复制到剪贴板')
    } catch {
      ElMessage.error('复制失败')
    } finally {
      document.body.removeChild(textarea)
    }
  }
}

const buildNoteTitle = (text = '') => {
  const firstLine = String(text).split('\n').find(i => i.trim())
  const title = (firstLine || 'AI 笔记').trim()
  return title.length > 30 ? `${title.slice(0, 30)}...` : title
}

const buildNoteSummary = (text = '') => {
  const clean = String(text).replace(/\s+/g, ' ').trim()
  return clean.length > 120 ? `${clean.slice(0, 120)}...` : clean
}

const saveMessageToNote = async (msg) => {
  const text = msg?.text || ''
  if (!text) {
    ElMessage.warning('没有可保存的内容')
    return
  }
  const payload = {
    title: buildNoteTitle(text),
    content: text,
    summary: buildNoteSummary(text),
    tags: [],
    isPublic: false
  }
  const res = await noteStore.createNote(payload)
  if (res && res.id) {
    ElMessage.success('已保存为笔记')
    router.push(`/notes/editor/${res.id}`)
  }
}

const syncStreamTransport = (roleId) => {
  if (!activeRoleStreaming.value) {
    useWebSocket.value = false
    return
  }
  useWebSocket.value = wsPreferredRoles.includes(roleId)
}

const sendMessage = async () => {
  const text = draft.value.trim()
  if (!text || sending.value) return
  const session = activeConversation.value
  if (!session) return

  const userMessage = buildMessage('user', text)
  session.messages.push(userMessage)
  updateConversationTitle(session, text)
  session.updatedAt = Date.now()
  draft.value = ''
  sending.value = true
  stopRequested.value = false
  nextTick(scrollToBottom)

  const loadingMessage = buildMessage('assistant', '', true)
  session.messages.push(loadingMessage)
  nextTick(scrollToBottom)

  try {
    if (activeRoleStreaming.value) {
      if (useWebSocket.value) {
        await streamViaWebSocket(session, loadingMessage.id, text)
      } else {
        await streamViaSse(session, loadingMessage.id, text)
      }
    } else {
      const res = await aiApi.chat({
        roleId: activeRoleId.value,
        message: text,
        model: chatSettings.value.model,
        systemPrompt: chatSettings.value.systemPrompt,
        allowCrossRoleReference: allowCrossRoleReference.value,
        crossRoleContext: buildCrossRoleContext()
      })
      if (res.code === 200 || res.code === '200') {
        const data = res.data || {}
        const replyText = data.reply || '已处理你的请求。'
        const citations = data.citations || []
        replaceMessage(session, loadingMessage.id, { text: replyText, loading: false, citations, time: Date.now() })
        session.updatedAt = Date.now()
        sortSessions(activeRoleId.value)
      } else {
        replaceMessage(session, loadingMessage.id, { text: res.msg || 'AI处理失败', loading: false, time: Date.now() })
      }
    }
  } catch (error) {
    const fallbackText = stopRequested.value ? '已停止生成。' : (error?.message || '请求失败')
    replaceMessage(session, loadingMessage.id, { text: fallbackText, loading: false, time: Date.now() })
  } finally {
    sending.value = false
    stopRequested.value = false
    activeAbortController = null
    activeSocket = null
    nextTick(scrollToBottom)
  }
}

const stopGeneration = () => {
  stopRequested.value = true
  if (activeAbortController) {
    activeAbortController.abort()
  }
  if (activeSocket && activeSocket.readyState === WebSocket.OPEN) {
    activeSocket.close()
  }
  sending.value = false
}

const scrollToBottom = (force = false) => {
  if (!force && !autoScroll.value) return
  const el = messageScrollRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const handleInputKeydown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

const formatTime = (timestamp) => {
  const d = new Date(timestamp)
  return `${`${d.getHours()}`.padStart(2, '0')}:${`${d.getMinutes()}`.padStart(2, '0')}`
}

const formatConversationTime = (timestamp) => {
  const d = new Date(timestamp)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return formatTime(timestamp)
  }
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const exportConversation = () => {
  const session = activeConversation.value
  if (!session || !session.messages.length) {
    ElMessage.warning('当前对话无可导出内容')
    return
  }
  const lines = session.messages.map(i => `[${new Date(i.time).toLocaleString()}] ${i.role === 'user' ? '我' : activeRoleName.value}: ${i.text}`)
  const content = lines.join('\n\n')
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${session.title || 'AI对话'}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const goAudit = () => {
  router.push('/ai/audit')
}

const createSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) return null
  const recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = true
  recognition.onstart = () => { recognizing.value = true }
  recognition.onend = () => { recognizing.value = false }
  recognition.onerror = () => {
    recognizing.value = false
    ElMessage.error('语音识别失败，请重试')
  }
  recognition.onresult = (event) => {
    draft.value = Array.from(event.results).map(i => i[0]?.transcript || '').join('')
  }
  return recognition
}

const toggleVoiceInput = () => {
  if (!speechRecognition) {
    speechRecognition = createSpeechRecognition()
  }
  if (!speechRecognition) {
    ElMessage.warning('当前浏览器不支持语音输入')
    return
  }
  if (recognizing.value) {
    speechRecognition.stop()
  } else {
    speechRecognition.start()
  }
}

const streamViaSse = async (session, messageId, text) => {
  const token = localStorage.getItem('system-token')
  if (!token) {
    replaceMessage(session, messageId, { text: '未登录或登录已过期', loading: false, time: Date.now() })
    return
  }
  const params = new URLSearchParams()
  params.set('token', token)
  params.set('roleId', activeRoleId.value)
  params.set('message', text)
  params.set('allowCrossRoleReference', String(allowCrossRoleReference.value))
  const crossRoleContext = buildCrossRoleContext()
  if (crossRoleContext) {
    params.set('crossRoleContext', crossRoleContext)
  }
  const apiBase = import.meta.env.VITE_BASE_URL
  const apiPrefix = apiBase && apiBase.startsWith('http') ? apiBase.replace(/\/$/, '') : ''
  const url = `${apiPrefix || ''}/api/ai/stream?${params.toString()}`
  activeAbortController = new AbortController()
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Accept': 'text/event-stream' },
    signal: activeAbortController.signal
  })
  if (!response.ok || !response.body) {
    replaceMessage(session, messageId, { text: '流式连接失败', loading: false, time: Date.now() })
    return
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let replyText = ''
  let citations = []
  let traceId = ''

  while (true) {
    if (stopRequested.value) break
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    for (const part of parts) {
      const lines = part.split('\n').filter(Boolean)
      let eventName = 'message'
      let data = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventName = line.replace('event:', '').trim()
        } else if (line.startsWith('data:')) {
          data += line.replace('data:', '').trim()
        }
      }
      if (eventName === 'chunk') {
        replyText += data
        replaceMessage(session, messageId, { text: replyText, loading: false, citations, time: Date.now() })
        scrollToBottom()
      } else if (eventName === 'meta') {
        try {
          const meta = JSON.parse(data)
          citations = meta.citations || []
          traceId = meta.traceId || traceId
          replaceMessage(session, messageId, { citations })
        } catch {
          citations = []
        }
      } else if (eventName === 'error') {
        replaceMessage(session, messageId, { text: data || '流式处理失败', loading: false, time: Date.now() })
      } else if (eventName === 'done') {
        session.updatedAt = Date.now()
        sortSessions(activeRoleId.value)
      }
    }
  }
  if (stopRequested.value) {
    replaceMessage(session, messageId, { text: replyText || '已停止生成。', loading: false, citations, time: Date.now() })
  }
}

const streamViaWebSocket = async (session, messageId, text) => {
  const token = localStorage.getItem('system-token')
  if (!token) {
    replaceMessage(session, messageId, { text: '未登录或登录已过期', loading: false, time: Date.now() })
    return
  }
  const traceId = `trace-${Date.now()}-${Math.random().toString(16).slice(2)}`
  let replyText = ''
  let citations = []
  let done = false
  let offset = 0
  const wsUrl = buildWsUrl(token)

  const connect = (payload) => new Promise((resolve) => {
    const socket = new WebSocket(wsUrl)
    activeSocket = socket
    socket.onopen = () => {
      if (stopRequested.value) {
        socket.close()
        return
      }
      socket.send(JSON.stringify(payload))
    }
    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'meta') {
          const data = msg.data || {}
          citations = data.citations || citations
        } else if (msg.type === 'chunk') {
          if (stopRequested.value) {
            socket.close()
            return
          }
          const data = msg.data || {}
          const chunk = data.data || ''
          replyText += chunk
          offset = replyText.length
          replaceMessage(session, messageId, { text: replyText, loading: false, citations, time: Date.now() })
          scrollToBottom()
        } else if (msg.type === 'done') {
          done = true
          socket.close()
        } else if (msg.type === 'error') {
          replaceMessage(session, messageId, { text: msg.data?.message || 'WebSocket 失败', loading: false, time: Date.now() })
          socket.close()
        }
      } catch {
      }
    }
    socket.onclose = () => {
      resolve({ done, offset })
    }
  })

  const payload = {
    action: 'chat',
    roleId: activeRoleId.value,
    message: text,
    allowCrossRoleReference: allowCrossRoleReference.value,
    crossRoleContext: buildCrossRoleContext(),
    traceId
  }

  const result = await connect(payload)
  if (!stopRequested.value && !result.done && result.offset > 0) {
    await connect({ action: 'resume', traceId, offset: result.offset })
  }
  if (stopRequested.value) {
    replaceMessage(session, messageId, { text: replyText || '已停止生成。', loading: false, citations, time: Date.now() })
  }
  session.updatedAt = Date.now()
  sortSessions(activeRoleId.value)
}

const buildWsUrl = (token) => {
  const apiBase = import.meta.env.VITE_BASE_URL
  let origin = window.location.origin
  if (apiBase && apiBase.startsWith('http')) {
    try {
      origin = new URL(apiBase).origin
    } catch {}
  }
  const base = origin.replace(/^http/, 'ws')
  return `${base}/ws/ai/chat?token=${encodeURIComponent(token)}`
}

watch(roleConversations, saveState, { deep: true })
watch(roleActiveConversationId, saveState, { deep: true })
watch(activeRoleId, saveState)
watch(storageUserKey, async (newKey, oldKey) => {
  if (!newKey || newKey === oldKey) return
  loadConversationState()
  loadSettings()
  roles.value.forEach(role => ensureRoleContext(role.id))
  ensureRoleContext(activeRoleId.value)
  await nextTick()
  scrollToBottom(true)
})

onMounted(async () => {
  loadConversationState()
  loadSettings()
  await loadRoles()
  ensureRoleContext(activeRoleId.value)
  nextTick(scrollToBottom)
})
</script>

<style scoped>
.ai-chat-page {
  height: calc(100vh - 112px);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  background: #fff;
  border: 1px solid #e9edf5;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.chat-sidebar {
  width: 260px;
  border-right: 1px solid #edf1f7;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
}

.sidebar-head {
  padding: 14px 14px 10px;
}

.new-chat-btn {
  width: 100%;
  border-radius: 10px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 12px;
}

.session-item {
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: transparent;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.session-item:hover {
  background: #f0f5ff;
}

.session-item.active {
  background: #ecf3ff;
  border-color: #d9e7ff;
}

.session-main {
  flex: 1;
  min-width: 0;
}

.session-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.session-item:hover .session-actions,
.session-item.active .session-actions {
  opacity: 1;
}

.session-delete {
  color: #94a3b8;
}

.session-delete:hover {
  color: #ef4444;
}

.session-title {
  font-size: 14px;
  color: #1f2a44;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  margin-top: 4px;
  font-size: 12px;
  color: #8a94ac;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
}

.chat-topbar {
  height: 64px;
  border-bottom: 1px solid #edf1f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mobile-session-btn {
  display: none;
}

.bot-meta {
  display: flex;
  flex-direction: column;
}

.bot-name {
  font-size: 16px;
  font-weight: 600;
  color: #182338;
}

.bot-subtitle {
  font-size: 12px;
  color: #8a94ac;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.role-tabs-wrap {
  border-bottom: 1px solid #edf1f7;
  padding: 8px 10px 8px;
}

.role-tabs {
  display: flex;
  gap: 8px;
  min-width: max-content;
}

.role-tab {
  border: 1px solid #dbe5fb;
  border-radius: 10px;
  background: #f8fbff;
  color: #30456f;
  padding: 6px 10px;
  min-width: 110px;
  cursor: pointer;
  transition: all .2s ease;
}

.role-tab.active {
  background: #eaf1ff;
  border-color: #9cb8ff;
  color: #1f3d90;
}

.tab-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
}

.tab-meta {
  font-size: 11px;
  opacity: .8;
}

.messages-panel {
  flex: 1;
  overflow-y: auto;
  background: linear-gradient(180deg, #fcfdff 0%, #f7f9fd 100%);
}

.messages-inner {
  max-width: 920px;
  margin: 0 auto;
  padding: 18px 20px 20px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

.row-user {
  justify-content: flex-end;
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e4eaf6;
  background: #fff;
  flex-shrink: 0;
}

.bubble-wrap {
  max-width: min(72%, 760px);
}

.msg-bubble {
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-assistant {
  background: #ffffff;
  color: #1f2a44;
  border: 1px solid #e7edf8;
  border-top-left-radius: 6px;
}

.bubble-user {
  background: linear-gradient(135deg, #5c8bff 0%, #4e6fff 100%);
  color: #fff;
  border-top-right-radius: 6px;
}

.user-text {
  white-space: pre-wrap;
}

.ai-markdown {
  color: #1f2a44;
  white-space: normal;
}

.ai-markdown :deep(p) {
  margin: 6px 0;
}

.ai-markdown :deep(h1),
.ai-markdown :deep(h2),
.ai-markdown :deep(h3) {
  margin: 8px 0 6px;
  font-weight: 600;
  color: #182338;
}

.ai-markdown :deep(ul),
.ai-markdown :deep(ol) {
  padding-left: 18px;
  margin: 6px 0;
}

.ai-markdown :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 10px;
  border-left: 3px solid #8fb1ff;
  background: #f5f8ff;
  color: #334155;
}

.ai-markdown :deep(pre) {
  background: #0f172a;
  color: #e2e8f0;
  padding: 10px 12px;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 12px;
}

.ai-markdown :deep(code) {
  background: rgba(15, 23, 42, 0.08);
  padding: 2px 4px;
  border-radius: 6px;
  font-size: 12px;
}

.ai-sections {
  display: grid;
  gap: 10px;
}

.ai-section {
  border: 1px solid #e4ecff;
  border-radius: 12px;
  background: #f8fbff;
  padding: 8px 10px;
}

.ai-section.highlight {
  border-color: #c7dbff;
  background: #edf4ff;
}

.ai-section-title {
  font-size: 12px;
  color: #4b6bba;
  font-weight: 600;
  margin-bottom: 6px;
}

.ai-section-body {
  font-size: 13px;
  color: #1f2a44;
}

.msg-actions {
  display: flex;
  gap: 6px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.msg-action-btn {
  color: #6b7a99;
}

.msg-action-btn:hover {
  color: #3b5bd6;
}

.msg-time {
  margin-top: 4px;
  font-size: 12px;
  color: #9aa3b8;
}

.row-user .msg-time {
  text-align: right;
}

.citation-list {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.citation-tag {
  font-size: 11px;
  color: #4d6db8;
  border: 1px solid #d8e4ff;
  border-radius: 999px;
  padding: 1px 8px;
  background: #f5f8ff;
}

.typing-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 4px;
  border-radius: 50%;
  background: #8093c6;
  animation: blink 1.2s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

.chat-input-panel {
  border-top: 1px solid #edf1f7;
  background: #fff;
  padding: 10px 14px 14px;
}

.input-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.stream-badge {
  font-size: 12px;
  color: #6f7ea1;
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.send-btn {
  height: 78px;
  border-radius: 10px;
  min-width: 92px;
}

.drawer-new-btn {
  width: 100%;
  margin-bottom: 12px;
}

.drawer-session-list {
  overflow-y: auto;
  max-height: calc(100vh - 180px);
}

@keyframes blink {
  0%, 80%, 100% { opacity: 0.35; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-1px); }
}

@media (max-width: 992px) {
  .ai-chat-page {
    height: calc(100vh - 96px);
  }
  .chat-sidebar {
    display: none;
  }
  .mobile-session-btn {
    display: inline-flex;
  }
  .messages-inner {
    padding: 14px 12px 14px;
  }
  .bubble-wrap {
    max-width: 80%;
  }
  .chat-topbar {
    padding: 0 12px;
  }
}

@media (max-width: 640px) {
  .topbar-actions {
    gap: 0;
  }
  .topbar-actions :deep(.el-button) {
    padding-left: 6px;
    padding-right: 6px;
  }
  .send-btn {
    min-width: 72px;
    height: 74px;
    padding-left: 10px;
    padding-right: 10px;
  }
  .msg-bubble {
    font-size: 13px;
  }
  .role-tab {
    min-width: 100px;
  }
}

/* v2 redesign overrides: desktop AI workspace */
.ai-chat-page {
  height: calc(100vh - 124px);
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 300px;
  gap: 16px;
  border: 0;
  border-radius: 0;
  overflow: visible;
  background: transparent;
  box-shadow: none;
}

.chat-sidebar,
.chat-main,
.context-panel {
  min-height: 0;
  border: 1px solid #d8e2ed;
  border-radius: 18px;
  background: #ffffff;
  overflow: hidden;
}

.chat-sidebar {
  width: auto;
  border-right: 1px solid #d8e2ed;
  background: #f8fafc;
}

.sidebar-head {
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.new-chat-btn {
  min-height: 42px;
  border-radius: 12px;
  background: #0f172a;
  border-color: #0f172a;
  font-weight: 700;
}

.session-list {
  padding: 12px;
}

.session-item {
  border-radius: 14px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.session-item:hover {
  background: #f1f8ff;
  border-color: #bfd7ff;
}

.session-item.active {
  background: #eaf4ff;
  border-color: #93c5fd;
}

.session-title {
  color: #0f172a;
  font-weight: 750;
}

.session-time {
  color: #64748b;
}

.chat-main {
  background: #ffffff;
}

.chat-topbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  min-height: 82px;
  height: auto;
  padding: 16px 18px;
  border-bottom-color: #d8e2ed;
}

.bot-name {
  color: #0f172a;
  font-size: 24px;
  font-weight: 850;
  line-height: 1.1;
  white-space: nowrap;
}

.bot-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.topbar-actions :deep(.el-button) {
  min-height: 36px;
  border-radius: 10px;
  color: #334155;
  background: #f8fafc;
}

.topbar-actions :deep(.el-button:hover) {
  color: #0f172a;
  background: #e2e8f0;
}

.role-tabs-wrap {
  padding: 12px 14px;
  border-bottom-color: #d8e2ed;
  background: #fbfdff;
}

.role-tabs {
  gap: 10px;
}

.role-tab {
  min-width: 132px;
  border-radius: 14px;
  border-color: #d8e2ed;
  background: #ffffff;
  color: #334155;
  padding: 10px 12px;
  text-align: left;
}

.role-tab.active {
  color: #0f172a;
  background: #e8f3ff;
  border-color: #93c5fd;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.12);
}

.tab-name {
  font-size: 14px;
}

.messages-panel {
  background:
    linear-gradient(180deg, #ffffff 0%, #f6f9fc 100%),
    radial-gradient(circle at 80% 0%, rgba(37, 99, 235, 0.08), transparent 34%);
}

.messages-inner {
  max-width: 940px;
  padding: 22px 24px;
}

.msg-avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border-color: #cbd5e1;
}

.bubble-wrap {
  max-width: min(74%, 780px);
}

.msg-bubble {
  border-radius: 16px;
  padding: 13px 15px;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.75;
}

.bubble-assistant {
  border-color: #d8e2ed;
  background: #ffffff;
  border-top-left-radius: 6px;
}

.bubble-user {
  background: #0f172a;
  color: #ffffff;
  border-top-right-radius: 6px;
}

.msg-action-btn {
  border-radius: 10px;
}

.citation-tag {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.chat-input-panel {
  padding: 14px 16px 16px;
  border-top-color: #d8e2ed;
  background: #ffffff;
}

.input-toolbar {
  margin-bottom: 12px;
}

.input-toolbar :deep(.el-button) {
  border-radius: 10px;
  background: #f8fafc;
}

.stream-badge {
  border-radius: 999px;
  padding: 4px 10px;
  background: #ecfdf5;
  color: #047857;
  font-weight: 700;
}

.input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 104px;
  gap: 12px;
}

.input-row :deep(.el-textarea__inner) {
  min-height: 82px !important;
  border-radius: 14px;
  border-color: #cbd5e1;
  box-shadow: none;
  line-height: 1.7;
}

.input-row :deep(.el-textarea__inner:focus) {
  border-color: #0f172a;
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.08);
}

.send-btn {
  height: 82px;
  min-width: 104px;
  border-radius: 14px;
  background: #0f172a;
  border-color: #0f172a;
  font-weight: 800;
}

.context-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 14px;
  background: #f8fafc;
  overflow-y: auto;
}

.context-card {
  border: 1px solid #d8e2ed;
  border-radius: 16px;
  padding: 14px;
  background: #ffffff;
}

.context-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.context-card-head strong {
  border-radius: 999px;
  padding: 3px 9px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
}

.context-map {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.context-row {
  display: grid;
  gap: 4px;
  border-radius: 12px;
  padding: 10px;
  background: #f8fafc;
}

.context-row span {
  color: #0f172a;
  font-weight: 800;
}

.context-row b,
.context-card p {
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.context-card p {
  margin: 12px 0 0;
}

.prompt-card {
  display: grid;
  gap: 10px;
}

.prompt-card button {
  border: 1px solid #d8e2ed;
  border-radius: 12px;
  min-height: 40px;
  padding: 0 12px;
  background: #ffffff;
  color: #0f172a;
  cursor: pointer;
  text-align: left;
  font-weight: 700;
}

.prompt-card button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

@media (max-width: 1280px) {
  .ai-chat-page {
    grid-template-columns: 260px minmax(0, 1fr);
  }

  .context-panel {
    display: none;
  }
}

/* image-reference AI workspace pass */
.ai-chat-page {
  height: calc(100vh - 112px);
  grid-template-columns: 268px minmax(0, 1fr) 292px;
  gap: 14px;
}

.chat-sidebar,
.chat-main,
.context-panel {
  border-color: #d7e1ec;
  border-radius: 8px;
  box-shadow: 0 12px 30px rgba(33, 54, 86, 0.05);
}

.chat-sidebar,
.context-panel {
  background: #f7fafc;
}

.sidebar-head {
  padding: 12px;
}

.new-chat-btn {
  min-height: 38px;
  border-radius: 8px;
  background: #1f6feb;
  border-color: #1f6feb;
}

.session-list {
  padding: 10px;
}

.session-item {
  border-radius: 8px;
  padding: 10px;
}

.session-item.active {
  background: #e9f2ff;
  border-color: #bed6ff;
}

.session-title {
  color: #172033;
  font-size: 13px;
}

.session-time {
  color: #66758b;
}

.chat-topbar {
  min-height: 72px;
  padding: 12px 14px;
}

.bot-name {
  color: #172033;
  font-size: 20px;
}

.bot-subtitle {
  color: #66758b;
}

.topbar-actions :deep(.el-button) {
  border-radius: 8px;
  background: #f7fafc;
}

.role-tabs-wrap {
  padding: 10px 12px;
}

.role-tab {
  min-width: 126px;
  border-radius: 8px;
  padding: 8px 10px;
}

.role-tab.active {
  background: #e9f2ff;
  border-color: #bed6ff;
}

.messages-panel {
  background: #fbfdff;
}

.messages-inner {
  max-width: 900px;
  padding: 18px 20px;
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
}

.msg-bubble {
  border-radius: 8px;
  padding: 11px 13px;
}

.bubble-assistant {
  border-color: #d7e1ec;
}

.bubble-user {
  background: #1f6feb;
}

.chat-input-panel {
  padding: 12px 14px 14px;
}

.input-toolbar {
  gap: 8px;
}

.stream-badge {
  background: #eaf8ef;
  color: #15803d;
}

.input-row {
  grid-template-columns: minmax(0, 1fr) 96px;
  gap: 10px;
}

.input-row :deep(.el-textarea__inner) {
  min-height: 76px !important;
  border-radius: 8px;
}

.send-btn {
  height: 76px;
  min-width: 96px;
  border-radius: 8px;
  background: #1f6feb;
  border-color: #1f6feb;
}

.context-panel {
  padding: 12px;
}

.context-card {
  border-color: #d7e1ec;
  border-radius: 8px;
}

.context-card-head strong,
.citation-tag {
  border-radius: 999px;
}

.context-row,
.prompt-card button {
  border-radius: 8px;
}

.prompt-card button:hover {
  border-color: #bed6ff;
  background: #e9f2ff;
}

@media (max-width: 1280px) {
  .ai-chat-page {
    grid-template-columns: 260px minmax(0, 1fr);
  }
}

/* collapsible chat workspace */
.ai-chat-page {
  transition: grid-template-columns 200ms ease;
}

.ai-chat-page.sessions-collapsed {
  grid-template-columns: 62px minmax(0, 1fr) 292px;
}

.ai-chat-page.context-collapsed {
  grid-template-columns: 268px minmax(0, 1fr) 62px;
}

.ai-chat-page.sessions-collapsed.context-collapsed {
  grid-template-columns: 62px minmax(0, 1fr) 62px;
}

.chat-sidebar,
.context-panel {
  transition: width 200ms ease, padding 200ms ease, background 200ms ease;
}

.chat-sidebar.collapsed,
.context-panel.collapsed {
  width: auto;
  align-items: center;
  padding: 10px 8px;
  overflow: hidden;
}

.sidebar-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-sidebar.collapsed .sidebar-head {
  flex-direction: column;
  padding: 8px 0;
  border-bottom: 0;
}

.panel-icon-btn {
  width: 34px;
  height: 34px;
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  display: inline-grid;
  place-items: center;
  color: #526276;
  background: #ffffff;
  cursor: pointer;
  transition: background 160ms ease, color 160ms ease, border-color 160ms ease;
}

.panel-icon-btn:hover {
  border-color: #bed6ff;
  color: #1f6feb;
  background: #e9f2ff;
}

.panel-icon-btn.is-large {
  width: 42px;
  height: 42px;
}

.panel-icon-btn.is-primary {
  color: #ffffff;
  border-color: #1f6feb;
  background: #1f6feb;
}

.collapsed-panel-summary {
  display: grid;
  place-items: center;
  gap: 2px;
  margin-top: 8px;
  color: #66758b;
  writing-mode: vertical-rl;
}

.collapsed-panel-summary span {
  color: #172033;
  font-size: 18px;
  font-weight: 850;
  writing-mode: horizontal-tb;
}

.collapsed-panel-summary small {
  font-size: 12px;
  letter-spacing: 0;
}

.role-select-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid #d7e1ec;
  padding: 10px 14px;
  background: #fbfdff;
}

.role-select-card {
  min-width: min(420px, 50%);
  display: grid;
  grid-template-columns: auto minmax(220px, 1fr);
  align-items: center;
  gap: 10px;
}

.role-label {
  color: #66758b;
  font-size: 12px;
  font-weight: 800;
}

.role-select {
  width: 100%;
}

.role-select :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #d7e1ec inset;
}

.role-select :deep(.el-select-dropdown__item) {
  height: auto;
  padding: 8px 10px;
}

.role-option {
  display: grid;
  gap: 2px;
  line-height: 1.25;
}

.role-option span {
  color: #172033;
  font-weight: 800;
}

.role-option small {
  color: #66758b;
  font-size: 12px;
}

.conversation-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.conversation-tools span {
  border: 1px solid #d7e1ec;
  border-radius: 999px;
  padding: 4px 10px;
  color: #526276;
  background: #ffffff;
  font-size: 12px;
  font-weight: 750;
}

.topbar-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.danger-tool {
  color: #dc2626 !important;
}

.context-panel.collapsed {
  justify-content: flex-start;
  background: #ffffff;
}

.context-collapsed-tools {
  min-height: 180px;
  display: grid;
  justify-items: center;
  align-content: start;
  gap: 12px;
}

.context-collapsed-tools span {
  color: #66758b;
  font-size: 12px;
  font-weight: 800;
  writing-mode: vertical-rl;
}

.context-head-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.context-head-actions .panel-icon-btn {
  width: 30px;
  height: 30px;
}

.ai-chat-page.sessions-collapsed .messages-inner,
.ai-chat-page.context-collapsed .messages-inner {
  max-width: 1080px;
}

.ai-chat-page.sessions-collapsed.context-collapsed .messages-inner {
  max-width: 1160px;
}

@media (max-width: 1280px) {
  .ai-chat-page,
  .ai-chat-page.sessions-collapsed,
  .ai-chat-page.context-collapsed,
  .ai-chat-page.sessions-collapsed.context-collapsed {
    grid-template-columns: minmax(0, 1fr);
  }

  .chat-sidebar,
  .context-panel {
    display: none;
  }

  .role-select-wrap {
    align-items: stretch;
    flex-direction: column;
  }

  .role-select-card {
    min-width: 0;
    grid-template-columns: 1fr;
  }
}
</style>
