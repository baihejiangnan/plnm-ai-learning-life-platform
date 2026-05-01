import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api'
import { useUserStore } from '@/stores/user'

const defaultSettings = {
  enabled: true,
  desktop: true,
  emailDigest: false,
  quietHours: false,
  sound: true,
  priorityOnly: false,
  digestTime: '21:30',
  categories: ['system', 'security', 'collaboration']
}

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    list: [],
    total: 0,
    unreadCount: 0,
    loading: false,
    overview: { total: 0, unreadCount: 0, typeCounts: {}, recent: [] },
    settings: { ...defaultSettings }
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
          const unreadInList = this.list.filter(n => n.status === 'unread').length
          this.unreadCount = Number.isFinite(Number(data.unreadCount))
            ? Number(data.unreadCount)
            : (status === 'unread' || status === 'all' ? unreadInList : (this.unreadCount || 0))
          this.overview = {
            ...this.overview,
            total: Number.isFinite(Number(data.total)) ? Number(data.total) : this.overview.total,
            unreadCount: this.unreadCount,
            typeCounts: data.typeCounts || this.overview.typeCounts || {}
          }
        } else {
          ElMessage.error(res?.msg || '获取通知失败')
        }
      } catch (e) {
        ElMessage.error(e?.message || '获取通知失败')
      } finally {
        this.loading = false
      }
    },
    async fetchOverview() {
      const userStore = useUserStore()
      const uid = userStore.userInfo?.id
      if (!uid) return this.overview
      this.loading = true
      try {
        const res = await notificationApi.overview({ userId: uid })
        if (res && res.code === 200) {
          const data = res.data || {}
          const recent = data.recent || []
          this.overview = {
            total: Number(data.total || 0),
            unreadCount: Number(data.unreadCount || 0),
            typeCounts: data.typeCounts || {},
            recent
          }
          this.unreadCount = this.overview.unreadCount
          this.total = this.overview.total
          this.list = recent
          if (data.settings) {
            this.settings = { ...defaultSettings, ...data.settings }
          }
          return this.overview
        }
        ElMessage.error(res?.msg || '获取通知概览失败')
        return this.overview
      } catch (e) {
        ElMessage.error(e?.message || '获取通知概览失败')
        return this.overview
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
          const readDelta = this.list.filter(n => ids.includes(n.id) && n.status === 'unread').length || ids.length
          this.list = this.list.map(n => ids.includes(n.id) ? { ...n, status: 'read' } : n)
          this.overview.recent = (this.overview.recent || []).map(n => ids.includes(n.id) ? { ...n, status: 'read' } : n)
          this.unreadCount = Math.max(0, this.unreadCount - readDelta)
          this.overview = { ...this.overview, unreadCount: this.unreadCount }
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
          const removedUnread = this.list.filter(n => ids.includes(n.id) && n.status === 'unread').length
          this.list = this.list.filter(n => !ids.includes(n.id))
          this.overview.recent = (this.overview.recent || []).filter(n => !ids.includes(n.id))
          this.unreadCount = Math.max(0, this.unreadCount - removedUnread)
          this.total = Math.max(0, this.total - ids.length)
          this.overview = { ...this.overview, total: this.total, unreadCount: this.unreadCount }
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
          this.settings = { ...defaultSettings, ...(res.data || {}) }
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
