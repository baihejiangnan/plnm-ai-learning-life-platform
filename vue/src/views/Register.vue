<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-title">欢迎注册</div>
      <el-form :model="data.form" ref="formRef" :rules="data.rules">
        <el-form-item prop="username">
          <el-input 
            :prefix-icon="User" 
            size="large" 
            v-model="data.form.username" 
            placeholder="请输入账号"
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            :prefix-icon="Lock" 
            size="large" 
            v-model="data.form.password" 
            placeholder="请输入密码" 
            show-password
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input 
            :prefix-icon="Lock" 
            size="large" 
            v-model="data.form.confirmPassword" 
            placeholder="请确认密码" 
            show-password
            class="login-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            size="large" 
            type="primary" 
            class="login-button"
            @click="register"
          >注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="register-link">
        已有账号？<a href="/login">立即登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { reactive, ref } from "vue";
  import { User, Lock } from "@element-plus/icons-vue";
  import request from "@/utils/request";
  import {ElMessage} from "element-plus";
  import router from "@/router";

  const validatePass = (rule, value, callback) => {
    if (!value) {
      callback(new Error('请确认密码'))
    } else if (value !== data.form.password) {
      callback(new Error('两次输入密码不一致'))
    } else {
      callback()
    }
  }

  const data = reactive({
    form: { role: 'USER' },
    rules: {
      username: [
        { required: true, message: '请输入账号', trigger: 'blur' },
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
      ],
      confirmPassword: [
        { validator: validatePass, trigger: 'blur' },
      ],
    }
  })

  const formRef = ref()

  // 修复：使用 async/await 形式进行校验与提交流程，并统一捕获异常
  const register = async () => {
    try {
      const valid = await formRef.value.validate()
      if (!valid) return

      // 仅发送后端需要的字段，避免将 confirmPassword 传给后端
      const payload = {
        username: data.form.username,
        password: data.form.password,
        role: 'USER'
      }

      const res = await request.post('/api/user/register', payload)
      if (res && (res.code === 200 || res.code === '200')) {
        ElMessage.success('注册成功')
        router.push('/login')
      } else {
        ElMessage.error(res?.msg || '注册失败')
      }
    } catch (error) {
      console.error(error)
      ElMessage.error(error?.response?.data?.msg || error?.message || '请求失败')
    }
  }
</script>

<style scoped>
.login-container {
  height: 100vh;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(-45deg, #ee7752, #e73c7e, #23a6d5, #23d5ab);
  background-size: 400% 400%;
  animation: gradientBG 15s ease infinite;
  position: relative;
}

.login-container::before,
.login-container::after,
.login-container .star {
  content: '';
  position: absolute;
  width: 2px;
  height: 2px;
  background: white;
  box-shadow: 0 0 20px 2px rgba(255,255,255,0.7),
              0 0 40px 6px rgba(255,255,255,0.5),
              0 0 60px 10px rgba(255,255,255,0.3);
  border-radius: 50%;
}

.login-container::before {
  animation: shooting-star-1 3s linear infinite;
  top: 20%;
  left: -10%;
}

.login-container::after {
  animation: shooting-star-2 4s linear infinite;
  top: 40%;
  left: -5%;
}

.login-container .star:nth-child(1) {
  animation: shooting-star-3 5s linear infinite;
  top: 60%;
  left: -15%;
}

.login-container .star:nth-child(2) {
  animation: shooting-star-4 6s linear infinite;
  top: 80%;
  left: -8%;
}

.login-box {
  width: 380px;
  padding: 40px;
  border-radius: 15px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  animation: fadeIn 0.5s ease-out;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.login-box:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.2);
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 35px;
  color: #2c3e50;
  letter-spacing: 2px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.login-input :deep(.el-input__wrapper):hover {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.login-button {
  width: 100%;
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.register-link {
  text-align: right;
  margin-top: 20px;
  font-size: 14px;
  color: #606266;
}

.register-link a {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s ease;
}

.register-link a:hover {
  color: #764ba2;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes gradientBG {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

@keyframes shooting-star-1 {
  0% {
    transform: translate(0, 0) rotate(45deg);
    opacity: 0;
  }
  10%, 90% {
    opacity: 1;
  }
  100% {
    transform: translate(100vw, -100vh) rotate(45deg);
    opacity: 0;
  }
}

@keyframes shooting-star-2 {
  0% {
    transform: translate(0, 0) rotate(35deg);
    opacity: 0;
  }
  15%, 85% {
    opacity: 1;
  }
  100% {
    transform: translate(90vw, -90vh) rotate(35deg);
    opacity: 0;
  }
}

@keyframes shooting-star-3 {
  0% {
    transform: translate(0, 0) rotate(55deg);
    opacity: 0;
  }
  20%, 80% {
    opacity: 1;
  }
  100% {
    transform: translate(110vw, -95vh) rotate(55deg);
    opacity: 0;
  }
}

@keyframes shooting-star-4 {
  0% {
    transform: translate(0, 0) rotate(40deg);
    opacity: 0;
  }
  25%, 75% {
    opacity: 1;
  }
  100% {
    transform: translate(95vw, -85vh) rotate(40deg);
    opacity: 0;
  }
}
</style>