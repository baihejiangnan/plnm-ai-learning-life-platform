<template>
  <div class="page-container settings-page">
    <section class="page-header settings-hero">
      <div class="header-left">
        <el-icon class="title-icon"><Setting /></el-icon>
        <div>
          <h1 class="page-title">系统设置</h1>
          <p class="page-subtitle">统一管理安全、通知和当前设备会话。</p>
        </div>
      </div>
    </section>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>系统设置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 已移除 个人资料 Tab -->
        <el-tab-pane label="安全设置" name="security">
          <el-form
            ref="secFormRef"
            :model="secForm"
            :rules="secRules"
            label-width="100px"
            class="security-form"
          >
            <!-- 修改密码 -->
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="secForm.oldPassword"
                type="password"
                show-password
                autocomplete="current-password"
                placeholder="请输入当前密码"
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="secForm.newPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="请输入新密码（6-20位）"
              />
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="secForm.confirmPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="请再次输入新密码"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="onSubmitChangePassword">提交</el-button>
              <el-button @click="onResetSecurity">重置</el-button>
            </el-form-item>

            <el-divider />

            <!-- 设备与会话 -->
            <div class="section-title">设备与会话</div>

            <div class="session-toolbar">
              <el-button :loading="sessLoading" @click="fetchSessions">刷新</el-button>
              <el-button type="danger" plain :loading="kickingAll" @click="onLogoutAllSessions">
                下线所有会话
              </el-button>
            </div>

            <el-table :data="sessions" v-loading="sessLoading" size="small" class="mb12">
              <el-table-column prop="device" label="设备/浏览器" min-width="240">
                <template #default="{ row }">
                  <span class="ua">{{ row.device || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="ip" label="IP地址" width="140" />
              <el-table-column label="登录时间" width="180">
                <template #default="{ row }">{{ formatTime(row.loginTime) }}</template>
              </el-table-column>
              <el-table-column label="过期时间" width="180">
                <template #default="{ row }">{{ formatTime(row.expireTime) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag type="success" v-if="row.current">当前设备</el-tag>
                  <el-tag v-else>其他</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140">
                <template #default="{ row }">
                  <el-button size="small" type="danger" plain :disabled="row.current" @click="onKickSession(row)">下线</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-if="!sessLoading && (!sessions || sessions.length === 0)" description="暂无会话记录" />

            <el-pagination
              v-if="sessTotal > sessSize"
              class="mt12"
              background
              layout="prev, pager, next, sizes, total"
              :total="sessTotal"
              v-model:current-page="sessPage"
              v-model:page-size="sessSize"
              @current-change="onSessPageChange"
              @size-change="onSessSizeChange"
            />

            <el-alert
              title="提示"
              type="info"
              :closable="false"
              description="该操作会让所有设备的登录态失效（包括当前设备），执行后将跳转至登录页。"
              show-icon
              class="mt12"
            />

            <el-divider />

            <el-alert
              title="安全建议"
              type="info"
              :closable="false"
              description="建议使用包含大小写字母、数字与符号的强密码，并避免与其他网站相同。"
              show-icon
            />
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="通知设置" name="notification">
          <section class="notification-studio">
            <div class="notification-settings-grid">
              <section class="settings-block is-main">
                <div class="block-title compact">
                  <div>
                    <h3>最近通知</h3>
                    <p>从后端通知概览读取，用于顶部小铃铛预览。</p>
                  </div>
                  <div class="block-actions">
                    <el-button :loading="notifLoading" @click="refreshNotificationList">
                      <el-icon><RefreshRight /></el-icon>
                      刷新
                    </el-button>
                    <el-button :disabled="notifUnreadCount === 0" @click="readAllNotifications">全部已读</el-button>
                  </div>
                </div>

                <div v-loading="notifLoading" class="notification-feed">
                  <button
                    v-for="item in notifPreviewList"
                    :key="item.id"
                    :class="['notification-row', { unread: item.status === 'unread' }]"
                    type="button"
                    @click="openNotification(item)"
                  >
                    <span :class="['row-dot', `is-${item.type || 'system'}`]"></span>
                    <span class="row-copy">
                      <strong>{{ item.title }}</strong>
                      <em>{{ item.content }}</em>
                    </span>
                    <span class="row-meta">{{ formatNotifTime(item.createTime) }}</span>
                  </button>
                  <el-empty v-if="!notifLoading && notifPreviewList.length === 0" description="暂无通知" />
                </div>
              </section>

              <aside class="settings-block">
                <div class="block-title compact">
                  <div>
                    <h3>接收方式</h3>
                    <p>按场景控制提醒强度。</p>
                  </div>
                </div>
                <div class="delivery-list">
                  <div class="delivery-item">
                    <div>
                      <strong>桌面提醒</strong>
                      <span>浏览器允许后显示系统级提醒</span>
                    </div>
                    <el-switch v-model="notif.desktop" :disabled="!notif.enabled" />
                  </div>
                  <div class="delivery-item">
                    <div>
                      <strong>提示音</strong>
                      <span>收到高优先级消息时轻提示</span>
                    </div>
                    <el-switch v-model="notif.sound" :disabled="!notif.enabled" />
                  </div>
                  <div class="delivery-item">
                    <div>
                      <strong>仅高优先级</strong>
                      <span>首页角标只保留安全和待处理项</span>
                    </div>
                    <el-switch v-model="notif.priorityOnly" :disabled="!notif.enabled" />
                  </div>
                  <div class="delivery-item">
                    <div>
                      <strong>每日摘要</strong>
                      <span>按固定时间汇总当天消息</span>
                    </div>
                    <el-switch v-model="notif.emailDigest" :disabled="!notif.enabled" />
                  </div>
                  <div class="delivery-time">
                    <span>摘要时间</span>
                    <el-input v-model="notif.digestTime" placeholder="21:30" :disabled="!notif.emailDigest" />
                  </div>
                </div>
              </aside>
            </div>

            <div class="notification-settings-grid lower">
              <section class="settings-block">
                <div class="block-title">
                  <div>
                    <h3>提醒策略</h3>
                    <p>这些设置会影响顶部小铃铛角标、站内提醒和摘要行为。</p>
                  </div>
                  <el-switch
                    v-model="notif.enabled"
                    size="large"
                    active-text="开启"
                    inactive-text="关闭"
                  />
                </div>

                <div class="category-picker">
                  <button
                    v-for="item in categoryOptions"
                    :key="item.key"
                    type="button"
                    :class="['category-card', `is-${item.key}`, { active: notif.categories.includes(item.key) }]"
                    @click="toggleNotificationCategory(item.key)"
                  >
                    <span class="category-icon">{{ item.short }}</span>
                    <span class="category-copy">
                      <strong>{{ item.label }}</strong>
                      <em>{{ item.description }}</em>
                    </span>
                    <span class="category-count">{{ categoryCount(item.key) }}</span>
                  </button>
                </div>
              </section>

              <aside class="settings-block preview-block">
                <div class="block-title compact">
                  <div>
                    <h3>首页角标预览</h3>
                    <p>保存后顶部铃铛会使用相同未读数据。</p>
                  </div>
                </div>
                <div class="bell-preview">
                  <div :class="['preview-bell', { muted: !notif.enabled }]">
                    <el-icon><Bell /></el-icon>
                    <span v-if="notifUnreadCount > 0">{{ notifUnreadCount }}</span>
                  </div>
                  <div>
                    <strong>{{ notif.enabled ? '通知已开启' : '通知已关闭' }}</strong>
                    <p>{{ notifStatusText }}</p>
                  </div>
                </div>
                <div class="quiet-card">
                  <div>
                    <strong>安静时段</strong>
                    <span>22:30 - 08:00 不弹出干扰提醒，只保留角标。</span>
                  </div>
                  <el-switch v-model="notif.quietHours" :disabled="!notif.enabled" />
                </div>
              </aside>
            </div>

            <div class="notification-savebar">
              <div>
                <strong>本地策略会同步到后端内存设置</strong>
                <span>刷新页面后仍会从 `/api/notifications/settings` 读取。</span>
              </div>
              <div class="savebar-actions">
                <el-button @click="onResetNotif">重置</el-button>
                <el-button type="primary" :loading="notifSaving" @click="onSaveNotif">保存通知设置</el-button>
              </div>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, reactive, watch, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api'
import { VALIDATION_RULES } from '@/constants'
import { useNotificationStore } from '@/stores/notification'
import { useRoute, useRouter } from 'vue-router'
import { Bell, RefreshRight, Setting } from '@element-plus/icons-vue'

// 默认聚焦安全设置 Tab
const activeTab = ref('security')

// 安全设置：修改密码
const userStore = useUserStore()
const saving = ref(false)
const kickingAll = ref(false)
const secFormRef = ref(null)
const secForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== secForm.newPassword) return callback(new Error('两次输入的密码不一致'))
  callback()
}

const secRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在6-20个字符之间', trigger: 'blur' }
  ],
  newPassword: [VALIDATION_RULES.PASSWORD],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

const onResetSecurity = () => {
  secForm.oldPassword = ''
  secForm.newPassword = ''
  secForm.confirmPassword = ''
}

const onSubmitChangePassword = () => {
  secFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { oldPassword: secForm.oldPassword, newPassword: secForm.newPassword }
      const res = await userApi.changePassword(payload)
      if (res && res.code === 200) {
        ElMessage.success(res.msg || '密码修改成功，请重新登录')
        // 安全起见，修改密码后强制登出
        await userStore.logout(false)
        ElMessage.success('请使用新密码重新登录')
      } else {
        ElMessage.error(res?.msg || '密码修改失败')
      }
    } catch (e) {
      ElMessage.error(e?.message || '密码修改失败')
    } finally {
      saving.value = false
    }
  })
}

// 设备与会话：列表与分页
const sessLoading = ref(false)
const sessions = ref([])
const sessPage = ref(1)
const sessSize = ref(10)
const sessTotal = ref(0)

const fetchSessions = async () => {
  if (activeTab.value !== 'security') return
  sessLoading.value = true
  try {
    const res = await userApi.getSessions({ page: sessPage.value, size: sessSize.value })
    if (res && res.code === 200) {
      const data = res.data || {}
      sessions.value = data.list || []
      sessTotal.value = data.total || 0
    } else {
      ElMessage.error(res?.msg || '获取会话列表失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取会话列表失败')
  } finally {
    sessLoading.value = false
  }
}

const onSessPageChange = (p) => {
  sessPage.value = p
  fetchSessions()
}
const onSessSizeChange = (s) => {
  sessSize.value = s
  sessPage.value = 1
  fetchSessions()
}

const formatTime = (ts) => {
  if (!ts) return '-'
  try {
    return new Date(ts).toLocaleString()
  } catch (e) {
    return '-'
  }
}

const onKickSession = (row) => {
  // 预留：需要后端会话存储/黑名单支持
  ElMessage.warning('暂未接入会话存储，无法下线单个会话')
}

watch(activeTab, (val) => {
  if (val === 'security' && (!sessions.value || sessions.value.length === 0)) {
    fetchSessions()
  }
}, { immediate: true })

const onLogoutAllSessions = async () => {
  try {
    await ElMessageBox.confirm(
      '该操作会让所有设备的登录态失效（包括当前设备），确认继续吗？',
      '下线所有会话',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    kickingAll.value = true
    const res = await userApi.logoutAllSessions()
    if (res && res.code === 200) {
      ElMessage.success(res.msg || '已下线所有会话')
      await userStore.logout(false)
    } else {
      ElMessage.error(res?.msg || '下线会话失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '下线会话失败')
  } finally {
    kickingAll.value = false
  }
}

const route = useRoute()
const router = useRouter()
watch(() => route.query.tab, (v) => {
  activeTab.value = ['security', 'notification'].includes(v) ? v : 'security'
}, { immediate: true })

const notificationStore = useNotificationStore()
const notifSaving = ref(false)
const { list: notifList, unreadCount: notifUnreadCount, loading: notifLoading } = storeToRefs(notificationStore)
const notif = reactive({
  enabled: true,
  desktop: true,
  emailDigest: false,
  quietHours: false,
  sound: true,
  priorityOnly: false,
  digestTime: '21:30',
  categories: ['system', 'security', 'collaboration']
})

const categoryOptions = [
  { key: 'system', label: '系统通知', short: 'S', description: '更新、同步、任务状态' },
  { key: 'security', label: '安全通知', short: 'A', description: '登录、密码、账号风险' },
  { key: 'collaboration', label: '协作通知', short: 'AI', description: 'AI 审计、复盘、建议' }
]

const notifPreviewList = computed(() => (notifList.value || []).slice(0, 8))
const notifStatusText = computed(() => {
  if (!notif.enabled) return '顶部角标会隐藏，最近通知仍可在此查看。'
  if (notif.priorityOnly) return '当前仅高优先级消息会推送到首页角标。'
  if (notif.quietHours) return '安静时段内保留角标，不主动弹出提醒。'
  return '所有已启用分类都会参与顶部角标和通知预览。'
})

const categoryCount = (type) => (notifList.value || []).filter(item => item.type === type).length

const toggleNotificationCategory = (key) => {
  if (notif.categories.includes(key)) {
    if (notif.categories.length === 1) {
      ElMessage.warning('至少保留一个通知分类')
      return
    }
    notif.categories = notif.categories.filter(item => item !== key)
    return
  }
  notif.categories = [...notif.categories, key]
}

const applyNotificationSettings = (settings = {}) => {
  notif.enabled = settings.enabled ?? true
  notif.desktop = settings.desktop ?? true
  notif.emailDigest = settings.emailDigest ?? false
  notif.quietHours = settings.quietHours ?? false
  notif.sound = settings.sound ?? true
  notif.priorityOnly = settings.priorityOnly ?? false
  notif.digestTime = settings.digestTime || '21:30'
  notif.categories = Array.isArray(settings.categories)
    ? settings.categories
    : ['system', 'security', 'collaboration']
}

const refreshNotificationList = async () => {
  try {
    await notificationStore.fetchOverview()
  } catch (e) {
    await notificationStore.fetchList('all', 1, 20)
  }
}

const loadNotif = async () => {
  try {
    const s = await notificationStore.fetchSettings()
    if (s) applyNotificationSettings(s)
    await refreshNotificationList()
  } catch (e) {}
}

const onSaveNotif = async () => {
  notifSaving.value = true
  try {
    const ok = await notificationStore.updateSettings({
      enabled: notif.enabled,
      desktop: notif.desktop,
      emailDigest: notif.emailDigest,
      quietHours: notif.quietHours,
      sound: notif.sound,
      priorityOnly: notif.priorityOnly,
      digestTime: notif.digestTime,
      categories: notif.categories
    })
    if (ok) ElMessage.success('通知设置已保存')
    else ElMessage.error('保存失败')
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    notifSaving.value = false
  }
}

const onResetNotif = () => { loadNotif() }

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
  if (target) router.push(target)
}

const readAllNotifications = async () => {
  const ids = (notifList.value || [])
    .filter(item => item.status === 'unread')
    .map(item => item.id)
  if (!ids.length) return
  const ok = await notificationStore.markRead(ids)
  if (ok) {
    ElMessage.success('已处理当前未读通知')
    await refreshNotificationList()
  }
}

const formatNotifTime = (value) => {
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

onMounted(() => { loadNotif() })
</script>

<style scoped>
.page-container {
  padding: 0;
}

.settings-page :deep(.el-card) {
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 42px rgba(33, 54, 86, 0.06);
}

.settings-page :deep(.el-card__header) {
  display: none;
}

.settings-page :deep(.el-card__body) {
  padding: 0;
}

.settings-page :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 18px;
  border-bottom: 1px solid #e2ebf4;
  background: #fbfdff;
}

.settings-page :deep(.el-tabs__content) {
  padding: 18px;
}

.settings-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding: 24px 28px;
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(31, 111, 235, 0.12), rgba(39, 184, 131, 0.1)),
    #f8fbff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: #1f6feb;
  background: #e8f2ff;
}

.page-title {
  margin: 0;
  color: #101827;
  font-size: 28px;
  line-height: 1.15;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #66758b;
}

.card-header {
  font-weight: 700;
}

.security-form :deep(.el-form-item) {
  max-width: 520px;
}

.session-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.ua {
  word-break: break-all;
}

.section-title {
  margin: 4px 0 8px;
  font-weight: 700;
}

.mb12 {
  margin-bottom: 12px;
}

.mt12 {
  margin-top: 12px;
}

.notification-studio {
  display: grid;
  gap: 16px;
}

.notification-settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.95fr);
  gap: 16px;
}

