<template>
  <div class="manager-container">
    <!-- 顶部导航栏 -->
    <header :class="['header', { 'is-scrolled': contentScrolled }]">
      <div class="header-left">
        <div class="logo-container">
          <div class="logo-icon">
            <el-icon size="28" color="#1890ff"><Document /></el-icon>
          </div>
          <h1 class="logo-text">  管理系统</h1>
        </div>
        
        <!-- 全局搜索 -->
        <div class="global-search">
          <el-input
            v-model="globalSearchKeyword"
            placeholder="搜索笔记、标签..."
            prefix-icon="Search"
            clearable
            @keyup.enter="handleGlobalSearch"
            class="search-input"
          />
        </div>
      </div>
      
      <div class="header-right">
        <!-- 快捷操作 -->
        <div class="quick-actions">
          <el-tooltip content="新建笔记" placement="bottom">
            <el-button circle @click="createNote" class="action-btn">
              <el-icon><Plus /></el-icon>
            </el-button>
          </el-tooltip>

          <el-tooltip content="AI快捷操作" placement="bottom">
            <el-button circle @click="openAiQuickDialog" class="action-btn ai-action-btn">
              <el-icon><MagicStick /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-tooltip content="通知" placement="bottom">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
              <el-button circle class="action-btn" @click="goNotificationSettings">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
          </el-tooltip>
        </div>
        
        <!-- 用户信息 -->
        <el-dropdown @command="handleUserCommand" class="user-dropdown">
          <div class="user-info">
            <img class="avatar" :src="user?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="avatar">
            <span class="username">{{ user?.name || user?.username }}</span>
            <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
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

    <!-- 主体内容区 -->
    <div class="main-container">
      <!-- 侧边栏 -->
      <aside class="sidebar" :class="{ 'sidebar-collapsed': isCollapse }">
        <!-- 侧边栏头部 -->
        <div class="sidebar-header">
          <el-button 
            link 
            @click="handleCollapse" 
            class="collapse-btn"
            :icon="isCollapse ? Expand : Fold"
          >
            <span v-if="!isCollapse">收起</span>
          </el-button>
        </div>
        
        <!-- 导航菜单 -->
        <el-menu
          router
          class="sidebar-menu"
          :default-active="router.currentRoute.value.path"
          :collapse="isCollapse"
          :collapse-transition="false"
        >
          <!-- 工作台 -->
          <!-- <div class="menu-group" v-if="!isCollapse">
            <div class="menu-group-title">工作台</div>
          </div> -->
          
          <el-menu-item index="/home" class="menu-item">
            <el-icon><HomeFilled /></el-icon>
            <span>概览</span>
          </el-menu-item>
                    <el-menu-item index="/ai/chat" class="menu-item">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI对话</span>
          </el-menu-item>
          <el-menu-item index="/ai/audit" class="menu-item">
            <el-icon><DataAnalysis /></el-icon>
            <span>AI审计日志</span>
          </el-menu-item>
          <!-- 笔记管理 -->
          <!-- <div class="menu-group" v-if="!isCollapse">
            <div class="menu-group-title">笔记管理</div>
          </div> -->
          
          <el-sub-menu index="notes">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>笔记中心</span>
            </template>
            <el-menu-item index="/notes/list">
              <el-icon><Document /></el-icon>
              <span>我的笔记</span>
            </el-menu-item>
            <el-menu-item index="/notes/editor">
              <el-icon><EditPen /></el-icon>
              <span>写笔记</span>
            </el-menu-item>
            <el-menu-item index="/notes/tags">
              <el-icon><PriceTag /></el-icon>
              <span>标签管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <!-- 个人中心 -->
          <!-- <div class="menu-group" v-if="!isCollapse">
            <div class="menu-group-title">个人中心</div>
          </div> -->
          

          <!-- 生活模块 -->
          <!-- <div class="menu-group" v-if="!isCollapse">
            <div class="menu-group-title">生活</div>
          </div> -->
          <el-sub-menu index="life">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>生活中心</span>
            </template>
            <el-menu-item index="/life/expenses" class="menu-item">
              <el-icon><Document /></el-icon>
              <span>消费记录</span>
            </el-menu-item>
            <el-menu-item index="/life/budgets" class="menu-item">
              <el-icon><PriceTag /></el-icon>
              <span>预算设置</span>
            </el-menu-item>
            <el-menu-item index="/life/categories" class="menu-item">
              <el-icon><PriceTag /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="learning">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>学习中心</span>
            </template>
            <el-menu-item index="/learning/courses">
              <el-icon><Collection /></el-icon>
              <span>课程分类</span>
            </el-menu-item>
            <el-menu-item index="/learning/progress">
              <el-icon><TrendCharts /></el-icon>
              <span>学习进度</span>
            </el-menu-item>
            <el-menu-item index="/learning/resources">
              <el-icon><Reading /></el-icon>
              <span>学习资源库</span>
            </el-menu-item>
          </el-sub-menu>





                  <el-sub-menu index="person">
            <template #title>
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </template>
            <el-menu-item index="/person/info" class="menu-item">
              <el-icon><User /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="/person/password" class="menu-item">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
          </el-sub-menu>



        </el-menu>
      </aside>

      <!-- 内容区 -->
      <main class="content" ref="contentRef" @scroll="handleContentScroll">
        <div class="content-wrapper">
          <router-view @updateUser="updateUser" />
        </div>
        <el-button
          v-show="showBackTop"
          class="back-to-top"
          type="primary"
          circle
          @click="scrollToTop"
          :icon="ArrowUp"
          title="回到顶部"
        />
      </main>
    </div>

    <el-dialog v-model="aiDialogVisible" title="AI 对话式快捷操作" width="560px" class="ai-quick-dialog">
      <div class="ai-guide">
        <div>示例：记一笔餐饮消费 35 元；把 Vue 课程进度加 10%；新建一篇复盘笔记</div>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'
