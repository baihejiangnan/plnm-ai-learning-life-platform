<template>
  <main class="register-page">
    <section class="register-shell">
      <div class="brand-panel">
        <div class="brand-badge">PLNM ACCOUNT</div>
        <h1>创建你的生活中枢账号</h1>
        <p>注册后会自动准备标签、预算分类和欢迎通知，进入首页就能开始记录。</p>

        <div class="setup-list" aria-label="注册后自动初始化内容">
          <div class="setup-item">
            <span class="setup-icon">01</span>
            <div>
              <strong>默认标签</strong>
              <small>学习、生活、预算、复盘</small>
            </div>
          </div>
          <div class="setup-item">
            <span class="setup-icon">02</span>
            <div>
              <strong>预算分类</strong>
              <small>餐饮、交通、购物、娱乐等</small>
            </div>
          </div>
          <div class="setup-item">
            <span class="setup-icon">03</span>
            <div>
              <strong>欢迎通知</strong>
              <small>首页小铃铛会显示首次提醒</small>
            </div>
          </div>
        </div>
      </div>

      <div class="form-panel">
        <div class="form-header">
          <div>
            <span>注册</span>
            <h2>填写账号信息</h2>
          </div>
          <RouterLink to="/login">去登录</RouterLink>
        </div>

        <el-form
          ref="formRef"
          :model="data.form"
          :rules="data.rules"
          class="register-form"
          label-position="top"
          @keyup.enter="register"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model.trim="data.form.username"
              :prefix-icon="User"
              size="large"
              placeholder="3-32 位字母、数字或下划线"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item label="昵称" prop="name">
            <el-input
              v-model.trim="data.form.name"
              :prefix-icon="Postcard"
              size="large"
              placeholder="显示在顶部用户信息中"
              autocomplete="nickname"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="data.form.password"
              :prefix-icon="Lock"
              size="large"
              placeholder="至少 6 位"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="data.form.confirmPassword"
              :prefix-icon="Lock"
              size="large"
              placeholder="再次输入密码"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>

          <el-form-item label="验证码" prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model.trim="data.form.captchaCode"
                size="large"
                maxlength="4"
                placeholder="输入右侧字符"
                autocomplete="off"
              />
              <button
                class="captcha-image"
                type="button"
                :disabled="data.captchaLoading"
                title="点击刷新验证码"
                @click="refreshCaptcha"
              >
                <img v-if="data.captchaImage" :src="data.captchaImage" alt="验证码" />
                <span v-else>刷新</span>
              </button>
            </div>
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="register-button"
            :loading="data.submitting"
            @click="register"
          >
            创建账号并进入首页
          </el-button>
        </el-form>

        <p class="form-tip">验证码 5 分钟内有效，提交一次后会自动失效。</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { Lock, Postcard, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { captchaApi, userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== data.form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const data = reactive({
  submitting: false,
  captchaLoading: false,
  captchaImage: '',
  form: {
    username: '',
    name: '',
    password: '',
    confirmPassword: '',
    captchaId: '',
    captchaCode: ''
  },
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' },
      { min: 3, max: 32, message: '账号长度需为 3-32 位', trigger: 'blur' },
      { pattern: /^[A-Za-z0-9_]+$/, message: '账号只能包含字母、数字和下划线', trigger: 'blur' }
    ],
    name: [
      { required: true, message: '请输入昵称', trigger: 'blur' },
      { max: 32, message: '昵称不能超过 32 个字符', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 64, message: '密码长度需为 6-64 位', trigger: 'blur' }
    ],
    confirmPassword: [
      { validator: validateConfirmPassword, trigger: 'blur' }
    ],
    captchaCode: [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { min: 4, max: 4, message: '验证码为 4 位字符', trigger: 'blur' }
    ]
  }
})

const refreshCaptcha = async () => {
  try {
    data.captchaLoading = true
    const res = await captchaApi.getCaptcha()
    data.captchaImage = res?.data?.image || ''
    data.form.captchaId = res?.data?.captchaId || ''
    data.form.captchaCode = ''
  } catch (error) {
    ElMessage.error(error?.message || '验证码加载失败')
  } finally {
    data.captchaLoading = false
  }
}

