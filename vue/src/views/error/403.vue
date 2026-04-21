<template>
  <div class="error-page">
    <div class="error-container">
      <div class="error-icon">
        <el-icon :size="120" color="#f56c6c">
          <WarningFilled />
        </el-icon>
      </div>
      <div class="error-content">
        <h1 class="error-title">403</h1>
        <h2 class="error-subtitle">访问被拒绝</h2>
        <p class="error-description">
          抱歉，您没有权限访问此页面。请联系管理员获取相应权限。
        </p>
        <div class="error-actions">
          <el-button type="primary" @click="goBack">
            返回上一页
          </el-button>
          <el-button @click="goHome">
            回到首页
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 回到首页
const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.error-container {
  text-align: center;
  background: white;
  border-radius: 20px;
  padding: 60px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
}

.error-icon {
  margin-bottom: 30px;
}

.error-title {
  font-size: 72px;
  font-weight: bold;
  color: #f56c6c;
  margin: 0 0 10px 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.error-subtitle {
  font-size: 28px;
  color: #333;
  margin: 0 0 20px 0;
  font-weight: 600;
}

.error-description {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 40px;
}

.error-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

.error-actions .el-button {
  padding: 12px 30px;
  font-size: 16px;
  border-radius: 25px;
}

@media (max-width: 768px) {
  .error-container {
    padding: 40px 20px;
  }
  
  .error-title {
    font-size: 60px;
  }
  
  .error-subtitle {
    font-size: 24px;
  }
  
  .error-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .error-actions .el-button {
    width: 200px;
  }
}
</style>