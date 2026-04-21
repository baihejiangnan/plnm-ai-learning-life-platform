<template>
  <div class="password-container">
    <div class="password-card card">
      <h2 class="card-title">修改密码</h2>
      <el-form :model="data.user" label-width="120px">
        <el-form-item label="原密码">
          <el-input 
            v-model="data.user.password" 
            show-password
            class="custom-input"
            placeholder="请输入原密码"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input 
            v-model="data.user.newPassword" 
            show-password
            class="custom-input"
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input 
            v-model="data.user.confirmPassword" 
            show-password
            class="custom-input"
            placeholder="请再次输入新密码"
          />
        </el-form-item>
        <div class="action-section">
          <el-button 
            type="primary" 
            @click="save"
            class="save-button"
          >
            确认修改
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import {reactive} from "vue"
import { ElMessage } from "element-plus";
import router from "@/router";
import { userApi } from "@/api";

const data = reactive({
  user: (() => {
    try {
      const stored = localStorage.getItem('system-user')
      if (!stored || stored === 'undefined' || stored === 'null') {
        return {}
      }
      return JSON.parse(stored)
    } catch (e) {
      console.warn('解析用户数据失败:', e)
      return {}
    }
  })()
})

const save = () => {
  // 基础校验
  if (!data.user.password) {
    ElMessage.error('请输入原密码')
    return
  }
  if (!data.user.newPassword) {
    ElMessage.error('请输入新密码')
    return
  }
  if (data.user.newPassword !== data.user.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  if (data.user.newPassword === data.user.password) {
    ElMessage.error('新密码不能与原密码相同')
    return
  }

  const payload = {
    oldPassword: data.user.password,
    newPassword: data.user.newPassword
  }

  userApi.changePassword(payload).then(res => {
    if (res.code === 200 || res.code === '200') {
      ElMessage.success('修改密码成功')
      // 清理本地登录态，要求用户重新登录
      localStorage.removeItem('system-user')
      localStorage.removeItem('system-token')
      router.push('/login')
    } else {
      ElMessage.error(res.msg || '修改密码失败')
    }
  }).catch(err => {
    console.error('修改密码请求异常:', err)
  })
}
</script>

<style scoped>
.password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 20px;
}

.password-card {
  width: 500px;
  padding: 40px;
}

.card-title {
  text-align: center;
  color: #1867c0;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 40px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  height: 45px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1867c0;
}

.action-section {
  text-align: center;
  margin-top: 40px;
}

.save-button {
  width: 180px;
  height: 45px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 25px;
  background: linear-gradient(45deg, #1867c0, #5cbbf6);
  border: none;
  transition: all 0.3s ease;
}

.save-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(24,103,192,0.3);
}
</style>