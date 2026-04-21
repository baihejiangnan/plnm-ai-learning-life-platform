/**
 * 表格相关组合式函数
 */
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DEFAULT_PAGE_SIZE } from '@/constants'

/**
 * 表格数据管理
 * @param {Function} fetchApi - 获取数据的API函数
 * @param {Function} deleteApi - 删除数据的API函数
 * @param {Object} options - 配置选项
 * @returns {Object} 表格相关的响应式数据和方法
 */
export function useTable(fetchApi, deleteApi = null, options = {}) {
  const {
    immediate = true,
    pageSize = DEFAULT_PAGE_SIZE,
    searchForm = {},
    transform = null
  } = options

  // 响应式数据
  const loading = ref(false)
  const tableData = ref([])
  const total = ref(0)
  const selectedRows = ref([])
  
  // 分页参数
  const pagination = reactive({
    pageNum: 1,
    pageSize: pageSize
  })
  
  // 搜索表单
  const searchParams = reactive({ ...searchForm })
  
  // 计算属性
  const hasSelection = computed(() => selectedRows.value.length > 0)
  const isAllSelected = computed(() => {
    return tableData.value.length > 0 && selectedRows.value.length === tableData.value.length
  })
  
  /**
   * 获取表格数据
   * @param {Object} params - 额外参数
   */
  const fetchData = async (params = {}) => {
    try {
      loading.value = true
      const requestParams = {
        ...pagination,
        ...searchParams,
        ...params
      }
      
      const response = await fetchApi(requestParams)
      const { data } = response
      
      if (transform && typeof transform === 'function') {
        tableData.value = transform(data.records || data.list || data)
      } else {
        tableData.value = data.records || data.list || data
      }
      
      total.value = data.total || 0
      
      // 清空选中项
      selectedRows.value = []
    } catch (error) {
      console.error('获取表格数据失败:', error)
      ElMessage.error('获取数据失败')
      tableData.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }
  
  /**
   * 刷新数据
   */
  const refresh = () => {
    fetchData()
  }
  
  /**
   * 重置并刷新
   */
  const reset = () => {
    pagination.pageNum = 1
    Object.keys(searchParams).forEach(key => {
      searchParams[key] = searchForm[key] || ''
    })
    fetchData()
  }
  
  /**
   * 搜索
   */
  const search = () => {
    pagination.pageNum = 1
    fetchData()
  }
  
  /**
   * 页码改变
   * @param {number} page - 页码
   */
  const handlePageChange = (page) => {
    pagination.pageNum = page
    fetchData()
  }
  
  /**
   * 页面大小改变
   * @param {number} size - 页面大小
   */
  const handleSizeChange = (size) => {
    pagination.pageSize = size
    pagination.pageNum = 1
    fetchData()
  }
  
  /**
   * 选择改变
   * @param {Array} selection - 选中的行
   */
  const handleSelectionChange = (selection) => {
    selectedRows.value = selection
  }
  
  /**
   * 删除单行
   * @param {Object} row - 行数据
   * @param {string} confirmText - 确认文本
   */
  const handleDelete = async (row, confirmText = '确定要删除这条记录吗？') => {
    if (!deleteApi) {
      console.warn('未提供删除API函数')
      return
    }
    
    try {
      await ElMessageBox.confirm(confirmText, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      await deleteApi(row.id)
      ElMessage.success('删除成功')
      
      // 如果当前页只有一条数据且不是第一页，则回到上一页
      if (tableData.value.length === 1 && pagination.pageNum > 1) {
        pagination.pageNum--
      }
      
      fetchData()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
        ElMessage.error('删除失败')
      }
    }
  }
  
  /**
   * 批量删除
   * @param {string} confirmText - 确认文本
   */
  const handleBatchDelete = async (confirmText = `确定要删除选中的 ${selectedRows.value.length} 条记录吗？`) => {
    if (!deleteApi) {
      console.warn('未提供删除API函数')
      return
    }
    
    if (selectedRows.value.length === 0) {
      ElMessage.warning('请选择要删除的记录')
      return
    }
    
    try {
      await ElMessageBox.confirm(confirmText, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      const ids = selectedRows.value.map(row => row.id)
      await Promise.all(ids.map(id => deleteApi(id)))
      
      ElMessage.success('批量删除成功')
      
      // 如果当前页数据全部删除且不是第一页，则回到上一页
      if (selectedRows.value.length === tableData.value.length && pagination.pageNum > 1) {
        pagination.pageNum--
      }
      
      fetchData()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('批量删除失败:', error)
        ElMessage.error('批量删除失败')
      }
    }
  }
  
  // 立即执行
  if (immediate) {
    fetchData()
  }
  
  return {
    // 响应式数据
    loading,
    tableData,
    total,
    selectedRows,
    pagination,
    searchParams,
    
    // 计算属性
    hasSelection,
    isAllSelected,
    
    // 方法
    fetchData,
    refresh,
    reset,
    search,
    handlePageChange,
    handleSizeChange,
    handleSelectionChange,
    handleDelete,
    handleBatchDelete
  }
}