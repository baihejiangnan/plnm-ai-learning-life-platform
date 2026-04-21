<template>
  <div class="person-container">
    <div class="profile-card">
      <el-form :model="data.user" label-width="100px">
        <div class="avatar-section">
          <el-upload 
            ref="uploadRef"
            :show-file-list="false" 
            class="avatar-uploader" 
            :action="uploadUrl"
            :on-change="onFileChange"
            :auto-upload="false"
            accept="image/*"
          >
            <div class="avatar-wrapper">
              <img v-if="data.user?.avatar" :src="data.user.avatar" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              <div class="avatar-hover-text">点击更换头像</div>
            </div>
          </el-upload>
        </div>
        
        <el-form-item label="账号">
          <el-input 
            disabled 
            v-model="data.user.username" 
            class="custom-input"
            placeholder="您的账号"
          />
        </el-form-item>
        
        <el-form-item label="名称">
          <el-input 
            v-model="data.user.name" 
            class="custom-input"
            placeholder="请输入您的名称"
          />
        </el-form-item>
        
        <div class="action-section">
          <el-button 
            type="primary" 
            @click="save"
            class="save-button"
          >
            保存修改
          </el-button>
        </div>
      </el-form>
      <!-- 头像裁剪弹窗（已停用，不再打开） -->
      <el-dialog v-model="cropDialogVisible" title="裁剪头像" width="840px" @closed="closeCropDialog">
        <div class="cropper-layout">
          <div class="cropper-container">
            <img :src="objectUrl" ref="cropImgRef" alt="source" class="cropper-image" />
          </div>
          <div class="preview-panel">
            <div class="avatar-preview"></div>
            <div class="preview-tips">预览</div>
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="() => (cropper = null)">重置</el-button>
            <el-button @click="() => (cropper = null)">左旋90°</el-button>
            <el-button @click="() => (cropper = null)">右旋90°</el-button>
            <el-button @click="() => (cropper = null)">放大</el-button>
            <el-button @click="() => (cropper = null)">缩小</el-button>
            <el-button @click="closeCropDialog">取消</el-button>
            <el-button type="primary" @click="confirmCropAndUpload">确定</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import {reactive, computed, ref, nextTick, onBeforeUnmount} from "vue"
import request from "@/utils/request";
import {ElMessage} from "element-plus";
// import Cropper from 'cropperjs' // 已停用
// import 'cropperjs/dist/cropper.css' // 已停用
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'
// Pinia 用户仓库
const userStore = useUserStore()
const uploadUrl = computed(() => `${import.meta.env.VITE_BASE_URL}/files/upload`)
const uploadRef = ref(null)

// 状态
const cropDialogVisible = ref(false) // 已停用：不再打开
const cropImgRef = ref(null) // 保留占位，避免模板引用报错
let cropper = null // 占位
const objectUrl = ref('') // 占位

// 自动裁剪输出参数
const OUTPUT_SIZE = 300 // 300x300 输出尺寸
const OUTPUT_MIME = 'image/jpeg'
const OUTPUT_QUALITY = 0.9

const MAX_SIZE_MB = 2
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']
 
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
 
// 选择文件后：校验 -> 居中裁剪到 300x300 -> 上传 -> 保存
const onFileChange = async (file /* UploadFile */) => {
  const raw = file?.raw || file
  if (!raw) return false

  const ok = await onBeforeUpload(raw)
  // 清理内部文件队列，避免重复缓存
  if (uploadRef.value) {
    try { uploadRef.value.clearFiles() } catch(_) {}
  }
  if (!ok) return false

  try {
    const blob = await centerCropAndResize(raw, OUTPUT_SIZE, OUTPUT_MIME, OUTPUT_QUALITY)
    const form = new FormData()
    const uploadFile = new File([blob], 'avatar.jpg', { type: OUTPUT_MIME })
    form.append('file', uploadFile)

    const res = await request.post(uploadUrl.value, form)
    if (res && (res.code === 200 || res.code === '200')) {
      // 后端返回的文件URL
      data.user.avatar = res.data
      await save()
      ElMessage.success('头像已自动裁剪并上传成功')
    } else {
      ElMessage.error(res?.msg || '上传失败')
    }
  } catch (e) {
    console.error('自动裁剪/上传异常:', e)
    ElMessage.error(e?.message || '处理失败，请重试')
  }
  return false
}

// 仅做基本校验
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

// 中心裁剪并缩放到固定大小
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

// 关闭（保留占位，确保模板安全）
const closeCropDialog = () => {
  cropDialogVisible.value = false
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = ''
  }
}

// 已取消裁剪功能（占位，避免模板调用出错）
const confirmCropAndUpload = () => {
  ElMessage.info('已取消裁剪功能，选择图片后将自动裁剪并上传')
}

onBeforeUnmount(() => {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
})
 
 const emit = defineEmits(["updateUser"])
 
 const save = async () => {
   if (!data.user || !data.user.id) {
     ElMessage.error('用户信息缺失，请重新登录')
     return
   }
  try {
    const res = await userApi.updateInfo({
      name: data.user.name,
      avatar: data.user.avatar
    })
    if (res.code === 200) {
      const updated = res.data || {}
      data.user.name = updated.name ?? data.user.name
      data.user.avatar = updated.avatar ?? data.user.avatar
      // 立即更新全局 store，驱动导航栏即时更新
      userStore.updateUser({
        name: data.user.name,
        avatar: data.user.avatar
      })
      // 后台静默刷新远端用户信息，不影响前端立即更新体验
      userStore.refreshUserInfo().catch((e) => {
        console.warn('刷新用户信息失败（已静默）:', e?.message || e)
      })
      ElMessage.success('个人信息更新成功')
      emit('updateUser')
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  }
 }
 </script>

<style scoped>
.person-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  min-height: 80vh;
}

.profile-card {
  width: 500px;
  background: white;
  border-radius: 15px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
  padding: 40px;
  transition: all 0.3s ease;
}

.profile-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 25px rgba(0,0,0,0.15);
}

.avatar-section {
  margin: 20px 0 40px;
  text-align: center;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #fff;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.avatar-hover-text {
  position: absolute;
  bottom: -25px;
  left: 50%;
  transform: translateX(-50%);
  color: #666;
  font-size: 14px;
  opacity: 0;
  transition: all 0.3s ease;
}

.avatar-wrapper:hover .avatar-hover-text {
  opacity: 1;
  bottom: -30px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.custom-input :deep(.el-input__inner) {
  height: 45px;
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

.avatar-uploader-icon {
  font-size: 35px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  border: 4px dashed #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  transition: all 0.3s ease;
}

.avatar-uploader-icon:hover {
  border-color: #1867c0;
  color: #1867c0;
}

/* 裁剪弹窗布局与尺寸固定（保留，无实际使用） */
.cropper-layout {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 600px; /* 固定高度 */
}

.cropper-container {
  flex: 1 1 auto;
  height: 100%; /* 左侧裁剪区固定高度 */
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24px;
}

.cropper-image {
  max-width: 100%;
  max-height: 580px; /* 留出上下内边距空间 */
  display: block;
}

.preview-panel {
  width: 220px;
  flex: 0 0 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.avatar-preview {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  border: 4px solid #fff;
  background: #f5f7fa;
}

.preview-tips {
  margin-top: 12px;
  color: #666;
  font-size: 14px;
}
</style>

<style>
.avatar-uploader .el-upload {
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.avatar-uploader .el-upload:hover {
  transform: scale(1.02);
}

</style>