import { 
  Fold, 
  Expand, 
  Document, 
  Search, 
  Plus, 
  Bell,
  User,
  Setting,
  SwitchButton,
  ArrowDown,
  HomeFilled,
  EditPen,
  PriceTag,
  Collection,
  TrendCharts,
  Reading,
  Lock,
  ArrowUp,
  MagicStick,
  ChatDotRound,
  DataAnalysis,
} from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { aiApi } from '@/api'
// removed duplicate icons import to avoid collision
// import { House, Tickets, Edit, User, Setting, Collection, TrendCharts, Reading } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 用户信息：使用 storeToRefs 确保响应式正确解包
const { userInfo } = storeToRefs(userStore)
const user = userInfo
// 全局搜索
const globalSearchKeyword = ref('')

// 处理全局搜索
const handleGlobalSearch = () => {
  if (globalSearchKeyword.value.trim()) {
    router.push({
      path: '/notes/list',
      query: { keyword: globalSearchKeyword.value }
    })
  }
}

// 创建笔记
const createNote = () => {
  router.push('/notes/editor')
}

// 处理用户下拉菜单命令
const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/person/info')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      logout()
      break
  }
}

// 退出登录
const logout = async () => {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      await userStore.logout()
      // 移除重复的路由跳转和成功提示，userStore.logout()中已经处理
    } catch (error) {
      if (error !== 'cancel') {
        console.error('退出登录失败:', error)
      }
    }
  }

// 侧边栏折叠
const isCollapse = ref(false)

const handleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 检查登录状态
onMounted(() => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
  }
})

const updateUser = async () => {
  try {
    await userStore.refreshUserInfo()
  } catch (e) {
    console.warn('updateUser: 刷新用户信息失败（已静默）', e?.message || e)
  }
}

const notificationStore = useNotificationStore()
const { unreadCount } = storeToRefs(notificationStore)

onMounted(async () => {
  try { await notificationStore.fetchList('unread', 1, 10) } catch (e) {}
})

const goNotificationSettings = () => {
  router.push({ path: '/settings', query: { tab: 'notification' } })
}

const contentRef = ref(null)
const contentScrolled = ref(false)
const showBackTop = ref(false)

const handleContentScroll = () => {
  const el = contentRef?.value
  if (!el) return
  const top = el.scrollTop || 0
  contentScrolled.value = top > 0
  showBackTop.value = top > 300
}

const scrollToTop = () => {
  const el = contentRef?.value
  if (!el) return
  el.scrollTo({ top: 0, behavior: 'smooth' })
}

const aiDialogVisible = ref(false)
const aiMessage = ref('')
const aiLoading = ref(false)
const aiResult = ref(null)

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

onMounted(() => {
  handleContentScroll()
})
</script>

