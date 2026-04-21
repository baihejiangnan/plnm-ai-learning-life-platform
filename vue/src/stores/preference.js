import { defineStore } from 'pinia'
import { ref, watch, computed } from 'vue'
import { STORAGE_KEYS, THEME_MODE, LANGUAGES, UI_SIZE } from '@/constants'

export const usePreferenceStore = defineStore('preference', () => {
  // 读取本地存储
  const theme = ref(localStorage.getItem(STORAGE_KEYS.THEME) || THEME_MODE.AUTO)
  const language = ref(localStorage.getItem(STORAGE_KEYS.LANGUAGE) || LANGUAGES.ZH_CN)
  const uiSize = ref(localStorage.getItem(STORAGE_KEYS.UI_SIZE) || UI_SIZE.DEFAULT)

  // 系统暗色模式检测
  const systemDarkQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const isSystemDark = ref(systemDarkQuery.matches)

  // 根据 theme 与系统偏好计算实际 dark 状态
  const isDark = computed(() => {
    if (theme.value === THEME_MODE.DARK) return true
    if (theme.value === THEME_MODE.LIGHT) return false
    return isSystemDark.value
  })

  // 应用到 DOM 与 Element Plus
  const applyThemeToDom = () => {
    const root = document.documentElement
    if (isDark.value) {
      root.classList.add('dark')
      root.setAttribute('data-theme', 'dark')
    } else {
      root.classList.remove('dark')
      root.setAttribute('data-theme', 'light')
    }
  }

  const applyLangToDom = () => {
    const root = document.documentElement
    root.setAttribute('lang', language.value)
  }

  const setTheme = (val) => { theme.value = val }
  const setLanguage = (val) => { language.value = val }
  const setUiSize = (val) => { uiSize.value = val }

  // 持久化
  watch(theme, (val) => localStorage.setItem(STORAGE_KEYS.THEME, val))
  watch(language, (val) => localStorage.setItem(STORAGE_KEYS.LANGUAGE, val))
  watch(uiSize, (val) => localStorage.setItem(STORAGE_KEYS.UI_SIZE, val))

  // 响应系统主题变化（当为 AUTO 时生效）
  const onSystemSchemeChange = (e) => {
    isSystemDark.value = e.matches
    if (theme.value === THEME_MODE.AUTO) {
      applyThemeToDom()
    }
  }
  systemDarkQuery.addEventListener?.('change', onSystemSchemeChange)

  // 初始化生效
  applyThemeToDom()
  applyLangToDom()

  return {
    // state
    theme,
    language,
    uiSize,
    // getters
    isDark,
    // actions
    setTheme,
    setLanguage,
    setUiSize,
    applyThemeToDom,
    applyLangToDom
  }
})