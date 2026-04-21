<template>
  <div class="ai-chat-page">
    <aside class="chat-sidebar">
      <div class="sidebar-head">
        <el-button type="primary" class="new-chat-btn" @click="startNewConversation">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
      </div>
      <div class="session-list">
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
        </div>
      </header>

      <div class="role-tabs-wrap">
        <el-scrollbar>
          <div class="role-tabs">
            <button
              v-for="role in roles"
              :key="role.id"
              :class="['role-tab', { active: role.id === activeRoleId }]"
              @click="switchRole(role.id)"
            >
              <span class="tab-name">{{ role.name }}</span>
              <span class="tab-meta">{{ role.streaming ? '流式' : '非流式' }}</span>
            </button>
          </div>
        </el-scrollbar>
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
            <el-option label="glm-5-turbo" value="glm-5-turbo" />
            <el-option label="gpt-5.3-codex" value="gpt-5.3-codex" />
            <el-option label="qwen3.5-plus" value="qwen3.5-plus" />
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
import { Plus, Operation, Setting, Download, RefreshRight, Microphone, Loading, Promotion, DataAnalysis, CopyDocument, DocumentAdd, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'
import { aiApi } from '@/api'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import { useNoteStore } from '@/stores/note'

const LOCAL_CONVERSATION_KEY = 'ai-chat-conversations-role-v2'
const LOCAL_SETTINGS_KEY = 'ai-chat-settings-v2'
const assistantAvatar = 'https://api.dicebear.com/7.x/bottts-neutral/svg?seed=plnm-ai'

const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)
const userAvatar = computed(() => userInfo.value?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')
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
const allowCrossRoleReference = ref(false)
const useWebSocket = ref(false)
const chatSettings = ref({
  model: 'glm-5-turbo',
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

let speechRecognition = null

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
  localStorage.setItem(LOCAL_CONVERSATION_KEY, JSON.stringify({
    roleConversations: roleConversations.value,
    roleActiveConversationId: roleActiveConversationId.value,
    activeRoleId: activeRoleId.value
  }))
}

const saveSettings = () => {
  localStorage.setItem(LOCAL_SETTINGS_KEY, JSON.stringify(chatSettings.value))
  settingsVisible.value = false
  ElMessage.success('设置已保存')
}

const loadSettings = () => {
  const raw = localStorage.getItem(LOCAL_SETTINGS_KEY)
  if (!raw) return
  try {
    const parsed = JSON.parse(raw)
    chatSettings.value = {
      model: parsed.model || 'glm-5-turbo',
      systemPrompt: parsed.systemPrompt || ''
    }
  } catch {
    chatSettings.value = { model: 'glm-5-turbo', systemPrompt: '' }
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
  const raw = localStorage.getItem(LOCAL_CONVERSATION_KEY)
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
    replaceMessage(session, loadingMessage.id, { text: error?.message || '请求失败', loading: false, time: Date.now() })
  } finally {
    sending.value = false
    nextTick(scrollToBottom)
  }
}

const scrollToBottom = () => {
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
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Accept': 'text/event-stream' }
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
    socket.onopen = () => {
      socket.send(JSON.stringify(payload))
    }
    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'meta') {
          const data = msg.data || {}
          citations = data.citations || citations
        } else if (msg.type === 'chunk') {
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
  if (!result.done && result.offset > 0) {
    await connect({ action: 'resume', traceId, offset: result.offset })
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
</style>
