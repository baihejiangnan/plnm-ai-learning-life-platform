package com.example.service;

import com.example.entity.LogoutLog;
import com.example.mapper.LogoutLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 登出日志服务层
 */
@Service
public class LogoutLogService {

    @Autowired
    private LogoutLogMapper logoutLogMapper;

    /**
     * 记录登出日志
     */
    public void recordLogout(Integer userId, String username, HttpServletRequest request, String logoutType) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            LogoutLog logoutLog = new LogoutLog(userId, username, ipAddress, userAgent, logoutType);
            logoutLogMapper.insert(logoutLog);
        } catch (Exception e) {
            // 记录日志失败不应该影响登出流程，只记录错误
            System.err.println("记录登出日志失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询登出日志
     */
    public List<LogoutLog> getLogoutLogsByUserId(Integer userId) {
        return logoutLogMapper.selectByUserId(userId);
    }

    /**
     * 根据用户名查询登出日志
     */
    public List<LogoutLog> getLogoutLogsByUsername(String username) {
        return logoutLogMapper.selectByUsername(username);
    }

    /**
     * 分页查询所有登出日志
     */
    public List<LogoutLog> getAllLogoutLogs(int page, int size) {
        int offset = (page - 1) * size;
        return logoutLogMapper.selectAll(size, offset);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}