<template>
  <div class="account-center">
    <section class="account-overview">
      <div class="identity-block">
        <el-upload
          ref="uploadRef"
          :show-file-list="false"
          class="avatar-uploader"
          :action="uploadUrl"
          :on-change="onFileChange"
          :auto-upload="false"
          accept="image/*"
        >
          <button class="avatar-button" type="button" aria-label="更换头像">
            <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar" alt="用户头像">
            <el-icon v-else class="avatar-fallback"><Plus /></el-icon>
            <span>更换头像</span>
          </button>
        </el-upload>

        <div class="identity-copy">
          <span class="eyebrow">ACCOUNT PROFILE</span>
          <h2>{{ profileForm.name || profileForm.username || '用户' }}</h2>
          <p>{{ profileForm.username || '未读取到账户名' }}</p>
        </div>
      </div>

      <div class="account-status">
        <div>
          <strong>资料状态</strong>
          <span>{{ profileCompleteText }}</span>
        </div>
        <div>
          <strong>登录安全</strong>
          <span>修改密码后需要重新登录</span>
        </div>
      </div>
    </section>

    <section class="account-grid">
      <article class="account-panel">
        <div class="panel-heading">
          <el-icon><User /></el-icon>
          <div>
            <h3>基础资料</h3>
            <p>维护头像和昵称，顶部导航会同步显示最新信息。</p>
          </div>
        </div>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-position="top"
          class="account-form"
        >
          <el-form-item label="账号">
            <el-input v-model="profileForm.username" disabled />
          </el-form-item>
          <el-form-item label="名称" prop="name">
            <el-input v-model="profileForm.name" maxlength="20" show-word-limit placeholder="请输入显示名称" />
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
          </div>
        </el-form>
      </article>

      <article ref="passwordPanelRef" class="account-panel security-panel">
        <div class="panel-heading">
          <el-icon><Lock /></el-icon>
          <div>
            <h3>登录安全</h3>
            <p>建议定期更新密码，避免和其他网站使用相同密码。</p>
          </div>
        </div>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          class="account-form"
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="请输入当前密码"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入 6-20 位新密码"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请再次输入新密码"
            />
          </el-form-item>

          <div class="security-note">
            <el-icon><CircleCheck /></el-icon>
            <span>密码修改成功后会清理当前登录态，并跳转到登录页重新认证。</span>
          </div>

          <div class="form-actions">
            <el-button @click="resetPasswordForm">清空</el-button>
            <el-button type="primary" :loading="passwordSaving" @click="changePassword">更新密码</el-button>
          </div>
        </el-form>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { CircleCheck, Lock, Plus, User } from '@element-plus/icons-vue'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const emit = defineEmits(['updateUser'])
const route = useRoute()
const userStore = useUserStore()
const uploadUrl = computed(() => `${import.meta.env.VITE_BASE_URL}/files/upload`)
const uploadRef = ref(null)
const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const passwordPanelRef = ref(null)
const profileSaving = ref(false)
const passwordSaving = ref(false)

const storedUser = (() => {
  try {
    const stored = localStorage.getItem('system-user')
    return stored && stored !== 'undefined' && stored !== 'null' ? JSON.parse(stored) : {}
  } catch (e) {
    console.warn('解析用户数据失败:', e)
    return {}
  }
})()

const profileForm = reactive({
  id: storedUser.id,
  username: storedUser.username || '',
  name: storedUser.name || '',
  avatar: storedUser.avatar || ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileCompleteText = computed(() => {
  const done = [profileForm.avatar, profileForm.name].filter(Boolean).length
  return `${done}/2 已完善`
})

const profileRules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { min: 1, max: 20, message: '名称长度应在 1-20 个字符之间', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== passwordForm.newPassword) return callback(new Error('两次输入的新密码不一致'))
  callback()
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在 6-20 个字符之间', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码长度应在 6-20 个字符之间', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && value === passwordForm.oldPassword) {
          return callback(new Error('新密码不能与当前密码相同'))
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const OUTPUT_SIZE = 300
const OUTPUT_MIME = 'image/jpeg'
const OUTPUT_QUALITY = 0.9
const MAX_SIZE_MB = 2
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

const onFileChange = async (file) => {
  const raw = file?.raw || file
  if (!raw) return false

  const ok = await onBeforeUpload(raw)
  if (uploadRef.value) {
    try { uploadRef.value.clearFiles() } catch (_) {}
  }
  if (!ok) return false

  try {
    const blob = await centerCropAndResize(raw, OUTPUT_SIZE, OUTPUT_MIME, OUTPUT_QUALITY)
    const form = new FormData()
    const uploadFile = new File([blob], 'avatar.jpg', { type: OUTPUT_MIME })
    form.append('file', uploadFile)

    const res = await request.post(uploadUrl.value, form)
    if (res && (res.code === 200 || res.code === '200')) {
      profileForm.avatar = res.data
      await saveProfile({ silent: true, skipValidate: true })
      ElMessage.success('头像已更新')
    } else {
      ElMessage.error(res?.msg || '上传失败')
    }
  } catch (e) {
    console.error('头像处理失败:', e)
    ElMessage.error(e?.message || '头像处理失败，请重试')
  }
  return false
}

const onBeforeUpload = async (file) => {
  const type = file.type || ''
  const sizeMB = file.size / 1024 / 1024
  if (!ALLOWED_TYPES.includes(type)) {
    ElMessage.error('仅支持 JPG/PNG/WebP 格式的图片')
    return false
  }
  if (sizeMB > MAX_SIZE_MB) {
    ElMessage.error(`图片大小不能超过 ${MAX_SIZE_MB}MB`)
    return false
  }
  return true
}

const centerCropAndResize = (file, outSize = 300, mime = 'image/jpeg', quality = 0.9) => {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      try {
        const s = Math.min(img.width, img.height)
        const sx = Math.max(0, (img.width - s) / 2)
        const sy = Math.max(0, (img.height - s) / 2)
        const canvas = document.createElement('canvas')
        canvas.width = outSize
        canvas.height = outSize
        const ctx = canvas.getContext('2d')
        ctx.imageSmoothingEnabled = true
        ctx.imageSmoothingQuality = 'high'
        ctx.drawImage(img, sx, sy, s, s, 0, 0, outSize, outSize)
        canvas.toBlob((blob) => {
          URL.revokeObjectURL(url)
          if (!blob) return reject(new Error('图片处理失败'))
          resolve(blob)
        }, mime, quality)
      } catch (err) {
        URL.revokeObjectURL(url)
        reject(err)
      }
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片加载失败'))
    }
    img.src = url
  })
}

