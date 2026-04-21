import { ElMessage } from 'element-plus'
import router from '../router'
import axios from "axios";

const request = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    timeout: 30000  // 后台接口超时时间设置
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
request.interceptors.request.use(config => {
    // 对于 FormData（文件上传），不要强制设置 Content-Type，交由浏览器/axios 自动带上 boundary
    if (config.data instanceof FormData) {
        if (config.headers && config.headers['Content-Type']) {
            delete config.headers['Content-Type']
        }
    } else {
        // 其他请求默认使用 JSON
        config.headers['Content-Type'] = 'application/json;charset=utf-8';
    }
    
    // 添加JWT认证头
    const token = localStorage.getItem('system-token')
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`
    }
    
    return config
}, error => {
    console.error('请求错误:', error)
    ElMessage.error('请求发送失败')
    return Promise.reject(error)
});

// response 拦截器
// 可以在接口响应后统一处理结果
request.interceptors.response.use(
    response => {
        let res = response.data;
        // 如果是返回的文件
        if (response.config.responseType === 'blob') {
            return res
        }
        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        
        // 规范化后端返回的状态码，字符串数字转为数字，避免 '200' 与 200 比较失败
        if (res && typeof res.code === 'string' && /^\d+$/.test(res.code)) {
            res.code = Number(res.code)
        }
        
        // 统一处理响应状态码
        const skipAuthRedirect = response.config && response.config.skipAuthRedirect
        if (res.code === 401 || res.code === '401') {
            // 如果是logout接口的401错误，不需要额外处理，让业务逻辑自己处理
            if (response.config.url && response.config.url.includes('/logout')) {
                return Promise.reject(new Error('认证失败'))
            }
            if (!skipAuthRedirect) {
                ElMessage.error('认证失败，请重新登录')
                localStorage.removeItem('system-user')
                localStorage.removeItem('system-token')
                router.push('/login')
            }
            return Promise.reject(new Error('认证失败'))
        } else if (res.code === 403 || res.code === '403') {
            ElMessage.error('权限不足，访问被拒绝')
            return Promise.reject(new Error('权限不足'))
        } else if (res.code === 500 || res.code === '500') {
            // 对于500错误，不在这里显示错误消息，让具体的业务逻辑处理
            // ElMessage.error('服务器内部错误')
            return Promise.reject(new Error(res.msg || '服务器内部错误'))
        }
        
        return res;
    },
    error => {
        console.error('响应错误:', error)
        
        if (error.response) {
            const status = error.response.status
            switch (status) {
                case 401:
                    // 如果是logout接口的401错误，不需要额外处理
                    if (error.config && error.config.url && error.config.url.includes('/logout')) {
                        break
                    }
                    // 在需要静默处理401的场景（例如个人资料更新后的信息刷新）跳过全局重定向
                    if (!(error.config && error.config.skipAuthRedirect)) {
                        ElMessage.error('认证失败，请重新登录')
                        localStorage.removeItem('system-user')
                        localStorage.removeItem('system-token')
                        router.push('/login')
                    }
                    break
                case 403:
                    ElMessage.error('权限不足，访问被拒绝')
                    break
                case 404:
                    ElMessage.error('请求的资源不存在')
                    break
                case 500:
                    ElMessage.error('服务器内部错误')
                    break
                default:
                    ElMessage.error(`请求失败: ${error.response.data?.message || '未知错误'}`)
            }
        } else if (error.request) {
            ElMessage.error('网络连接失败，请检查网络')
        } else {
            ElMessage.error('请求配置错误')
        }
        
        return Promise.reject(error)
    }
)


export default request
