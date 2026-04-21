/**
 * 路由守卫
 */
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { ROUTE_NAMES } from '@/constants'

/**
 * 白名单路由（不需要登录验证）
 */
const whiteList = ['/login', '/register']

/**
 * 前置守卫
 * @param {Object} to - 目标路由
 * @param {Object} from - 来源路由
 * @param {Function} next - 导航函数
 */
export function beforeEachGuard(to, from, next) {
  const userStore = useUserStore()
  
  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 管理系统`
  } else {
    document.title = '管理系统'
  }
  
  // 检查是否在白名单中
  if (whiteList.includes(to.path)) {
    // 如果已登录且访问登录页，重定向到首页
    if (userStore.isLoggedIn && to.path === '/login') {
      next('/')
    } else {
      next()
    }
    return
  }
  
  // 检查登录状态
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    next('/login')
    return
  }
  
  // 检查权限
  if (to.meta?.requiresAuth !== false) {
    const hasPermission = checkPermission(to, userStore)
    if (!hasPermission) {
      ElMessage.error('没有访问权限')
      next('/403')
      return
    }
  }
  
  next()
}

/**
 * 后置守卫
 * @param {Object} to - 目标路由
 * @param {Object} from - 来源路由
 */
export function afterEachGuard(to, from) {
  // 可以在这里添加页面访问统计、埋点等逻辑
  console.log(`路由跳转: ${from.path} -> ${to.path}`)
}

/**
 * 检查路由权限
 * @param {Object} route - 路由对象
 * @param {Object} userStore - 用户状态
 * @returns {boolean} 是否有权限
 */
function checkPermission(route, userStore) {
  // 如果路由没有设置权限要求，默认允许访问
  if (!route.meta?.roles) {
    return true
  }
  
  // 检查用户角色是否在允许的角色列表中
  const userRole = userStore.userRole
  const requiredRoles = route.meta.roles
  
  if (Array.isArray(requiredRoles)) {
    return requiredRoles.includes(userRole)
  }
  
  return requiredRoles === userRole
}

/**
 * 路由错误处理
 * @param {Error} error - 错误对象
 */
export function routeErrorHandler(error) {
  console.error('路由错误:', error)
  ElMessage.error('页面加载失败')
}

/**
 * 动态添加路由
 * @param {Object} router - 路由实例
 * @param {Object} userStore - 用户状态
 */
export function addDynamicRoutes(router, userStore) {
  const userRole = userStore.userRole
  
  // 根据用户角色动态添加路由
  const dynamicRoutes = getDynamicRoutes(userRole)
  
  dynamicRoutes.forEach(route => {
    router.addRoute(route)
  })
}

/**
 * 获取动态路由
 * @param {string} role - 用户角色
 * @returns {Array} 动态路由列表
 */
function getDynamicRoutes(role) {
  const routes = []
  
  // 根据角色动态添加路由
  // 这里可以根据实际需要添加动态路由
  // 目前所有路由都在静态路由中定义
  
  return routes
}

/**
 * 重置路由
 * @param {Object} router - 路由实例
 */
export function resetRouter(router) {
  // 获取所有路由
  const routes = router.getRoutes()
  
  // 移除动态添加的路由
  routes.forEach(route => {
    if (route.meta?.dynamic) {
      router.removeRoute(route.name)
    }
  })
}