const saveProfile = async (options = {}) => {
  if (!profileForm.id) {
    ElMessage.error('用户信息缺失，请重新登录')
    return
  }

  if (!options.skipValidate) {
    const valid = await profileFormRef.value?.validate().catch(() => false)
    if (!valid) return
  }

  profileSaving.value = true
  try {
    const res = await userApi.updateInfo({
      name: profileForm.name,
      avatar: profileForm.avatar
    })
    if (res.code === 200 || res.code === '200') {
      const updated = res.data || {}
      profileForm.name = updated.name ?? profileForm.name
      profileForm.avatar = updated.avatar ?? profileForm.avatar
      userStore.updateUser({
        name: profileForm.name,
        avatar: profileForm.avatar
      })
      userStore.refreshUserInfo().catch((e) => {
        console.warn('刷新用户信息失败（已静默）:', e?.message || e)
      })
      if (!options.silent) ElMessage.success('个人资料已保存')
      emit('updateUser')
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  } finally {
    profileSaving.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const changePassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordSaving.value = true
  try {
    const res = await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.code === 200 || res.code === '200') {
      ElMessage.success('密码已更新，请重新登录')
      resetPasswordForm()
      await userStore.logout(false)
    } else {
      ElMessage.error(res.msg || '修改密码失败')
    }
  } catch (err) {
    ElMessage.error(err?.message || '修改密码失败')
  } finally {
    passwordSaving.value = false
  }
}

const scrollToPasswordPanel = async () => {
  if (route.query.section !== 'password') return
  await nextTick()
  passwordPanelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(() => route.query.section, scrollToPasswordPanel, { immediate: true })
</script>

<style scoped>
.account-center {
  display: grid;
  gap: 16px;
}

.account-overview,
.account-panel {
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(33, 54, 86, 0.06);
}

.account-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px;
  background:
    linear-gradient(135deg, rgba(31, 111, 235, 0.08), rgba(39, 184, 131, 0.08)),
    #ffffff;
}

.identity-block {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.avatar-button {
  position: relative;
  display: inline-grid;
  place-items: center;
  width: 92px;
  height: 92px;
  border: 1px solid #cfe0f2;
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
  background: #eef5ff;
  cursor: pointer;
}

.avatar,
.avatar-fallback {
  width: 100%;
  height: 100%;
}

.avatar {
  object-fit: cover;
}

.avatar-fallback {
  display: grid;
  place-items: center;
  color: #1f6feb;
  font-size: 28px;
}

.avatar-button span {
  position: absolute;
  inset: auto 0 0;
  padding: 6px 4px;
  color: #ffffff;
  background: rgba(15, 23, 42, 0.72);
  font-size: 12px;
  font-weight: 700;
  opacity: 0;
  transition: opacity 160ms ease;
}

.avatar-button:hover span {
  opacity: 1;
}

.identity-copy {
  min-width: 0;
}

.eyebrow {
  color: #0f766e;
  font-size: 12px;
  font-weight: 850;
}

.identity-copy h2 {
  margin: 8px 0 4px;
  color: #101827;
  font-size: 28px;
  line-height: 1.1;
}

.identity-copy p {
  margin: 0;
  color: #66758b;
}

.account-status {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 10px;
}

.account-status div {
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid #d7e1ec;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
}

.account-status strong,
.account-status span {
  display: block;
}

.account-status strong {
  color: #172033;
}

.account-status span {
  margin-top: 8px;
  color: #66758b;
  font-size: 12px;
}

.account-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 16px;
}

.account-panel {
  padding: 20px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-heading > .el-icon {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 8px;
  color: #1f6feb;
  background: #e8f2ff;
}

.security-panel .panel-heading > .el-icon {
  color: #0f766e;
  background: #dcf7ed;
}

.panel-heading h3 {
  margin: 0;
  color: #101827;
  font-size: 19px;
}

.panel-heading p {
  margin: 6px 0 0;
  color: #66758b;
  line-height: 1.6;
}

.account-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.account-form :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 8px;
}

.security-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 2px 0 18px;
  padding: 10px 12px;
  border: 1px solid #cfe0f2;
  border-radius: 8px;
  color: #526276;
  background: #f8fbff;
  line-height: 1.6;
}

.security-note .el-icon {
  margin-top: 3px;
  color: #16a34a;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

.avatar-uploader :deep(.el-upload) {
  display: block;
}

@media (max-width: 1100px) {
  .account-overview,
  .account-grid {
    grid-template-columns: 1fr;
  }

  .account-overview {
    display: grid;
  }
}
</style>
