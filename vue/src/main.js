import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/css/global.css'
import { usePreferenceStore } from '@/stores/preference'
import { LANGUAGES } from '@/constants'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

// 偏好：语言与尺寸需要在 ElementPlus 安装前解析
const pref = usePreferenceStore()
const locale = pref.language === LANGUAGES.EN_US ? en : zhCn
const size = pref.uiSize

app.use(router)
app.use(ElementPlus, {
    locale,
    size,
})

// 注册所有Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 初始应用主题/语言到 DOM
pref.applyThemeToDom()
pref.applyLangToDom()

app.mount('#app')