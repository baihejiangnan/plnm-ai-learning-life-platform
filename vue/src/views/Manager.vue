<template>
  <div :class="['plnm-shell', { 'rail-collapsed': isCollapse }]">
    <aside class="plnm-rail" :class="{ 'is-collapsed': isCollapse }">
      <div class="brand-block" @click="router.push('/home')">
        <div class="brand-mark">
          <el-icon><Document /></el-icon>
        </div>
        <div class="brand-copy" v-if="!isCollapse">
          <strong>PLNM</strong>
          <span>Personal Life Notes Manager</span>
        </div>
      </div>

      <button
        class="rail-toggle"
        type="button"
        :title="isCollapse ? '展开导航' : '收起导航'"
        @click="handleCollapse"
      >
        <el-icon><component :is="isCollapse ? Expand : Fold" /></el-icon>
        <span v-if="!isCollapse">收起导航</span>
      </button>

      <nav class="rail-nav" aria-label="主导航">
        <section v-for="group in navGroups" :key="group.title" class="nav-section">
          <div v-if="!isCollapse" class="nav-section-title">{{ group.title }}</div>
          <button
            v-for="item in group.items"
            :key="item.path"
            type="button"
            :class="['nav-button', { active: isRouteActive(item.path) }]"
            :title="isCollapse ? item.label : ''"
            @click="router.push(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span v-if="!isCollapse" class="label">{{ item.label }}</span>
            <span v-if="!isCollapse && item.badge" class="count">{{ item.badge }}</span>
          </button>
        </section>
      </nav>

      <div class="rail-card" v-if="!isCollapse">
        <div class="rail-card-title">今日建议</div>
        <p>先处理预算阈值、学习进度和本周复盘，再进入 AI 对话生成总结。</p>
        <button type="button" @click="openAiQuickDialog">AI 快捷执行</button>
      </div>
    </aside>

    <section class="plnm-main">
      <header :class="['workspace-topbar', { 'is-scrolled': contentScrolled }]">
        <div class="command-search">
          <el-icon><Search /></el-icon>
          <el-input
            v-model="globalSearchKeyword"
            placeholder="搜索笔记、课程、账单、AI 对话"
            clearable
            @keyup.enter="handleGlobalSearch"
          />
          <span class="search-hint">Enter</span>
        </div>

        <div class="topbar-actions">
          <el-tooltip content="新建笔记" placement="bottom">
            <button class="icon-action" type="button" @click="createNote">
              <el-icon><Plus /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="AI 快捷操作" placement="bottom">
            <button class="icon-action is-primary" type="button" @click="openAiQuickDialog">
              <el-icon><MagicStick /></el-icon>
            </button>
          </el-tooltip>
          <el-popover
            placement="bottom-end"
            trigger="click"
            :width="380"
            popper-class="plnm-notification-popover"
            @show="refreshNotifications"
          >
            <template #reference>
              <el-badge
                :value="unreadCount"
                :hidden="unreadCount === 0"
                :max="99"
                class="notification-badge"
              >
                <button
                  :class="['icon-action', 'notification-action', { 'has-unread': unreadCount > 0 }]"
                  type="button"
                  title="通知中心"
                >
                  <el-icon><Bell /></el-icon>
                </button>
              </el-badge>
            </template>

            <div class="notification-panel">
              <div class="notification-panel-head">
                <div>
                  <strong>通知中心</strong>
                  <span>{{ unreadCount > 0 ? `${unreadCount} 条未读` : '暂无未读消息' }}</span>
                </div>
                <div class="notification-head-actions">
                  <button type="button" title="刷新通知" @click="refreshNotifications">
                    <el-icon><RefreshRight /></el-icon>
                  </button>
                  <button type="button" @click="goNotificationSettings">设置</button>
                </div>
              </div>

              <div class="notification-summary-strip">
                <span>实时提醒</span>
                <strong>{{ notificationPreview.length }}</strong>
                <small>最近动态</small>
              </div>

              <div v-if="notificationPreview.length" class="notification-feed-mini">
                <button
                  v-for="item in notificationPreview"
                  :key="item.id"
                  :class="['notification-mini-item', { unread: item.status === 'unread' }]"
                  type="button"
                  @click="openNotification(item)"
                >
                  <span :class="['notification-type-dot', `is-${item.type || 'system'}`]"></span>
                  <span class="notification-mini-main">
                    <strong>{{ item.title }}</strong>
                    <em>{{ item.content }}</em>
                  </span>
                  <time>{{ formatNotificationTime(item.createTime) }}</time>
                </button>
              </div>

              <div v-else class="notification-empty-mini">
                <el-icon><Bell /></el-icon>
                <span>暂无通知</span>
              </div>

              <div class="notification-panel-foot">
                <button type="button" :disabled="unreadCount === 0" @click="markAllPreviewRead">全部已读</button>
                <button type="button" @click="goNotificationSettings">查看设置</button>
              </div>
            </div>
          </el-popover>

          <el-dropdown
            trigger="click"
            popper-class="plnm-user-menu-popper"
            @command="handleUserCommand"
            class="user-dropdown"
          >
            <div class="profile-chip">
              <img class="avatar" :src="user?.avatar || defaultAvatar" alt="用户头像">
              <div class="profile-copy">
                <strong>{{ user?.name || user?.username || 'newuser' }}</strong>
                <span>{{ user?.role || 'USER' }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main ref="contentRef" class="workspace-content" @scroll="handleContentScroll">
        <div :class="contentWrapperClass">
          <router-view @updateUser="updateUser" />
        </div>
        <button
          v-show="showBackTop"
          class="back-to-top"
          type="button"
          title="回到顶部"
          @click="scrollToTop"
        >
          <el-icon><ArrowUp /></el-icon>
        </button>
      </main>
    </section>

    <el-dialog v-model="aiDialogVisible" title="AI 对话式快捷操作" width="560px" class="ai-quick-dialog">
      <div class="ai-guide">
        示例：记一笔餐饮消费 35 元；把 Vue 课程进度加 10%；新建一篇复盘笔记
      </div>
      <el-input
        v-model="aiMessage"
        type="textarea"
        :rows="4"
        resize="none"
        placeholder="请输入你想让 AI 帮你完成的操作"
      />
      <div class="ai-dialog-actions">
        <el-button @click="fillExample('记一笔餐饮消费 32 元，备注午餐')">记账示例</el-button>
        <el-button @click="fillExample('把课程 Vue3 项目实战 进度加10%，备注今天完成组件通信')">学习示例</el-button>
        <el-button @click="fillExample('新建笔记：本周复盘，内容是本周学习与消费总结')">笔记示例</el-button>
      </div>
      <div v-if="aiResult" class="ai-result">
        <div class="ai-result-title">执行结果</div>
        <div class="ai-result-text">{{ aiResult.reply }}</div>
        <div v-if="aiResult.intent" class="ai-result-meta">意图：{{ aiResult.intent }}</div>
      </div>
      <template #footer>
        <el-button @click="aiDialogVisible = false">关闭</el-button>
        <el-button v-if="aiResult?.navigateTo" @click="router.push(aiResult.navigateTo)">前往查看</el-button>
        <el-button type="primary" :loading="aiLoading" @click="submitAiQuickAction">执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { storeToRefs } from 'pinia'
import {
  ArrowDown,
  ArrowUp,
  Bell,
  ChatDotRound,
  Collection,
  DataAnalysis,
  Document,
  EditPen,
  Expand,
  Fold,
  HomeFilled,
  MagicStick,
  Plus,
  PriceTag,
  Reading,
  RefreshRight,
  Search,
  Setting,
  SwitchButton,
  TrendCharts,
  User,
  Wallet
} from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { useUserStore } from '@/stores/user'
import { aiApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { userInfo } = storeToRefs(userStore)
const { unreadCount, list: notificationList } = storeToRefs(notificationStore)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const user = userInfo
const globalSearchKeyword = ref('')
const isCollapse = ref(false)
const contentRef = ref(null)
const contentScrolled = ref(false)
const showBackTop = ref(false)
const aiDialogVisible = ref(false)
const aiMessage = ref('')
const aiLoading = ref(false)
const aiResult = ref(null)

const notificationPreview = computed(() => (notificationList.value || []).slice(0, 5))

const navGroups = [
  {
    title: '工作台',
    items: [
      { label: '首页', path: '/home', icon: HomeFilled },
      { label: 'AI 对话', path: '/ai/chat', icon: ChatDotRound },
      { label: 'AI 审计', path: '/ai/audit', icon: DataAnalysis }
    ]
  },
  {
    title: '内容',
    items: [
      { label: '我的笔记', path: '/notes/list', icon: Document },
      { label: '写笔记', path: '/notes/editor', icon: EditPen },
      { label: '标签管理', path: '/notes/tags', icon: PriceTag }
    ]
  },
  {
    title: '生活与学习',
    items: [
      { label: '消费记录', path: '/life/expenses', icon: Wallet },
      { label: '预算设置', path: '/life/budgets', icon: PriceTag },
      { label: '课程分类', path: '/learning/courses', icon: Collection },
      { label: '学习进度', path: '/learning/progress', icon: TrendCharts },
      { label: '资源库', path: '/learning/resources', icon: Reading }
    ]
  },
  {
    title: '账户',
    items: [
      { label: '个人资料', path: '/person/info', icon: User },
      { label: '系统设置', path: '/settings', icon: Setting }
    ]
  }
]

const contentWrapperClass = computed(() => [
  'content-canvas',
  {
    'is-ai-page': route.path.startsWith('/ai/chat')
  }
])

const isRouteActive = (path) => {
  if (path === '/home') return route.path === '/home'
  return route.path === path || route.path.startsWith(`${path}/`)
}

const handleGlobalSearch = () => {
  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) return
  router.push({ path: '/notes/list', query: { keyword } })
}

const createNote = () => {
  router.push('/notes/editor')
}

const handleUserCommand = (command) => {
  if (command === 'profile') router.push('/person/info')
  if (command === 'settings') router.push('/settings')
  if (command === 'logout') logout()
}

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
  } catch (error) {
    if (error !== 'cancel') console.error('退出登录失败:', error)
  }
}

const handleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const updateUser = async () => {
  try {
    await userStore.refreshUserInfo()
  } catch (e) {
    console.warn('updateUser: 刷新用户信息失败（已静默）', e?.message || e)
  }
}

const goNotificationSettings = () => {
  router.push({ path: '/settings', query: { tab: 'notification' } })
}

const refreshNotifications = async () => {
  try {
    await notificationStore.fetchOverview()
  } catch (e) {
    await notificationStore.fetchList('unread', 1, 6)
  }
}

const normalizeNotificationLink = (link) => {
  if (!link) return ''
  if (link === '/notes') return '/notes/list'
  return link
}

const openNotification = async (item) => {
  if (!item) return
  if (item.status === 'unread') {
    await notificationStore.markRead([item.id])
  }
  const target = normalizeNotificationLink(item.linkUrl)
  if (target) {
    router.push(target)
  }
}

const markAllPreviewRead = async () => {
  const ids = (notificationList.value || [])
    .filter(item => item.status === 'unread')
    .map(item => item.id)
  if (!ids.length) return
  const ok = await notificationStore.markRead(ids)
  if (ok) {
    ElMessage.success('未读通知已处理')
    refreshNotifications()
  }
}

const formatNotificationTime = (value) => {
  if (!value) return '刚刚'
  const diff = Date.now() - Number(value)
  const minutes = Math.max(1, Math.floor(diff / 60000))
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return new Date(Number(value)).toLocaleDateString()
}

