/**
 * 应用常量定义
 */

/**
 * 用户角色常量
 */
export const USER_ROLES = {
  ADMIN: 'admin',
  USER: 'user',
  MANAGER: 'manager'
}

/**
 * 用户角色标签
 */
export const USER_ROLE_LABELS = {
  [USER_ROLES.ADMIN]: '管理员',
  [USER_ROLES.USER]: '普通用户',
  [USER_ROLES.MANAGER]: '经理'
}

/**
 * 用户状态常量
 */
export const USER_STATUS = {
  ACTIVE: 1,
  INACTIVE: 0,
  BANNED: -1
}

/**
 * 用户状态标签
 */
export const USER_STATUS_LABELS = {
  [USER_STATUS.ACTIVE]: '正常',
  [USER_STATUS.INACTIVE]: '禁用',
  [USER_STATUS.BANNED]: '封禁'
}

/**
 * 用户状态颜色
 */
export const USER_STATUS_COLORS = {
  [USER_STATUS.ACTIVE]: 'success',
  [USER_STATUS.INACTIVE]: 'warning',
  [USER_STATUS.BANNED]: 'danger'
}

/**
 * 性别常量
 */
export const GENDER = {
  MALE: 1,
  FEMALE: 2,
  UNKNOWN: 0
}

/**
 * 性别标签
 */
export const GENDER_LABELS = {
  [GENDER.MALE]: '男',
  [GENDER.FEMALE]: '女',
  [GENDER.UNKNOWN]: '未知'
}

/**
 * HTTP状态码
 */
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  METHOD_NOT_ALLOWED: 405,
  CONFLICT: 409,
  INTERNAL_SERVER_ERROR: 500,
  BAD_GATEWAY: 502,
  SERVICE_UNAVAILABLE: 503
}

/**
 * 响应状态码
 */
export const RESPONSE_CODE = {
  SUCCESS: 200,
  ERROR: 500,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  VALIDATION_ERROR: 400
}

/**
 * 本地存储键名
 */
export const STORAGE_KEYS = {
  TOKEN: 'system-token',
  USER_INFO: 'system-user',
  THEME: 'theme',
  LANGUAGE: 'language',
  SIDEBAR_COLLAPSED: 'sidebarCollapsed',
  REMEMBER_PASSWORD: 'rememberPassword',
  UI_SIZE: 'uiSize'
}

/**
 * 路由名称
 */
export const ROUTE_NAMES = {
  LOGIN: 'Login',
  HOME: 'Home',
  USER: 'User',
  MANAGER: 'Manager',
  PROFILE: 'Profile',
  SETTINGS: 'Settings',
  NOT_FOUND: 'NotFound'
}

/**
 * 页面大小选项
 */
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

/**
 * 默认页面大小
 */
export const DEFAULT_PAGE_SIZE = 10

/**
 * 文件上传限制
 */
export const FILE_UPLOAD = {
  MAX_SIZE: 10 * 1024 * 1024, // 10MB
  ALLOWED_TYPES: {
    IMAGE: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
    DOCUMENT: ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'],
    EXCEL: ['application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']
  },
  ALLOWED_EXTENSIONS: {
    IMAGE: ['.jpg', '.jpeg', '.png', '.gif', '.webp'],
    DOCUMENT: ['.pdf', '.doc', '.docx'],
    EXCEL: ['.xls', '.xlsx']
  }
}

/**
 * 主题模式
 */
export const THEME_MODE = {
  LIGHT: 'light',
  DARK: 'dark',
  AUTO: 'auto'
}

/**
 * 语言选项
 */
export const LANGUAGES = {
  ZH_CN: 'zh-cn',
  EN_US: 'en-us'
}

/**
 * 语言标签
 */
export const LANGUAGE_LABELS = {
  [LANGUAGES.ZH_CN]: '简体中文',
  [LANGUAGES.EN_US]: 'English'
}

/**
 * 全局组件尺寸（布局密度）
 */