.notification-settings-grid.lower {
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.7fr);
}

.settings-block {
  min-width: 0;
  padding: 18px;
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: #ffffff;
}

.settings-block.is-main {
  background:
    linear-gradient(180deg, rgba(31, 111, 235, 0.04), transparent 42%),
    #ffffff;
}

.block-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.block-title.compact {
  align-items: center;
}

.block-title h3 {
  margin: 0;
  color: #101827;
  font-size: 18px;
}

.block-title p {
  margin: 6px 0 0;
  color: #66758b;
  line-height: 1.6;
}

.block-actions {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}

.category-picker {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.category-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 96px;
  padding: 14px;
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: #f9fbfd;
  color: #172033;
  text-align: left;
  cursor: pointer;
}

.category-card.active {
  border-color: #9fc5ff;
  background: #edf5ff;
  box-shadow: inset 0 0 0 1px rgba(31, 111, 235, 0.18);
}

.category-icon {
  display: inline-grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  color: #ffffff;
  background: #1f6feb;
  font-weight: 850;
}

.category-card.is-security .category-icon {
  background: #ef4444;
}

.category-card.is-collaboration .category-icon {
  background: #16a34a;
}

.category-copy,
.category-copy strong,
.category-copy em {
  min-width: 0;
  display: block;
}