const handleContentScroll = () => {
  const el = contentRef.value
  if (!el) return
  const top = el.scrollTop || 0
  contentScrolled.value = top > 0
  showBackTop.value = top > 300
}

const scrollToTop = () => {
  contentRef.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

const openAiQuickDialog = () => {
  aiDialogVisible.value = true
}

const fillExample = (value) => {
  aiMessage.value = value
}

const submitAiQuickAction = async () => {
  if (!aiMessage.value.trim()) {
    ElMessage.warning('请先输入操作内容')
    return
  }
  aiLoading.value = true
  try {
    const res = await aiApi.quickAction({ message: aiMessage.value.trim() })
    if (res.code === 200 || res.code === '200') {
      aiResult.value = res.data || null
      ElMessage.success('AI 已处理请求')
    } else {
      ElMessage.error(res.msg || 'AI 请求失败')
    }
  } catch (error) {
    ElMessage.error(error?.message || 'AI 请求失败')
  } finally {
    aiLoading.value = false
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    await refreshNotifications()
  } catch (e) {}
  handleContentScroll()
})
</script>

<style scoped>
.plnm-shell {
  --shell-bg: #edf4fb;
  --surface: #ffffff;
  --surface-soft: #f7fafc;
  --ink: #0f172a;
  --muted: #64748b;
  --line: #d8e2ed;
  --primary: #0f172a;
  --accent: #2563eb;
  --success: #059669;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  overflow: visible;
  background: var(--shell-bg);
  color: var(--ink);
  transition: grid-template-columns 220ms ease;
}