export const UI_SIZE = {
  DEFAULT: 'default',
  SMALL: 'small',
  LARGE: 'large'
}

/**
 * 日期格式
 */
export const DATE_FORMATS = {
  DATE: 'YYYY-MM-DD',
  DATETIME: 'YYYY-MM-DD HH:mm:ss',
  TIME: 'HH:mm:ss',
  MONTH: 'YYYY-MM',
  YEAR: 'YYYY'
}

/**
 * 操作类型
 */
export const OPERATION_TYPE = {
  CREATE: 'create',
  UPDATE: 'update',
  DELETE: 'delete',
  VIEW: 'view',
  EXPORT: 'export',
  IMPORT: 'import'
}

/**
 * 操作类型标签
 */
export const OPERATION_TYPE_LABELS = {
  [OPERATION_TYPE.CREATE]: '新增',
  [OPERATION_TYPE.UPDATE]: '修改',
  [OPERATION_TYPE.DELETE]: '删除',
  [OPERATION_TYPE.VIEW]: '查看',
  [OPERATION_TYPE.EXPORT]: '导出',
  [OPERATION_TYPE.IMPORT]: '导入'
}

/**
 * 表单验证规则
 */
export const VALIDATION_RULES = {
  REQUIRED: { required: true, message: '此字段为必填项', trigger: 'blur' },
  EMAIL: {
    pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
    message: '请输入有效的邮箱地址',
    trigger: 'blur'
  },
  PHONE: {
    pattern: /^1[3-9]\d{9}$/,
    message: '请输入有效的手机号码',
    trigger: 'blur'
  },
  PASSWORD: {
    min: 6,
    max: 20,
    message: '密码长度应在6-20个字符之间',
    trigger: 'blur'
  },
  USERNAME: {
    pattern: /^[a-zA-Z0-9_]{3,20}$/,
    message: '用户名只能包含字母、数字和下划线，长度3-20个字符',
    trigger: 'blur'
  }
}

/**
 * 消息类型
 */
export const MESSAGE_TYPE = {
  SUCCESS: 'success',
  WARNING: 'warning',
  INFO: 'info',
  ERROR: 'error'
}

/**
 * 确认框类型
 */
export const CONFIRM_TYPE = {
  WARNING: 'warning',
  INFO: 'info',
  SUCCESS: 'success',
  ERROR: 'error'
}

/**
 * 加载状态
 */
export const LOADING_STATE = {
  IDLE: 'idle',
  LOADING: 'loading',
  SUCCESS: 'success',
  ERROR: 'error'
}

/**
 * 默认头像
 */
export const DEFAULT_AVATAR = '/images/default-avatar.png'

/**
 * 系统配置
 */
export const SYSTEM_CONFIG = {
  APP_NAME: 'PLNM - 基于多智能体协作的个人学习生活平台',
  APP_VERSION: '1.0.0',
  COPYRIGHT: '© 2024 PLNM - 基于多智能体协作的个人学习生活平台. All rights reserved.',
  CONTACT_EMAIL: 'admin@example.com'
}

/**
 * 导出所有常量
 */
export default {
  USER_ROLES,
  USER_ROLE_LABELS,
  USER_STATUS,
  USER_STATUS_LABELS,
  USER_STATUS_COLORS,
  GENDER,
  GENDER_LABELS,
  HTTP_STATUS,
  RESPONSE_CODE,
  STORAGE_KEYS,
  ROUTE_NAMES,
  PAGE_SIZE_OPTIONS,
  DEFAULT_PAGE_SIZE,
  FILE_UPLOAD,
  THEME_MODE,
  LANGUAGES,
  LANGUAGE_LABELS,
  DATE_FORMATS,
  OPERATION_TYPE,
  OPERATION_TYPE_LABELS,
  VALIDATION_RULES,
  MESSAGE_TYPE,
  CONFIRM_TYPE,
  LOADING_STATE,
  DEFAULT_AVATAR,
  SYSTEM_CONFIG,
  UI_SIZE
}
