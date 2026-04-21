import { defineStore } from 'pinia'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api'
import { useUserStore } from '@/stores/user'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    list: [],
    total: 0,
    unreadCount: 0,
    loading: false,
    settings: { enabled: true, categories: ['system', 'security', 'collaboration'] }
  }),
  actions: {
    async fetchList(status = 'unread', page = 1, size = 10) {
      const userStore = useUserStore()
      const uid = userStore.userInfo?.id
      if (!uid) return
      this.loading = true
      try {
        const res = await notificationApi.getList({ userId: uid, status, page, size })
        if (res && res.code === 200) {
          const data = res.data || {}
          this.list = data.list || []
          this.total = data.total || 0
          this.unreadCount = status === 'unread' ? (this.list?.length || 0) : (this.unreadCount || 0)
        } else {
          ElMessage.error(res?.msg || '获取通知失败')
        }
      } catch (e) {
        ElMessage.error(e?.message || '获取通知失败')
      } finally {
        this.loading = false
      }
    },
    async markRead(ids = []) {
      const userStore = useUserStore()
      const uid = userStore.userInfo?.id
      if (!uid || !ids.length) return false
      try {
        const res = await notificationApi.markRead({ userId: uid, ids })
        if (res && res.code === 200) {
          // 本地同步
          this.list = this.list.map(n => ids.includes(n.id) ? { ...n, status: 'read' } : n)
          this.unreadCount = Math.max(0, this.unreadCount - ids.length)
          return true
        }
        ElMessage.error(res?.msg || '标记已读失败')
        return false
      } catch (e) {
        ElMessage.error(e?.message || '标记已读失败')
        return false
      }
    },
    async remove(ids = []) {
      const userStore = useUserStore()
      const uid = userStore.userInfo?.id
      if (!uid || !ids.length) return false
      try {
        const res = await notificationApi.delete({ userId: uid, ids })
        if (res && res.code === 200) {
          this.list = this.list.filter(n => !ids.includes(n.id))
          this.unreadCount = Math.max(0, this.unreadCount - ids.filter(id => this.list.find(n => n.id === id && n.status === 'unread')).length)
          return true
        }
        ElMessage.error(res?.msg || '删除通知失败')
        return false
      } catch (e) {
        ElMessage.error(e?.message || '删除通知失败')
        return false
      }
    },
    async fetchSettings() {
      const userStore = useUserStore()
      const uid = userStore.userInfo?.id
      if (!uid) return this.settings
      try {
        const res = await notificationApi.getSettings({ userId: uid })
        if (res && res.code === 200) {
          this.settings = res.data || this.settings
          return this.settings
        }
        ElMessage.error(res?.msg || '获取通知设置失败')
        return this.settings
      } catch (e) {
        ElMessage.error(e?.message || '获取通知设置失败')
        return this.settings
      }
    },
    async updateSettings(payload) {
      const userStore = useUserStore()
      const uid = userStore.userInfo?.id
      if (!uid) return false
      try {
        const res = await notificationApi.updateSettings({ userId: uid }, payload)
        if (res && res.code === 200) {
          this.settings = { ...this.settings, ...payload }
          return true
        }
        ElMessage.error(res?.msg || '保存设置失败')
        return false
      } catch (e) {
        ElMessage.error(e?.message || '保存设置失败')
        return false
      }
    }
  }
})