.plnm-shell.rail-collapsed {
  grid-template-columns: 76px minmax(0, 1fr);
}

.plnm-rail {
  position: sticky;
  top: 0;
  height: 100vh;
  background: #09111f;
  color: #dbeafe;
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.plnm-rail.is-collapsed {
  padding-left: 10px;
  padding-right: 10px;
}

.plnm-shell.rail-collapsed .brand-block,
.plnm-shell.rail-collapsed .nav-button {
  grid-template-columns: 1fr;
  justify-items: center;
}

.brand-block {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #12b981);
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.28);
}

.brand-copy {
  min-width: 0;
}

.brand-copy strong {
  display: block;
  font-size: 18px;
  letter-spacing: 0.08em;
}

.brand-copy span {
  display: block;
  margin-top: 2px;
  color: #8fa3bc;
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rail-toggle,
.nav-button,
.rail-card button,
.icon-action,
.back-to-top {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.rail-toggle {
  height: 38px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.06);
  color: #cbd5e1;
  transition: background 180ms ease, color 180ms ease, width 220ms ease;
}

.rail-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.plnm-shell.rail-collapsed .rail-toggle {
  width: 100%;
}

.rail-nav {
  display: grid;
  gap: 16px;
}

.nav-section {
  display: grid;
  gap: 6px;
}

.nav-section-title {
  padding: 0 10px 4px;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: #7690ad;
}

.nav-button {
  min-height: 42px;
  width: 100%;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  padding: 0 12px;
  background: transparent;
  color: #9fb1c7;
  text-align: left;
  transition: background 180ms ease, color 180ms ease, transform 180ms ease;
}

.is-collapsed .nav-button {
  grid-template-columns: 1fr;
  justify-items: center;
  padding: 0;
}

.nav-button:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.nav-button.active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.18);
}

.nav-button .label {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.nav-button .count {
  min-width: 26px;
  border-radius: 999px;
  padding: 2px 8px;
  text-align: center;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 11px;
}

.rail-card {
  margin-top: auto;
  border-radius: 16px;
  padding: 14px;
  color: #dbeafe;
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.22), rgba(5, 150, 105, 0.16));
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.rail-card-title {
  font-weight: 700;
}

.rail-card p {
  margin: 8px 0 12px;
  color: #b7c7d9;
  font-size: 12px;
  line-height: 1.7;
}

.rail-card button {
  width: 100%;
  height: 36px;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  font-weight: 700;
}

.plnm-main {
  min-width: 0;
  min-height: 100vh;
  display: grid;
  grid-template-rows: 76px minmax(0, 1fr);
}

.workspace-topbar {
  position: sticky;
  top: 0;
  display: grid;
  grid-template-columns: minmax(320px, 620px) auto;
  align-items: center;
  gap: 18px;
  padding: 14px 24px;
  border-bottom: 1px solid rgba(216, 226, 237, 0.8);
  background: rgba(237, 244, 251, 0.92);
  backdrop-filter: blur(18px);
  z-index: 10;
}

