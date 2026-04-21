/**
 * 表单相关组合式函数
 */
import { ref, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 表单数据管理
 * @param {Object} initialData - 初始数据
 * @param {Object} options - 配置选项
 * @returns {Object} 表单相关的响应式数据和方法
 */
export function useForm(initialData = {}, options = {}) {
  const {
    resetAfterSubmit = false,
    validateOnSubmit = true
  } = options

  // 表单引用
  const formRef = ref(null)
  
  // 表单数据
  const formData = reactive({ ...initialData })
  
  // 表单状态
  const loading = ref(false)
  const isEdit = ref(false)
  
  /**
   * 重置表单
   */
  const resetForm = () => {
    if (formRef.value) {
      formRef.value.resetFields()
    }
    Object.keys(formData).forEach(key => {
      formData[key] = initialData[key] || ''
    })
    isEdit.value = false
  }
  
  /**
   * 清空验证
   */
  const clearValidate = () => {
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  }
  
  /**
   * 验证表单
   * @returns {Promise<boolean>} 验证结果
   */
  const validateForm = () => {
    return new Promise((resolve) => {
      if (!formRef.value) {
        resolve(false)
        return
      }
      
      formRef.value.validate((valid) => {
        resolve(valid)
      })
    })
  }
  
  /**
   * 验证指定字段
   * @param {string|Array} fields - 字段名或字段名数组
   * @returns {Promise<boolean>} 验证结果
   */
  const validateFields = (fields) => {
    return new Promise((resolve) => {
      if (!formRef.value) {
        resolve(false)
        return
      }
      
      formRef.value.validateField(fields, (valid) => {
        resolve(valid)
      })
    })
  }
  
  /**
   * 设置表单数据
   * @param {Object} data - 数据对象
   */
  const setFormData = (data) => {
    Object.keys(data).forEach(key => {
      if (key in formData) {
        formData[key] = data[key]
      }
    })
    isEdit.value = true
  }
  
  /**
   * 获取表单数据
   * @returns {Object} 表单数据副本
   */
  const getFormData = () => {
    return { ...formData }
  }
  
  /**
   * 提交表单
   * @param {Function} submitApi - 提交API函数
   * @param {Object} options - 提交选项
   * @returns {Promise} 提交结果
   */
  const submitForm = async (submitApi, submitOptions = {}) => {
    const {
      successMessage = isEdit.value ? '更新成功' : '创建成功',
      errorMessage = isEdit.value ? '更新失败' : '创建失败',
      transform = null,
      onSuccess = null,
      onError = null
    } = submitOptions
    
    try {
      // 验证表单
      if (validateOnSubmit) {
        const valid = await validateForm()
        if (!valid) {
          return false
        }
      }
      
      loading.value = true
      
      // 准备提交数据
      let submitData = getFormData()
      if (transform && typeof transform === 'function') {
        submitData = transform(submitData)
      }
      
      // 调用API
      const result = await submitApi(submitData)
      
      // 成功处理
      ElMessage.success(successMessage)
      
      if (resetAfterSubmit) {
        resetForm()
      }
      
      if (onSuccess && typeof onSuccess === 'function') {
        onSuccess(result)
      }
      
      return result
    } catch (error) {
      console.error('表单提交失败:', error)
      ElMessage.error(errorMessage)
      
      if (onError && typeof onError === 'function') {
        onError(error)
      }
      
      return false
    } finally {
      loading.value = false
    }
  }
  
  return {
    // 响应式数据
    formRef,
    formData,
    loading,
    isEdit,
    
    // 方法
    resetForm,
    clearValidate,
    validateForm,
    validateFields,
    setFormData,
    getFormData,
    submitForm
  }
}

/**
 * 对话框表单管理
 * @param {Object} initialData - 初始数据
 * @param {Object} options - 配置选项
 * @returns {Object} 对话框表单相关的响应式数据和方法
 */
export function useDialogForm(initialData = {}, options = {}) {
  const {
    title = { create: '新增', edit: '编辑' },
    width = '600px'
  } = options
  
  // 继承表单功能
  const formMethods = useForm(initialData, options)
  
  // 对话框状态
  const dialogVisible = ref(false)
  const dialogTitle = ref('')
  const dialogWidth = ref(width)
  
  /**
   * 打开新增对话框
   */
  const openCreateDialog = () => {
    formMethods.resetForm()
    dialogTitle.value = title.create
    dialogVisible.value = true
    
    nextTick(() => {
      formMethods.clearValidate()
    })
  }
  
  /**
   * 打开编辑对话框
   * @param {Object} data - 编辑数据
   */
  const openEditDialog = (data) => {
    formMethods.setFormData(data)
    dialogTitle.value = title.edit
    dialogVisible.value = true
    
    nextTick(() => {
      formMethods.clearValidate()
    })
  }
  
  /**
   * 关闭对话框
   */
  const closeDialog = () => {
    dialogVisible.value = false
    formMethods.resetForm()
  }
  
  /**
   * 提交对话框表单
   * @param {Function} submitApi - 提交API函数
   * @param {Object} options - 提交选项
   * @returns {Promise} 提交结果
   */
  const submitDialogForm = async (submitApi, submitOptions = {}) => {
    const defaultOptions = {
      onSuccess: () => {
        closeDialog()
      },
      ...submitOptions
    }
    
    return await formMethods.submitForm(submitApi, defaultOptions)
  }
  
  return {
    // 继承表单方法
    ...formMethods,
    
    // 对话框相关
    dialogVisible,
    dialogTitle,
    dialogWidth,
    
    // 对话框方法
    openCreateDialog,
    openEditDialog,
    closeDialog,
    submitDialogForm
  }
}

/**
 * 搜索表单管理
 * @param {Object} initialData - 初始搜索条件
 * @param {Function} searchCallback - 搜索回调函数
 * @returns {Object} 搜索表单相关的响应式数据和方法
 */
export function useSearchForm(initialData = {}, searchCallback = null) {
  // 搜索表单数据
  const searchForm = reactive({ ...initialData })
  
  // 搜索表单引用
  const searchFormRef = ref(null)
  
  /**
   * 搜索
   */
  const handleSearch = () => {
    if (searchCallback && typeof searchCallback === 'function') {
      searchCallback(searchForm)
    }
  }
  
  /**
   * 重置搜索
   */
  const handleReset = () => {
    if (searchFormRef.value) {
      searchFormRef.value.resetFields()
    }
    
    Object.keys(searchForm).forEach(key => {
      searchForm[key] = initialData[key] || ''
    })
    
    if (searchCallback && typeof searchCallback === 'function') {
      searchCallback(searchForm)
    }
  }
  
  /**
   * 设置搜索条件
   * @param {Object} data - 搜索条件
   */
  const setSearchForm = (data) => {
    Object.keys(data).forEach(key => {
      if (key in searchForm) {
        searchForm[key] = data[key]
      }
    })
  }
  
  return {
    searchForm,
    searchFormRef,
    handleSearch,
    handleReset,
    setSearchForm
  }
}