.category-copy strong {
  font-size: 15px;
}

.category-copy em {
  margin-top: 6px;
  color: #66758b;
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  color: #1557c0;
  background: #dcecff;
  font-weight: 850;
}

.delivery-list {
  display: grid;
  gap: 10px;
}

.delivery-item,
.delivery-time,
.quiet-card,
.bell-preview,
.notification-row,
.notification-savebar {
  display: flex;
  align-items: center;
}

.delivery-item,
.delivery-time,
.quiet-card {
  justify-content: space-between;
  gap: 14px;
  min-height: 64px;
  padding: 12px;
  border: 1px solid #e0e8f1;
  border-radius: 8px;
  background: #f9fbfd;
}

.delivery-item strong,
.delivery-item span,
.quiet-card strong,
.quiet-card span {
  display: block;
}

.delivery-item strong,
.quiet-card strong {
  color: #172033;
}

.delivery-item span,
.quiet-card span {
  margin-top: 4px;
  color: #66758b;
  font-size: 12px;
  line-height: 1.5;
}

.delivery-time :deep(.el-input) {
  width: 104px;
}

.notification-feed {
  display: grid;
  gap: 10px;
  min-height: 260px;
}

.notification-row {
  width: 100%;
  gap: 12px;
  min-height: 72px;
  padding: 12px;
  border: 1px solid #e0e8f1;
  border-radius: 8px;
  background: #ffffff;
  color: #172033;
  text-align: left;
  cursor: pointer;
}

