<template>
  <div class="page-container settings-page">
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
        <el-tab-pane label="偏好设置" name="preferences">
          <el-form label-width="100px" class="pref-form" :model="prefForm">
            <el-form-item label="主题模式">
              <el-radio-group v-model="prefForm.theme" @change="onThemeChange">
                <el-radio-button label="light">浅色</el-radio-button>
                <el-radio-button label="dark">深色</el-radio-button>
                <el-radio-button label="auto">跟随系统</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="界面语言">
              <el-select v-model="prefForm.language" style="width: 220px" @change="onLanguageChange">
                <el-option label="简体中文" value="zh-cn" />
                <el-option label="English" value="en-us" />
              </el-select>
            </el-form-item>

            <el-form-item label="布局密度">
              <el-radio-group v-model="prefForm.uiSize" @change="onUiSizeChange">
                <el-radio-button label="small">紧凑</el-radio-button>
                <el-radio-button label="default">默认</el-radio-button>
                <el-radio-button label="large">宽松</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="通知设置" name="notification">
          <el-form label-width="100px" class="notif-form">
            <el-form-item label="启用通知">
              <el-switch v-model="notif.enabled" />
            </el-form-item>
            <el-form-item label="通知类别">
              <el-checkbox-group v-model="notif.categories">
                <el-checkbox label="system">系统</el-checkbox>
                <el-checkbox label="security">安全</el-checkbox>
                <el-checkbox label="collaboration">协作</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="notifSaving" @click="onSaveNotif">保存</el-button>
              <el-button @click="onResetNotif">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePreferenceStore } from '@/stores/preference'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api'
import { THEME_MODE, LANGUAGES, UI_SIZE, VALIDATION_RULES } from '@/constants'
import { useNotificationStore } from '@/stores/notification'
import { useRoute } from 'vue-router'

// 默认聚焦安全设置 Tab
const activeTab = ref('security')

// 偏好设置
const pref = usePreferenceStore()
const { theme, language, uiSize } = storeToRefs(pref)
const prefForm = reactive({
  theme: theme.value,
  language: language.value,
  uiSize: uiSize.value
})

const onThemeChange = (val) => {
  pref.setTheme(val)
  pref.applyThemeToDom()
}
const onLanguageChange = (val) => {
  pref.setLanguage(val)
  pref.applyLangToDom()
  location.reload()
}
const onUiSizeChange = (val) => {
  pref.setUiSize(val)
  location.reload()
}
watch(theme, v => prefForm.theme = v)
watch(language, v => prefForm.language = v)
watch(uiSize, v => prefForm.uiSize = v)

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
watch(() => route.query.tab, (v) => { if (v) activeTab.value = v }, { immediate: true })

const notificationStore = useNotificationStore()
const notifSaving = ref(false)
const notif = reactive({ enabled: true, categories: ['system','security','collaboration'] })

const loadNotif = async () => {
  try {
    const s = await notificationStore.fetchSettings()
    if (s) {
      notif.enabled = s.enabled ?? true
      notif.categories = Array.isArray(s.categories) ? s.categories : ['system','security','collaboration']
    }
  } catch (e) {}
}

const onSaveNotif = async () => {
  notifSaving.value = true
  try {
    const ok = await notificationStore.updateSettings({ enabled: notif.enabled, categories: notif.categories })
    if (ok) ElMessage.success('通知设置已保存')
    else ElMessage.error('保存失败')
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    notifSaving.value = false
  }
}

const onResetNotif = () => { loadNotif() }

onMounted(() => { loadNotif() })
</script>

<style scoped>
.page-container { padding: 16px; }
.card-header { font-weight: 600; }
.pref-form :deep(.el-form-item) { margin-bottom: 18px; }
.security-form :deep(.el-form-item) { max-width: 520px; }
.session-toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.ua { word-break: break-all; }
</style>

<style scoped>
.page-container { padding: 16px; }
.card-header { font-weight: 600; }
.pref-form :deep(.el-form-item) { margin-bottom: 18px; }
.security-form :deep(.el-form-item) { max-width: 520px; }
.section-title { font-weight: 600; margin: 4px 0 8px; }
.mb12 { margin-bottom: 12px; }
.mt12 { margin-top: 12px; }
.notif-form :deep(.el-form-item) { max-width: 520px; }
</style>