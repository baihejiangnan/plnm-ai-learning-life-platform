/**
 * 通用工具函数库
 */

/**
 * 格式化日期
 * @param {Date|string|number} date - 日期
 * @param {string} format - 格式化模板，默认 'YYYY-MM-DD HH:mm:ss'
 * @returns {string} 格式化后的日期字符串
 */
export function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return ''
  
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 防抖函数
 * @param {Function} func - 要防抖的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
export function debounce(func, delay = 300) {
  let timeoutId
  return function (...args) {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => func.apply(this, args), delay)
  }
}

/**
 * 节流函数
 * @param {Function} func - 要节流的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(func, delay = 300) {
  let lastTime = 0
  return function (...args) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      func.apply(this, args)
    }
  }
}

/**
 * 深拷贝对象
 * @param {any} obj - 要拷贝的对象
 * @returns {any} 拷贝后的对象
 */
export function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj.getTime())
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  if (typeof obj === 'object') {
    const clonedObj = {}
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key])
      }
    }
    return clonedObj
  }
}

/**
 * 生成随机字符串
 * @param {number} length - 字符串长度
 * @returns {string} 随机字符串
 */
export function generateRandomString(length = 8) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

/**
 * 验证邮箱格式
 * @param {string} email - 邮箱地址
 * @returns {boolean} 是否为有效邮箱
 */
export function validateEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

/**
 * 验证手机号格式
 * @param {string} phone - 手机号
 * @returns {boolean} 是否为有效手机号
 */
export function validatePhone(phone) {
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(phone)
}

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化后的文件大小
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 获取文件扩展名
 * @param {string} filename - 文件名
 * @returns {string} 文件扩展名
 */
export function getFileExtension(filename) {
  return filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2)
}

/**
 * 数组去重
 * @param {Array} arr - 原数组
 * @param {string} key - 去重依据的键名（对象数组时使用）
 * @returns {Array} 去重后的数组
 */
export function uniqueArray(arr, key) {
  if (!key) {
    return [...new Set(arr)]
  }
  const seen = new Set()
  return arr.filter(item => {
    const value = item[key]
    if (seen.has(value)) {
      return false
    }
    seen.add(value)
    return true
  })
}

/**
 * 树形数据扁平化
 * @param {Array} tree - 树形数据
 * @param {string} childrenKey - 子节点键名，默认 'children'
 * @returns {Array} 扁平化后的数组
 */
export function flattenTree(tree, childrenKey = 'children') {
  const result = []
  function traverse(nodes) {
    nodes.forEach(node => {
      result.push(node)
      if (node[childrenKey] && node[childrenKey].length > 0) {
        traverse(node[childrenKey])
      }
    })
  }
  traverse(tree)
  return result
}

/**
 * 扁平数据转树形结构
 * @param {Array} list - 扁平数据
 * @param {string} idKey - ID键名，默认 'id'
 * @param {string} parentIdKey - 父ID键名，默认 'parentId'
 * @param {string} childrenKey - 子节点键名，默认 'children'
 * @returns {Array} 树形结构数据
 */
export function listToTree(list, idKey = 'id', parentIdKey = 'parentId', childrenKey = 'children') {
  const map = {}
  const roots = []
  
  // 创建映射
  list.forEach(item => {
    map[item[idKey]] = { ...item, [childrenKey]: [] }
  })
  
  // 构建树形结构
  list.forEach(item => {
    const node = map[item[idKey]]
    if (item[parentIdKey] && map[item[parentIdKey]]) {
      map[item[parentIdKey]][childrenKey].push(node)
    } else {
      roots.push(node)
    }
  })
  
  return roots
}

/**
 * 本地存储封装
 */
export const storage = {
  /**
   * 设置本地存储
   * @param {string} key - 键名
   * @param {any} value - 值
   */
  set(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value))
    } catch (error) {
      console.error('localStorage set error:', error)
    }
  },
  
  /**
   * 获取本地存储
   * @param {string} key - 键名
   * @param {any} defaultValue - 默认值
   * @returns {any} 存储的值
   */
  get(key, defaultValue = null) {
    try {
      const value = localStorage.getItem(key)
      return value ? JSON.parse(value) : defaultValue
    } catch (error) {
      console.error('localStorage get error:', error)
      return defaultValue
    }
  },
  
  /**
   * 删除本地存储
   * @param {string} key - 键名
   */
  remove(key) {
    try {
      localStorage.removeItem(key)
    } catch (error) {
      console.error('localStorage remove error:', error)
    }
  },
  
  /**
   * 清空本地存储
   */
  clear() {
    try {
      localStorage.clear()
    } catch (error) {
      console.error('localStorage clear error:', error)
    }
  }
}

/**
 * Token管理
 */
import { STORAGE_KEYS } from '@/constants'

/**
 * 获取Token
 * @returns {string|null} Token值
 */
export const getToken = () => {
  return localStorage.getItem(STORAGE_KEYS.TOKEN)
}

/**
 * 设置Token
 * @param {string} token - Token值
 */
export const setToken = (token) => {
  localStorage.setItem(STORAGE_KEYS.TOKEN, token)
}

/**
 * 删除Token
 */
export const removeToken = () => {
  localStorage.removeItem(STORAGE_KEYS.TOKEN)
}