.workspace-topbar.is-scrolled {
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.command-search {
  min-width: 0;
  height: 48px;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 0 12px;
  background: #ffffff;
}

.command-search :deep(.el-input__wrapper) {
  padding: 0;
  border: 0;
  box-shadow: none;
  background: transparent;
}

.command-search :deep(.el-input__inner) {
  height: 42px;
  font-size: 15px;
}

.search-hint {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 3px 8px;
  color: var(--muted);
  font-size: 12px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.icon-action {
  width: 44px;
  height: 44px;
  display: inline-grid;
  place-items: center;
  border-radius: 12px;
  color: #475569;
  background: #ffffff;
  border: 1px solid var(--line);
}

.icon-action:hover,
.icon-action.is-primary {
  color: #ffffff;
  background: #0f172a;
  border-color: #0f172a;
}

.profile-chip {
  height: 48px;
  display: grid;
  grid-template-columns: 38px minmax(0, 128px) 18px;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 0 12px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.profile-chip:hover {
  border-color: #93c5fd;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 13px;
  object-fit: cover;
  background: linear-gradient(135deg, #0ea5e9, #059669);
  border: 2px solid #e2e8f0;
}

.profile-copy {
  min-width: 0;
}

.profile-copy strong,
.profile-copy span {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.profile-copy strong {
  color: var(--ink);
  font-size: 13px;
}

.profile-copy span {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.workspace-content {
  min-height: 0;
  overflow: visible;
  padding: 24px;
}

.content-canvas {
  width: min(1480px, 100%);
  margin: 0 auto;
}

.content-canvas.is-ai-page {
  width: 100%;
  height: 100%;
}

.back-to-top {
  position: fixed;
  right: 26px;
  bottom: 26px;
  z-index: 30;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  color: #ffffff;
  background: #0f172a;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.2);
}

.ai-guide {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #cfe6ff;
  background: #f1f8ff;
  color: #31557c;
  font-size: 13px;
}

.ai-dialog-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.ai-result {
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #cfe6ff;
  background: #f8fbff;
}

.ai-result-title {
  color: #0f3d68;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 6px;
}

.ai-result-text {
  color: #334155;
  line-height: 1.7;
}

.ai-result-meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

:global(.plnm-user-menu-popper) {
  border: 1px solid rgba(148, 163, 184, 0.32) !important;
  border-radius: 16px !important;
  padding: 6px !important;
  background: rgba(255, 255, 255, 0.98) !important;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.16) !important;
  backdrop-filter: blur(14px);
}

:global(.plnm-user-menu-popper .el-popper__arrow::before) {
  border-color: rgba(148, 163, 184, 0.22) !important;
  background: rgba(255, 255, 255, 0.98) !important;
}

:global(.plnm-user-menu-popper .el-dropdown-menu) {
  min-width: 168px;
  padding: 4px;
  border: 0;
  background: transparent;
}

:global(.plnm-user-menu-popper .el-dropdown-menu__item) {
  height: 40px;
  gap: 10px;
  border-radius: 11px;
  padding: 0 12px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  line-height: 40px;
}

:global(.plnm-user-menu-popper .el-dropdown-menu__item .el-icon) {
  margin-right: 0;
  color: #64748b;
  font-size: 16px;
}

:global(.plnm-user-menu-popper .el-dropdown-menu__item:not(.is-disabled):focus),
:global(.plnm-user-menu-popper .el-dropdown-menu__item:not(.is-disabled):hover) {
  background: #eef6ff;
  color: #0f172a;
}

:global(.plnm-user-menu-popper .el-dropdown-menu__item:not(.is-disabled):focus .el-icon),
:global(.plnm-user-menu-popper .el-dropdown-menu__item:not(.is-disabled):hover .el-icon) {
  color: #2563eb;
}

:global(.plnm-user-menu-popper .el-dropdown-menu__item--divided) {
  margin: 6px 0 0;
  border-top: 1px solid #e2e8f0;
}

@media (max-width: 1100px) {
  .plnm-shell {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .plnm-rail:not(.is-collapsed) {
    padding-left: 10px;
    padding-right: 10px;
  }

  .brand-copy,
  .rail-toggle span,
  .nav-section-title,
  .nav-button .label,
  .nav-button .count,
  .rail-card {
    display: none;
  }

  .brand-block,
  .nav-button {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .workspace-topbar {
    grid-template-columns: 1fr;
  }
}

/* image-reference workbench redesign */
.plnm-shell {
  --shell-bg: #edf3f8;
  --surface: #ffffff;
  --surface-soft: #f7fafc;
  --ink: #172033;
  --muted: #66758b;
  --line: #d7e1ec;
  --primary: #1f6feb;
  --accent: #1f6feb;
  --success: #16a34a;
  grid-template-columns: 236px minmax(0, 1fr);
  background: var(--shell-bg);
}

.plnm-shell.rail-collapsed {
  grid-template-columns: 80px minmax(0, 1fr);
}

.plnm-rail {
  margin: 16px 0 16px 16px;
  height: calc(100vh - 32px);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
  color: var(--ink);
  padding: 14px 12px;
  box-shadow: 0 12px 30px rgba(33, 54, 86, 0.06);
}

.brand-block {
  min-height: 44px;
  padding: 4px;
  border-radius: 8px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: linear-gradient(135deg, #1f6feb, #27b883);
  box-shadow: none;
}

.brand-copy strong {
  color: var(--ink);
  font-size: 15px;
  letter-spacing: 0;
}

.brand-copy span,
.nav-section-title {
  color: var(--muted);
  letter-spacing: 0;
}

.rail-toggle {
  height: 32px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #f5f8fb;
  color: #516174;
}

.rail-toggle:hover {
  background: #ebf3ff;
  color: var(--primary);
}

.rail-nav {
  gap: 12px;
}

.nav-section {
  gap: 4px;
}

.nav-section-title {
  padding: 0 8px 4px;
  font-size: 11px;
  font-weight: 750;
}

.nav-button {
  min-height: 38px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #526276;
  font-weight: 700;
}

.nav-button:hover {
  background: #f2f7fd;
  color: var(--ink);
}

.nav-button.active {
  border-color: #c8dcff;
  background: #e9f2ff;
  color: #1557c0;
  box-shadow: none;
}

.nav-button .count {
  background: #dbeafe;
  color: #1557c0;
}

.rail-card {
  border: 1px solid #cfe0f2;
  border-radius: 8px;
  color: var(--ink);
  background: #f3f8ff;
}

.rail-card p {
  color: var(--muted);
}

.rail-card button {
  border: 1px solid #1f6feb;
  border-radius: 8px;
  background: #1f6feb;
  color: #ffffff;
}

.plnm-main {
  grid-template-rows: 80px minmax(0, 1fr);
}

.workspace-topbar {
  margin: 16px 16px 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 30px rgba(33, 54, 86, 0.05);
}

.workspace-topbar.is-scrolled {
  box-shadow: 0 16px 36px rgba(33, 54, 86, 0.08);
}

.command-search {
  height: 46px;
  border-radius: 8px;
  background: #f9fbfd;
}

.search-hint {
  border-radius: 6px;
  background: #ffffff;
}

.icon-action {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  background: #f9fbfd;
}

.icon-action:hover,
.icon-action.is-primary {
  color: #ffffff;
  background: #1f6feb;
  border-color: #1f6feb;
}

.profile-chip {
  height: 46px;
  grid-template-columns: 34px minmax(0, 112px) 16px;
  border-radius: 8px;
  background: #f9fbfd;
  box-shadow: none;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
}

.workspace-content {
  padding: 16px;
}

.content-canvas {
  width: min(1440px, 100%);
}

.back-to-top {
  border-radius: 8px;
  background: #1f6feb;
}

:global(.plnm-user-menu-popper) {
  border-radius: 8px !important;
}

:global(.plnm-user-menu-popper .el-dropdown-menu__item) {
  border-radius: 6px;
}

.notification-badge :deep(.el-badge__content) {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border: 2px solid #ffffff;
  border-radius: 999px;
  background: #ef4444;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(239, 68, 68, 0.28);
}

.notification-action {
  position: relative;
  overflow: hidden;
}

.notification-action.has-unread::after {
  content: '';
  position: absolute;
  top: 10px;
  right: 11px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.14);
}

:global(.plnm-notification-popover) {
  padding: 0 !important;
  border: 1px solid #d7e1ec !important;
  border-radius: 8px !important;
  box-shadow: 0 24px 50px rgba(23, 32, 51, 0.16) !important;
}

.notification-panel {
  padding: 14px;
  color: #172033;
}

.notification-panel-head,
.notification-panel-foot,
.notification-mini-item,
.notification-summary-strip {
  display: flex;
  align-items: center;
}

.notification-panel-head {
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5edf5;
}

.notification-panel-head strong {
  display: block;
  font-size: 17px;
  color: #101827;
}

.notification-panel-head span {
  display: block;
  margin-top: 2px;
  color: #66758b;
  font-size: 12px;
}

.notification-head-actions {
  display: flex;
  gap: 6px;
}

.notification-head-actions button,
.notification-panel-foot button {
  height: 32px;
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: #f8fbfe;
  color: #40516a;
  font-weight: 750;
  cursor: pointer;
}

.notification-head-actions button {
  min-width: 32px;
  padding: 0 10px;
}

.notification-summary-strip {
  justify-content: space-between;
  margin: 12px 0;
  padding: 12px;
  border: 1px solid #cfe0f2;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(31, 111, 235, 0.12), rgba(39, 184, 131, 0.12)),
    #f7fbff;
}

.notification-summary-strip span,
.notification-summary-strip small {
  color: #526276;
  font-size: 12px;
  font-weight: 750;
}

.notification-summary-strip strong {
  color: #1557c0;
  font-size: 24px;
  line-height: 1;
}

.notification-feed-mini {
  display: grid;
  gap: 8px;
  max-height: 324px;
  overflow: auto;
  padding-right: 2px;
}

.notification-mini-item {
  width: 100%;
  gap: 10px;
  min-height: 70px;
  padding: 10px;
  border: 1px solid #e0e8f1;
  border-radius: 8px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
}

.notification-mini-item:hover {
  border-color: #9fc5ff;
  background: #f7fbff;
}

.notification-mini-item.unread {
  border-color: #b9d6ff;
  background: #edf5ff;
}

.notification-type-dot {
  width: 10px;
  height: 10px;
  flex: 0 0 10px;
  border-radius: 50%;
  background: #1f6feb;
  box-shadow: 0 0 0 5px rgba(31, 111, 235, 0.12);
}

.notification-type-dot.is-security {
  background: #ef4444;
  box-shadow: 0 0 0 5px rgba(239, 68, 68, 0.12);
}

.notification-type-dot.is-collaboration {
  background: #16a34a;
  box-shadow: 0 0 0 5px rgba(22, 163, 74, 0.12);
}

.notification-mini-main {
  min-width: 0;
  flex: 1;
}

.notification-mini-main strong,
.notification-mini-main em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-mini-main strong {
  color: #172033;
  font-size: 14px;
}

.notification-mini-main em {
  margin-top: 4px;
  color: #66758b;
  font-size: 12px;
  font-style: normal;
}

.notification-mini-item time {
  color: #8391a5;
  font-size: 11px;
  white-space: nowrap;
}

.notification-empty-mini {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 140px;
  color: #8391a5;
  border: 1px dashed #d7e1ec;
  border-radius: 8px;
  background: #f8fbfe;
}

.notification-panel-foot {
  justify-content: space-between;
  gap: 8px;
  padding-top: 12px;
}

.notification-panel-foot button {
  flex: 1;
}

.notification-panel-foot button:disabled {
  color: #a8b3c2;
  cursor: not-allowed;
  background: #f4f7fa;
}
</style>