const register = async () => {
  if (data.submitting) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    data.submitting = true
    const payload = {
      username: data.form.username,
      password: data.form.password,
      name: data.form.name,
      captchaId: data.form.captchaId,
      captchaCode: data.form.captchaCode
    }

    const res = await userApi.register(payload)
    if (!res || res.code !== 200) {
      throw new Error(res?.msg || '注册失败')
    }

    await userStore.login({
      username: data.form.username,
      password: data.form.password
    })
    await router.push('/home')
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || error?.message || '注册失败')
    await refreshCaptcha()
  } finally {
    data.submitting = false
  }
}

onMounted(refreshCaptcha)
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background:
    radial-gradient(circle at 16% 18%, rgba(37, 99, 235, 0.16), transparent 30%),
    radial-gradient(circle at 84% 8%, rgba(16, 185, 129, 0.16), transparent 30%),
    linear-gradient(135deg, #eef6ff 0%, #f8fafc 48%, #ecfdf5 100%);
  color: #0f172a;
}

.register-shell {
  width: min(980px, 100%);
  min-height: 620px;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  overflow: hidden;
  border: 1px solid #d8e2ed;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(18px);
}

.brand-panel {
  position: relative;
  padding: 48px;
  background:
    linear-gradient(145deg, rgba(15, 23, 42, 0.96), rgba(30, 64, 175, 0.88)),
    #0f172a;
  color: #fff;
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: auto 42px 42px auto;
  width: 180px;
  height: 180px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 50%;
}

.brand-badge {
  width: fit-content;
  margin-bottom: 32px;
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(219, 234, 254, 0.14);
  color: #bfdbfe;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.brand-panel h1 {
  max-width: 420px;
  margin: 0;
  font-size: 48px;
  line-height: 1.08;
  letter-spacing: 0;
}

.brand-panel p {
  max-width: 440px;
  margin: 20px 0 0;
  color: rgba(226, 232, 240, 0.82);
  font-size: 17px;
  line-height: 1.8;
}

.setup-list {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 14px;
  margin-top: 52px;
}

.setup-item {
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 420px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.08);
}

.setup-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 12px;
  background: #fff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 900;
}

.setup-item strong,
.setup-item small {
  display: block;
}

.setup-item strong {
  font-size: 16px;
}

.setup-item small {
  margin-top: 4px;
  color: rgba(226, 232, 240, 0.72);
}

.form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 42px;
}

.form-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.form-header span {
  color: #2563eb;
  font-size: 13px;
  font-weight: 900;
}

.form-header h2 {
  margin: 6px 0 0;
  font-size: 30px;
  letter-spacing: 0;
}

.form-header a {
  border: 1px solid #d8e2ed;
  border-radius: 12px;
  padding: 9px 13px;
  color: #0f172a;
  font-weight: 800;
  text-decoration: none;
}

.register-form :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 800;
}

.register-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px #d8e2ed inset;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 138px;
  gap: 10px;
  width: 100%;
}

.captcha-image {
  height: 46px;
  overflow: hidden;
  border: 1px solid #d8e2ed;
  border-radius: 14px;
  background: #f8fafc;
  cursor: pointer;
}

.captcha-image:disabled {
  cursor: wait;
  opacity: 0.7;
}

.captcha-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.register-button {
  width: 100%;
  min-height: 48px;
  margin-top: 4px;
  border: none;
  border-radius: 14px;
  background: #0f172a;
  font-weight: 900;
  letter-spacing: 0;
}

.register-button:hover {
  background: #1e293b;
}

.form-tip {
  margin: 18px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .register-page {
    padding: 18px;
  }

  .register-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    padding: 34px;
  }

  .brand-panel h1 {
    font-size: 34px;
  }

  .form-panel {
    padding: 30px;
  }
}
</style>
