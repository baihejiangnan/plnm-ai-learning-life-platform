/**
 * API接口统一管理
 */
import request from '@/utils/request'

// API基础路径
const API_BASE = '/api'



/**
 * 笔记相关API
 */
export const noteApi = {
  /**
   * 获取笔记列表
   * @param {Object} params - 查询参数 {page, size, keyword, tagId, sortBy, sortOrder}
   * @returns {Promise} 笔记列表
   */
  getList(params) {
    return request({
      url: `${API_BASE}/notes`,
      method: 'get',
      params
    })
  },

  /**
   * 获取笔记详情
   * @param {number|string} id - 笔记ID
   * @returns {Promise} 笔记详情
   */
  getDetail(id) {
    return request({
      url: `${API_BASE}/notes/${id}`,
      method: 'get'
    })
  },

  /**
   * 根据ID获取笔记（getDetail的别名）
   * @param {number|string} id - 笔记ID
   * @returns {Promise} 笔记详情
   */
  getById(id) {
    return this.getDetail(id)
  },

  /**
   * 创建新笔记
   * @param {Object} data - 笔记数据
   * @returns {Promise} 创建结果
   */
  create(data) {
    return request({
      url: `${API_BASE}/notes`,
      method: 'post',
      data
    })
  },

  /**
   * 更新笔记
   * @param {number|string} id - 笔记ID
   * @param {Object} data - 更新数据
   * @returns {Promise} 更新结果
   */
  update(id, data) {
    console.log('noteApi.update - 接收到的参数:')
    console.log('id:', id)
    console.log('data:', data)
    
    // 确保data中包含id字段
    const requestData = {
      ...data,
      id: id
    }
    
    console.log('noteApi.update - 发送的数据:', requestData)
    
    return request({
      url: `${API_BASE}/notes`,
      method: 'put',
      data: requestData
    })
  },

  /**
   * 删除笔记
   * @param {number|string} id - 笔记ID
   * @returns {Promise} 删除结果
   */
  delete(id) {
    return request({
      url: `${API_BASE}/notes/${id}`,
      method: 'delete'
    })
  }
}

/**
 * 标签相关API
 */
export const tagApi = {
  /**
   * 获取标签列表
   * @param {Object} params - 查询参数 { userId }
   * @returns {Promise} 标签列表
   */
  getList(params) {
    return request({
      url: `${API_BASE}/tags`,
      method: 'get',
      params
    })
  },

  /**
   * 创建标签
   * @param {Object} data - 标签数据
   * @returns {Promise} 创建结果
   */
  create(data) {
    return request({
      url: `${API_BASE}/tags`,
      method: 'post',
      data
    })
  },

  /**
   * 更新标签
   * @param {number|string} id - 标签ID
   * @param {Object} data - 标签数据
   * @returns {Promise} 更新结果
   */
  update(id, data) {
    return request({
      url: `${API_BASE}/tags/${id}`,
      method: 'put',
      data
    })
  },

  /**
   * 搜索标签
   * @param {Object} params - 查询参数 { userId, keyword }
   * @returns {Promise} 标签列表
   */
  search(params) {
    return request({
      url: `${API_BASE}/tags/search`,
      method: 'get',
      params
    })
  },

  /**
   * 获取热门标签
   * @param {Object} params - 查询参数 { userId, limit }
   * @returns {Promise} 热门标签列表
   */
  getPopular(params) {
    return request({
      url: `${API_BASE}/tags/popular`,
      method: 'get',
      params
    })
  },

  /**
   * 删除标签
   * @param {number|string} id - 标签ID
   * @returns {Promise} 删除结果
   */
  delete(id) {
    return request({
      url: `${API_BASE}/tags/${id}`,
      method: 'delete'
    })
  }
}

/**
 * 用户相关API
 */