.notification-row:hover {
  border-color: #9fc5ff;
  background: #f7fbff;
}

.notification-row.unread {
  border-color: #b9d6ff;
  background: #edf5ff;
}

.row-dot {
  width: 10px;
  height: 10px;
  flex: 0 0 10px;
  border-radius: 50%;
  background: #1f6feb;
  box-shadow: 0 0 0 5px rgba(31, 111, 235, 0.12);
}

.row-dot.is-security {
  background: #ef4444;
  box-shadow: 0 0 0 5px rgba(239, 68, 68, 0.12);
}

.row-dot.is-collaboration {
  background: #16a34a;
  box-shadow: 0 0 0 5px rgba(22, 163, 74, 0.12);
}

.row-copy {
  min-width: 0;
  flex: 1;
}

.row-copy strong,
.row-copy em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-copy strong {
  font-size: 14px;
}

.row-copy em {
  margin-top: 5px;
  color: #66758b;
  font-size: 12px;
  font-style: normal;
}

.row-meta {
  color: #8391a5;
  font-size: 12px;
  white-space: nowrap;
}

.preview-block {
  display: grid;
  align-content: start;
  gap: 12px;
}

.bell-preview {
  gap: 14px;
  padding: 16px;
  border: 1px solid #cfe0f2;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(31, 111, 235, 0.08), rgba(39, 184, 131, 0.1)),
    #f8fbff;
}

.preview-bell {
  position: relative;
  display: inline-grid;
  place-items: center;
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  border-radius: 8px;
  color: #ffffff;
  background: #1f6feb;
  box-shadow: 0 14px 28px rgba(31, 111, 235, 0.28);
}

.preview-bell.muted {
  background: #94a3b8;
  box-shadow: none;
}

.preview-bell span {
  position: absolute;
  top: -7px;
  right: -7px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border: 2px solid #ffffff;
  border-radius: 999px;
  background: #ef4444;
  color: #ffffff;
  font-size: 12px;
  font-weight: 850;
  line-height: 18px;
  text-align: center;
}

.bell-preview strong,
.bell-preview p {
  display: block;
  margin: 0;
}

.bell-preview p {
  margin-top: 6px;
  color: #66758b;
  line-height: 1.6;
}

.notification-savebar {
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #cfe0f2;
  border-radius: 8px;
  background: #f8fbff;
}

.notification-savebar strong,
.notification-savebar span {
  display: block;
}

.notification-savebar span {
  margin-top: 4px;
  color: #66758b;
  font-size: 12px;
}

.savebar-actions {
  display: flex;
  gap: 10px;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .notification-settings-grid,
  .notification-settings-grid.lower {
    grid-template-columns: 1fr;
  }

  .category-picker {
    grid-template-columns: 1fr;
  }

  .notification-savebar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