<style scoped>
.manager-container {
  height: 100vh;
  overflow: hidden;
  background-color: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 顶部导航栏 */
.header {
  height: 64px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e8eaed;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.logo-text {
  color: #1f2937;
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  letter-spacing: -0.025em;
}

.global-search {
  width: 320px;
}

.search-input {
  border-radius: 8px;
}

.search-input :deep(.el-input__wrapper) {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: none;
  transition: all 0.2s;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #1890ff;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.quick-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
  color: #64748b;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
  background-color: #f0f9ff;
}

.ai-action-btn {
  border-color: #d9d2ff;
  color: #6751d6;
  background: #f7f4ff;
}

.ai-action-btn:hover {
  border-color: #7f6bf2;
  color: #5b44d6;
  background: #efe9ff;
}

.notification-badge :deep(.el-badge__content) {
  background-color: #ef4444;
  border: 2px solid #ffffff;
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.user-info:hover {
  background-color: #f8fafc;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid #e2e8f0;
  object-fit: cover;
}

.username {
  color: #374151;
  font-size: 14px;
  font-weight: 500;
}

.dropdown-icon {
  color: #9ca3af;
  font-size: 12px;
  transition: transform 0.2s;
}

.user-dropdown.is-active .dropdown-icon {
  transform: rotate(180deg);
}

/* 主体内容区 */
.main-container {
  display: flex;
  height: calc(100vh - 64px);
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 240px;
  background-color: #ffffff;
  border-right: 1px solid #e8eaed;
  transition: width 0.3s ease;
  overflow-y: auto;
  height: 100%;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}

.sidebar-collapsed {
  width: 64px;
}

.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f1f3f4;
  padding: 0 16px;
}

.collapse-btn {
  color: #5f6368;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background-color: #f8f9fa;
  color: #1a73e8;
}

.sidebar-menu {
  border: none;
  padding: 8px;
  background-color: transparent;
}

.menu-group {
  padding: 16px 16px 8px;
}

.menu-group-title {
  font-size: 12px;
  font-weight: 600;
  color: #5f6368;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.menu-item {
  margin: 2px 0;
  border-radius: 8px;
  transition: all 0.2s;
  font-size: 14px;
  height: 40px;
  line-height: 40px;
}

.menu-item :deep(.el-menu-item__title) {
  font-weight: 500;
}

.menu-item:hover {
  background-color: #f8f9fa !important;
  color: #1a73e8 !important;
}

.el-menu-item.is-active {
  background-color: #e8f0fe !important;
  color: #1a73e8 !important;
  font-weight: 600;
  position: relative;
}

.el-menu-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background-color: #1a73e8;
  border-radius: 0 2px 2px 0;
}

/* 内容区 */
.content {
  flex: 1;
  background-color: #f8fafc;
  overflow-y: auto;
  height: 100%;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}

.content-wrapper {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu) {
  border: 1px solid #e8eaed;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 8px;
}

:deep(.el-dropdown-menu__item) {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: #374151;
  transition: all 0.2s;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: #f8f9fa;
  color: #1a73e8;
}

:deep(.el-dropdown-menu__item.is-divided) {
  border-top: 1px solid #f1f3f4;
  margin-top: 4px;
  padding-top: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }
  
  .header-left {
    gap: 16px;
  }
  
  .global-search {
    width: 200px;
  }
  
  .logo-text {
    display: none;
  }
  
  .sidebar {
    width: 64px;
  }
  
  .content-wrapper {
    padding: 16px;
  }
}

/* 滚动条样式 */
:deep(.el-scrollbar__bar) {
  opacity: 0.3;
}

:deep(.el-scrollbar__thumb) {
  background-color: #cbd5e1;
  border-radius: 4px;
}

:deep(.el-scrollbar__thumb:hover) {
  background-color: #94a3b8;
}

/* 表格样式优化 */
:deep(th) {
  color: #374151;
  font-weight: 600;
  background-color: #f8fafc;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table__header) {
  background-color: #f8fafc;
}

/* 按钮样式优化 */
:deep(.el-button--primary) {
  background-color: #1890ff;
  border-color: #1890ff;
}

:deep(.el-button--primary:hover) {
  background-color: #40a9ff;
  border-color: #40a9ff;
}
/* 统一两侧滚动条视觉样式 */
.sidebar::-webkit-scrollbar,
.content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
.sidebar::-webkit-scrollbar-thumb,
.content::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 4px;
}
.sidebar::-webkit-scrollbar-thumb:hover,
.content::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}
/* Firefox */
.sidebar { scrollbar-width: thin; scrollbar-color: #cbd5e1 transparent; }
.content { scrollbar-width: thin; scrollbar-color: #cbd5e1 transparent; }

/* 滚动时 Header 阴影与轻微变色 */
.header.is-scrolled {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-bottom-color: #e2e8f0;
  background: #ffffff;
}

/* 悬浮回到顶部按钮样式 */
.back-to-top {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1200;
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.3);
}

.back-to-top:hover {
  box-shadow: 0 8px 20px rgba(24, 144, 255, 0.4);
}

.ai-guide {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ece9ff;
  background: #f8f6ff;
  color: #5c5192;
  font-size: 12px;
}

.ai-dialog-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.ai-result {
  margin-top: 14px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #dcecff;
  background: #f7fbff;
}

.ai-result-title {
  color: #2d4d93;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.ai-result-text {
  color: #334155;
  line-height: 1.6;
}

.ai-result-meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}
</style>