export const userApi = {
  /**
   * 用户登录
   * @param {Object} data - 登录数据 {username, password}
   * @returns {Promise} 登录结果
   */
  login(data) {
    return request({
      url: `${API_BASE}/user/login`,
      method: 'post',
      data
    })
  },

  /**
   * 用户注册
   * @param {Object} data - 注册数据
   * @returns {Promise} 注册结果
   */
  register(data) {
    return request({
      url: `${API_BASE}/user/register`,
      method: 'post',
      data
    })
  },

  /**
   * 获取用户信息
   * @param {Object} [config] - 额外请求配置（如 skipAuthRedirect 等）
   * @returns {Promise} 用户信息
   */
  getInfo(config = {}) {
    return request({
      url: `${API_BASE}/user/info`,
      method: 'get',
      ...config
    })
  },

  /**
   * 更新用户信息
   * @param {Object} data - 用户数据 {name, avatar}
   * @returns {Promise} 更新结果
   */
  updateInfo(data) {
    return request({
      url: `${API_BASE}/user/profile`,
      method: 'put',
      data
    })
  },

  /**
   * 修改密码
   * @param {Object} data - 密码数据 {oldPassword, newPassword}
   * @returns {Promise} 修改结果
   */
  changePassword(data) {
    return request({
      url: `${API_BASE}/user/password`,
      method: 'put',
      data
    })
  },

  /**
   * 分页查询用户
   * @param {Object} params - 查询参数
   * @returns {Promise} 分页结果
   */
  getPage(params) {
    return request({
      url: `${API_BASE}/user/page`,
      method: 'get',
      params
    })
  },

  /**
   * 获取登录会话列表
   * @param {Object} params - 查询参数 {page, size}
   * @returns {Promise} 会话分页数据
   */
  getSessions(params) {
    return request({
      url: `${API_BASE}/user/sessions`,
      method: 'get',
      params
    })
  },

  // 退出登录（当前会话）
  logout() {
    return request({
      url: `${API_BASE}/user/logout`,
      method: 'post'
    })
  },

  // 下线所有会话（包含当前）
  logoutAllSessions() {
    return request({
      url: `${API_BASE}/user/logoutAll`,
      method: 'post'
    })
  }
}

/**
 * 验证码相关API
 */
export const captchaApi = {
  getCaptcha() {
    return request({
      url: `${API_BASE}/captcha`,
      method: 'get'
    })
  }
}

/**
 * 文件上传相关API
 */
export const fileApi = {
  /**
   * 上传文件
   * @param {FormData} formData - 文件数据
   * @returns {Promise} 上传结果
   */
  upload(formData) {
    return request({
      url: `/files/upload`,
      method: 'post',
      data: formData
      // 不要手动设置 Content-Type，交由 axios 自动处理 boundary
    })
  },

  /**
   * 删除文件
   * @param {number|string} id - 文件ID
   * @returns {Promise} 删除结果
   */
  delete(id) {
    return request({
      url: `/files/${id}`,
      method: 'delete'
    })
  }
}

/**
 * 系统相关API
 */
export const systemApi = {
  /**
   * 健康检查
   * @returns {Promise} 健康状态
   */
  health() {
    return request({
      url: `${API_BASE}/actuator/health`,
      method: 'get'
    })
  },

  /**
   * 获取系统配置
   * @returns {Promise} 系统配置
   */
  getConfig() {
    return request({
      url: `${API_BASE}/system/config`,
      method: 'get'
    })
  },

  /**
   * 更新系统配置
   * @param {Object} data - 配置数据
   * @returns {Promise} 更新结果
   */
  updateConfig(data) {
    return request({
      url: `${API_BASE}/system/config`,
      method: 'put',
      data
    })
  }
}

export const notificationApi = {
  getList: (params) => request({ url: '/api/notifications', method: 'get', params }),
  overview: (params) => request({ url: '/api/notifications/overview', method: 'get', params }),
  markRead: (data) => request({ url: '/api/notifications/read', method: 'post', data }),
  delete: (data) => request({ url: '/api/notifications', method: 'delete', data }),
  getSettings: (params) => request({ url: '/api/notifications/settings', method: 'get', params }),
  updateSettings: (params, data) => request({ url: '/api/notifications/settings', method: 'put', params, data })
}

