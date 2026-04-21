import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api'
import { STORAGE_KEYS, USER_ROLES } from '@/constants'
import { getToken, setToken, removeToken } from '@/utils'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(getToken())
  const userInfo = ref((() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.USER_INFO)
      return stored ? JSON.parse(stored) : null
    } catch (e) {
      return null
    }
  })())
  const permissions = ref([])
  const roles = ref([])
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => roles.value.includes(USER_ROLES.ADMIN))
  const isManager = computed(() => roles.value.includes(USER_ROLES.MANAGER))
  const userName = computed(() => userInfo.value?.username || userInfo.value?.name || '')
  const userAvatar = computed(() => userInfo.value?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')
  const userRole = computed(() => userInfo.value?.role || roles.value[0] || null)
  
  // 设置token
  const setUserToken = (newToken) => {
    token.value = newToken
    setToken(newToken)
  }
  
  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.value = info
    localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(info))
    if (info?.roles) {
      roles.value = Array.isArray(info.roles) ? info.roles : [info.roles]
    }
    if (info?.permissions) {
      permissions.value = Array.isArray(info.permissions) ? info.permissions : [info.permissions]
    }
  }
  
  // 登录
  const login = async (loginForm) => {
    try {
      const response = await userApi.login(loginForm)
      
      // 按照请求拦截器统一返回的数据结构进行判断
      if (response && response.code === 200) {
        const { token: newToken, user: userData } = response.data || {}
        
        // 保存token和用户信息
        setUserToken(newToken)
        setUserInfo(userData)
        
        ElMessage.success('登录成功')
        return Promise.resolve(response)
      } else {
        // 登录失败
        const errorMsg = response?.msg || '登录失败'
        ElMessage.error(errorMsg)
        return Promise.reject(new Error(errorMsg))
      }
    } catch (error) {
      const errorMsg = error.response?.data?.msg || error.message || '登录失败'
      ElMessage.error(errorMsg)
      return Promise.reject(error)
    }
  }
  
  // 获取用户信息
  const getUserInfo = async () => {
    try {
      if (!token.value) {
        throw new Error('未登录')
      }
      
      const response = await userApi.getInfo({ skipAuthRedirect: true })
      setUserInfo(response.data)
      return Promise.resolve(response)
    } catch (error) {
      // 仅在明确401且不在静默模式时才处理为登出，这里选择静默失败，交由调用方决定
      console.warn('获取用户信息失败:', error?.message || error)
      return Promise.reject(error)
    }
  }
  
  // 刷新用户信息（兼容旧调用）
  const refreshUserInfo = async () => {
    return getUserInfo()
  }
  
  // 登出
  const logout = async (showMessage = true) => {
    try {
      // 调用后端登出接口
      if (token.value) {
        await userApi.logout()
      }
    } catch (error) {
      console.warn('登出接口调用失败:', error)
    } finally {
      // 清除本地状态
      token.value = null
      userInfo.value = null
      permissions.value = []
      roles.value = []
      
      // 清除本地存储
      removeToken()
      localStorage.removeItem(STORAGE_KEYS.USER_INFO)
      
      if (showMessage) {
        ElMessage.success('已退出登录')
      }
      
      // 跳转到登录页
      router.push('/login')
    }
  }
  
  // 更新用户信息
  const updateUser = (userData) => {
    userInfo.value = { ...userInfo.value, ...userData }
    localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(userInfo.value))
  }
  
  // 检查权限
  const hasPermission = (permission) => {
    if (!permission) return true
    return permissions.value.includes(permission)
  }
  
  // 检查角色
  const hasRole = (role) => {
    if (!role) return true
    return roles.value.includes(role)
  }
  
  // 检查认证状态
  const checkAuth = () => {
    if (!isLoggedIn.value) {
      ElMessage.error('请先登录')
      router.push('/login')
      return false
    }
    return true
  }
  
  return {
    // 状态
    token,
    userInfo,
    permissions,
    roles,
    
    // 计算属性
    isLoggedIn,
    isAdmin,
    isManager,
    userName,
    userAvatar,
    userRole,
    
    // 方法
    setUserToken,
    setUserInfo,
    login,
    logout,
    getUserInfo,
    refreshUserInfo,
    updateUser,
    hasPermission,
    hasRole,
    checkAuth
  }
})