import { defineStore } from 'pinia'
import { noteApi, tagApi } from '@/api'
import { ElMessage } from 'element-plus'
import { useUserStore } from './user'

export const useNoteStore = defineStore('note', {
  state: () => ({
    // 笔记相关状态
    notes: [],
    currentNote: null,
    noteLoading: false,
    noteTotal: 0,
    notePage: 1,
    noteSize: 10,
    noteKeyword: '',
    selectedTagId: null,
    // 新增：排序相关
    sortBy: 'updateTime',
    sortOrder: 'desc',
    
    // 标签相关状态
    tags: [],
    popularTags: [],
    tagLoading: false,
    currentTag: null
  }),

  getters: {
    // 获取当前页笔记
    currentPageNotes: (state) => state.notes,
    
    // 获取笔记总数
    totalNotes: (state) => state.noteTotal,
    
    // 获取分页信息
    pagination: (state) => ({
      current: state.notePage,
      size: state.noteSize,
      total: state.noteTotal
    }),
    
    // 获取搜索条件
    searchParams: (state) => ({
      keyword: state.noteKeyword,
      tagId: state.selectedTagId,
      page: state.notePage,
      size: state.noteSize,
      sortBy: state.sortBy,
      sortOrder: state.sortOrder
    }),
    
    // 获取所有标签
    allTags: (state) => state.tags,
    
    // 获取热门标签
    hotTags: (state) => state.popularTags
  },

  actions: {
    // 内部：本地排序兜底
    sortNotesLocally() {
      if (!Array.isArray(this.notes) || !this.sortBy) return
      const by = this.sortBy
      const order = this.sortOrder === 'asc' ? 1 : -1
      const normalize = (v) => {
        if (v == null) return 0
        // 处理日期字符串
        if (typeof v === 'string' && /\d{4}-\d{2}-\d{2}/.test(v)) {
          const t = Date.parse(v)
          return isNaN(t) ? v : t
        }
        return v
      }
      this.notes = [...this.notes].sort((a, b) => {
        let va
        let vb
        switch (by) {
          case 'createTime':
          case 'createdTime':
            va = a.createdTime || a.createTime
            vb = b.createdTime || b.createTime
            break
          case 'updateTime':
          case 'updatedTime':
            va = a.updatedTime || a.updateTime
            vb = b.updatedTime || b.updateTime
            break
          case 'title':
            va = (a.title || '').toLowerCase()
            vb = (b.title || '').toLowerCase()
            break
          case 'viewCount':
            va = a.viewCount ?? 0
            vb = b.viewCount ?? 0
            break
          case 'wordCount':
            va = a.wordCount ?? 0
            vb = b.wordCount ?? 0
            break
          default:
            va = a[by]
            vb = b[by]
        }
        va = normalize(va)
        vb = normalize(vb)
        if (va < vb) return -1 * order
        if (va > vb) return 1 * order
        return 0
      })
    },

    // 获取笔记列表
    async fetchNotes(params = {}) {
      this.noteLoading = true
      try {
        const userStore = useUserStore()
        const userId = userStore.userInfo?.id
        
        if (!userId) {
          ElMessage.error('用户未登录')
          return
        }
        
        const searchParams = {
          userId,
          page: this.notePage,
          size: this.noteSize,
          keyword: this.noteKeyword,
          tagId: this.selectedTagId,
          sortBy: this.sortBy,
          sortOrder: this.sortOrder,
          ...params
        }
        
        // 同步外部传入的排序参数
        if (params.sortBy) this.sortBy = params.sortBy
        if (params.sortOrder) this.sortOrder = params.sortOrder
        
        const response = await noteApi.getList(searchParams)
        if (response.code === 200) {
          const pageData = response.data || {}
          // 兼容 PageHelper 的 PageInfo.list 与自定义 records 两种结构
          this.notes = pageData.list || pageData.records || []
          this.noteTotal = pageData.total || (Array.isArray(this.notes) ? this.notes.length : 0)
          // 本地排序兜底（防止后端未实现排序）
          this.sortNotesLocally()
        } else {
          ElMessage.error(response.msg || '获取笔记列表失败')
        }
      } catch (error) {
        console.error('获取笔记列表失败:', error)
        ElMessage.error('获取笔记列表失败')
      } finally {
        this.noteLoading = false
      }
    },

    // 设置排序
    setSort(sortBy, sortOrder = 'desc') {
      this.sortBy = sortBy
      this.sortOrder = sortOrder
    },

    // 获取笔记详情
    async fetchNoteById(id) {
      this.noteLoading = true
      try {
        const response = await noteApi.getById(id)
        if (response.code === 200) {
          this.currentNote = response.data
          // 取消前端手动增加浏览量，避免与后端重复计数（后端 getNoteById 已自增）
          return response.data
        } else {
          ElMessage.error(response.msg || '获取笔记详情失败')
          return null
        }
      } catch (error) {
        console.error('获取笔记详情失败:', error)
        ElMessage.error('获取笔记详情失败')
        return null
      } finally {
        this.noteLoading = false
      }
    },

    // 创建笔记（成功提示交由组件处理）
    async createNote(noteData) {
      try {
        const response = await noteApi.create(noteData)
        if (response.code === 200) {
          await this.fetchNotes() // 刷新列表
          return response.data
        } else {
          ElMessage.error(response.msg || '创建笔记失败')
          return null
        }
      } catch (error) {
        console.error('创建笔记失败:', error)
        ElMessage.error('创建笔记失败')
        return null
      }
    },

    // 更新笔记（成功提示交由组件处理）
    async updateNote(id, noteData) {
      try {
        console.log('noteStore.updateNote - 接收到的参数:')
        console.log('id:', id)
        console.log('noteData:', noteData)
        
        const response = await noteApi.update(id, noteData)
        console.log('noteApi.update - 响应:', response)
        
        if (response.code === 200) {
          await this.fetchNotes() // 刷新列表
          return response.data
        } else {
          ElMessage.error(response.msg || '更新笔记失败')
          return null
        }
      } catch (error) {
        console.error('更新笔记失败:', error)
        ElMessage.error('更新笔记失败')
        return null
      }
    },

    // 删除笔记（成功提示交由组件处理）
    async deleteNote(id) {
      try {
        const response = await noteApi.delete(id)
        if (response.code === 200) {
          await this.fetchNotes() // 刷新列表
          return true
        } else {
          ElMessage.error(response.msg || '删除笔记失败')
          return false
        }
      } catch (error) {
        console.error('删除笔记失败:', error)
        ElMessage.error('删除笔记失败')
        return false
      }
    },

    // 新增：批量删除（单接口循环调用，完成后统一刷新）
    async deleteNotesBatch(ids = []) {
      if (!Array.isArray(ids) || ids.length === 0) return { success: 0, fail: 0 }
      let success = 0
      let fail = 0
      for (const id of ids) {
        try {
          const res = await noteApi.delete(id)
          if (res.code === 200) success++
          else fail++
        } catch (e) {
          console.error('批量删除失败项:', id, e)
          fail++
        }
      }
      await this.fetchNotes()
      return { success, fail }
    },

    // 搜索笔记
    async searchNotes(keyword) {
      this.noteKeyword = keyword
      this.notePage = 1 // 重置页码
      await this.fetchNotes()
    },

    // 按标签筛选
    async filterByTag(tagId) {
      this.selectedTagId = tagId
      this.notePage = 1 // 重置页码
      await this.fetchNotes()
    },

    // 增加笔记浏览量
    async incrementNoteView(id) {
      try {
        await noteApi.incrementView(id)
      } catch (error) {
        console.error('增加浏览量失败:', error)
      }
    },

    // 设置分页
    setPage(page) {
      this.notePage = page
    },

    // 设置每页大小
    setPageSize(size) {
      this.noteSize = size
      this.notePage = 1 // 重置页码
    },

    // 清空搜索条件
    clearSearch() {
      this.noteKeyword = ''
      this.selectedTagId = null
      this.notePage = 1
    },

    // 获取标签列表
    async fetchTags() {
      this.tagLoading = true
      try {
        const userStore = useUserStore()
        const userId = userStore.userInfo?.id
        
        if (!userId) {
          ElMessage.error('用户未登录')
          return
        }
        
        const response = await tagApi.getList({ userId })
        if (response.code === 200) {
          const list = response.data || []
          // 兼容后端返回字段：如果没有 noteCount，则使用 useCount 映射
          this.tags = list.map(t => ({
            ...t,
            noteCount: t?.noteCount ?? t?.useCount ?? 0
          }))
        } else {
          ElMessage.error(response.msg || '获取标签列表失败')
        }
      } catch (error) {
        console.error('获取标签列表失败:', error)
        ElMessage.error('获取标签列表失败')
      } finally {
        this.tagLoading = false
      }
    },

    // 获取热门标签
    async fetchPopularTags(limit = 10) {
      try {
        const userStore = useUserStore()
        const userId = userStore.userInfo?.id
        if (!userId) {
          ElMessage.error('用户未登录')
          return
        }
        const response = await tagApi.getPopular({ userId, limit })
        if (response.code === 200) {
          this.popularTags = response.data || []
        } else {
          ElMessage.error(response.msg || '获取热门标签失败')
        }
      } catch (error) {
        console.error('获取热门标签失败:', error)
        ElMessage.error('获取热门标签失败')
      }
    },

    // 创建标签
    async createTag(tagData) {
      try {
        const response = await tagApi.create(tagData)
        if (response.code === 200) {
          await this.fetchTags()
          return response.data
        } else {
          ElMessage.error(response.msg || '创建标签失败')
          return null
        }
      } catch (error) {
        console.error('创建标签失败:', error)
        ElMessage.error('创建标签失败')
        return null
      }
    },

    // 更新标签
    async updateTag(id, tagData) {
      try {
        const response = await tagApi.update(id, tagData)
        if (response.code === 200) {
          await this.fetchTags()
          return response.data
        } else {
          ElMessage.error(response.msg || '更新标签失败')
          return null
        }
      } catch (error) {
        console.error('更新标签失败:', error)
        ElMessage.error('更新标签失败')
        return null
      }
    },

    // 删除标签
    async deleteTag(id) {
      try {
        const response = await tagApi.delete(id)
        if (response.code === 200) {
          await this.fetchTags()
          return true
        } else {
          ElMessage.error(response.msg || '删除标签失败')
          return false
        }
      } catch (error) {
        console.error('删除标签失败:', error)
        ElMessage.error('删除标签失败')
        return false
      }
    },

    // 搜索标签
    async searchTags(keyword) {
      try {
        const userStore = useUserStore()
        const userId = userStore.userInfo?.id
        if (!userId) {
          ElMessage.error('用户未登录')
          return
        }
        const response = await tagApi.search({ userId, keyword })
        if (response.code === 200) {
          this.tags = response.data || []
        } else {
          ElMessage.error(response.msg || '搜索标签失败')
        }
      } catch (error) {
        console.error('搜索标签失败:', error)
        ElMessage.error('搜索标签失败')
      }
    },

    resetState() {
      this.notes = []
      this.currentNote = null
      this.noteLoading = false
      this.noteTotal = 0
      this.notePage = 1
      this.noteSize = 10
      this.noteKeyword = ''
      this.selectedTagId = null
      this.sortBy = 'updateTime'
      this.sortOrder = 'desc'
      
      this.tags = []
      this.popularTags = []
      this.tagLoading = false
      this.currentTag = null
    }
  }
})