// 导出所有API
export default {
  userApi,
  fileApi,
  systemApi
}

// 生活-消费分类 API
export const expenseCategoryApi = {
  getList(params) {
    return request({ url: `${API_BASE}/expense-categories`, method: 'get', params })
  },
  create(data) {
    return request({ url: `${API_BASE}/expense-categories`, method: 'post', data })
  },
  update(id, data) {
    return request({ url: `${API_BASE}/expense-categories/${id}`, method: 'put', data })
  },
  delete(id) {
    return request({ url: `${API_BASE}/expense-categories/${id}`, method: 'delete' })
  },
  initDefault() {
    return request({ url: `${API_BASE}/expense-categories/init-default`, method: 'post' })
  }
}

// 生活-消费记录/预算 API
export const expenseApi = {
  // 分页列表
  getList(params) {
    return request({ url: `${API_BASE}/expenses`, method: 'get', params })
  },
  // 新建
  create(data) {
    return request({ url: `${API_BASE}/expenses`, method: 'post', data })
  },
  // 更新
  update(id, data) {
    return request({ url: `${API_BASE}/expenses/${id}`, method: 'put', data })
  },
  // 删除
  delete(id) {
    return request({ url: `${API_BASE}/expenses/${id}`, method: 'delete' })
  },
  // 月度统计（总额/分类占比/预算信息）
  getMonthStats(month) {
    return request({ url: `${API_BASE}/expenses/stats/month`, method: 'get', params: { month } })
  },
  // 获取预算
  getBudgets(month) {
    return request({ url: `${API_BASE}/expenses/budgets`, method: 'get', params: { month } })
  },
  // 设置预算（新增或更新）
  setBudget(data) {
    return request({ url: `${API_BASE}/expenses/budgets`, method: 'post', data })
  }
}

/**
 * 学习课程分类相关API
 */
export const learningCourseCategoryApi = {
  /** 列表查询 */
  getList() {
    return request({ url: `${API_BASE}/learning/course-categories`, method: 'get' })
  },
  /** 创建 */
  create(data) {
    return request({ url: `${API_BASE}/learning/course-categories`, method: 'post', data })
  },
  /** 更新 */
  update(id, data) {
    return request({ url: `${API_BASE}/learning/course-categories/${id}`, method: 'put', data })
  },
  /** 删除 */
  delete(id) {
    return request({ url: `${API_BASE}/learning/course-categories/${id}`, method: 'delete' })
  },
  /** 初始化默认分类 */
  initDefault() {
    return request({ url: `${API_BASE}/learning/course-categories/init-default`, method: 'post' })
  }
}

/** 学习资源库相关API */
export const learningResourceApi = {
  getList(params) {
    return request({ url: `${API_BASE}/learning/resources`, method: 'get', params })
  },
  create(data) {
    return request({ url: `${API_BASE}/learning/resources`, method: 'post', data })
  },
  update(id, data) {
    return request({ url: `${API_BASE}/learning/resources/${id}`, method: 'put', data })
  },
  delete(id) {
    return request({ url: `${API_BASE}/learning/resources/${id}`, method: 'delete' })
  }
}

export const learningProgressApi = {
  getList() {
    return request({ url: `${API_BASE}/learning/progress`, method: 'get' })
  },
  create(data) {
    return request({ url: `${API_BASE}/learning/progress`, method: 'post', data })
  },
  update(id, data) {
    return request({ url: `${API_BASE}/learning/progress/${id}`, method: 'put', data })
  },
  delete(id) {
    return request({ url: `${API_BASE}/learning/progress/${id}`, method: 'delete' })
  }
}

export const aiApi = {
  quickAction(data) {
    return request({ url: `${API_BASE}/ai/quick-action`, method: 'post', data })
  },
  getRoles() {
    return request({ url: `${API_BASE}/ai/roles`, method: 'get' })
  },
  chat(data) {
    return request({ url: `${API_BASE}/ai/chat`, method: 'post', data })
  },
  getAuditLogs(params) {
    return request({ url: `${API_BASE}/ai/audit`, method: 'get', params })
  }
}
