import { createRouter, createWebHistory } from 'vue-router'
import { beforeEachGuard, afterEachGuard, routeErrorHandler } from './guards'
import { ROUTE_NAMES, USER_ROLES } from '@/constants'

// 基础路由配置
const routes = [
  {
    path: '/login',
    name: ROUTE_NAMES.LOGIN,
    component: () => import('@/views/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: {
      title: '注册',
      requiresAuth: false
    }
  },
  {
    path: '/',
    name: 'Manager',
    component: () => import('@/views/Manager.vue'),
    redirect: '/home',
    meta: {
      title: '管理系统',
      requiresAuth: true
    },
    children: [
      {
        path: 'home',
        name: ROUTE_NAMES.HOME,
        component: () => import('@/views/manager/Home.vue'),
        meta: {
          title: '系统首页',
          requiresAuth: true
        }
      },
      // 个人中心（父级 + 子级）
      {
        path: '/person',
        name: 'Person',
        component: () => import('@/views/manager/PersonLayout.vue'),
        redirect: '/person/info',
        children: [
          {
            path: 'info',
            name: ROUTE_NAMES.PROFILE,
            component: () => import('@/views/manager/Person.vue'),
            meta: { title: '个人信息', requiresAuth: true }
          },
          {
            path: 'password',
            name: 'Password',
            redirect: { path: '/person/info', query: { section: 'password' } },
            meta: { title: '个人信息', requiresAuth: true }
          }
        ]
      },
      // 笔记中心（父级 + 子级）
      {
        path: '/notes',
        name: 'NotesRoot',
        component: () => import('@/views/manager/NotesLayout.vue'),
        redirect: '/notes/list',
        children: [
          {
            path: 'list',
            name: 'Notes',
            component: () => import('@/views/manager/NoteList.vue'),
            meta: { title: '笔记列表', requiresAuth: true }
          },
          {
            path: 'editor',
            name: 'NoteEditor',
            component: () => import('@/views/manager/NoteEditor.vue'),
            meta: { title: '新建笔记', requiresAuth: true }
          },
          {
            path: 'editor/:id',
            name: 'NoteEditorEdit',
            component: () => import('@/views/manager/NoteEditor.vue'),
            meta: { title: '编辑笔记', requiresAuth: true }
          },
          {
            path: 'detail/:id',
            name: 'NoteDetail',
            component: () => import('@/views/manager/NoteDetail.vue'),
            meta: { title: '笔记详情', requiresAuth: true }
          },
          {
            path: 'tags',
            name: 'Tags',
            component: () => import('@/views/manager/TagManager.vue'),
            meta: { title: '标签管理', requiresAuth: true }
          }
        ]
      },
      // 保持生活模块嵌套结构
      {
        path: '/life',
        name: 'Life',
        component: () => import('@/views/manager/LifeLayout.vue'),
        redirect: '/life/expenses',
        children: [
          { path: 'expenses', name: 'ExpenseList', component: () => import('@/views/manager/life/ExpenseList.vue') },
          { path: 'budgets', name: 'BudgetSetting', component: () => import('@/views/manager/life/BudgetSetting.vue') },
          { path: 'categories', name: 'ExpenseCategoryManager', component: () => import('@/views/manager/life/ExpenseCategoryManager.vue') }
        ]
      },
      // 学习模块
      {
        path: '/learning',
        name: 'Learning',
        component: () => import('@/views/manager/LearningLayout.vue'),
        redirect: '/learning/courses',
        children: [
          { path: 'courses', name: 'CourseManager', component: () => import('@/views/manager/learning/CourseManager.vue') },
          { path: 'progress', name: 'ProgressTracker', component: () => import('@/views/manager/learning/ProgressTracker.vue') },
          { path: 'resources', name: 'ResourceLibrary', component: () => import('@/views/manager/learning/ResourceLibrary.vue') }
        ]
      },
      {
        path: '/ai/chat',
        name: 'AiChat',
        component: () => import('@/views/manager/ai/AiChat.vue'),
        meta: { title: 'AI对话', requiresAuth: true }
      },
      {
        path: '/ai/audit',
        name: 'AiAuditLog',
        component: () => import('@/views/manager/ai/AiAuditLog.vue'),
        meta: { title: 'AI审计日志', requiresAuth: true }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/manager/Settings.vue'),
        meta: {
          title: '系统设置',
          requiresAuth: true
        }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: {
      title: '访问被拒绝',
      requiresAuth: false
    }
  },
  {
    path: '/404',
    name: ROUTE_NAMES.NOT_FOUND,
    component: () => import('@/views/error/404.vue'),
    meta: {
      title: '页面不存在',
      requiresAuth: false
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 路由切换时的滚动行为
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 注册路由守卫
router.beforeEach(beforeEachGuard)
router.afterEach(afterEachGuard)
router.onError(routeErrorHandler)

export default router
