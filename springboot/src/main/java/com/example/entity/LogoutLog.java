package com.example.entity;

import java.time.LocalDateTime;

/**
 * 登出日志实体类
 */
public class LogoutLog {

    /** ID */
    private Integer id;
    /** 用户ID */
    private Integer userId;
    /** 用户名 */
    private String username;
    /** 登出时间 */
    private LocalDateTime logoutTime;
    /** 登出IP地址 */
    private String ipAddress;
    /** 用户代理信息 */
    private String userAgent;
    /** 登出方式 (主动登出/token过期等) */
    private String logoutType;
    /** 备注 */
    private String remark;

    public LogoutLog() {}

    public LogoutLog(Integer userId, String username, String ipAddress, String userAgent, String logoutType) {
        this.userId = userId;
        this.username = username;
        this.logoutTime = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.logoutType = logoutType;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getLogoutType() {
        return logoutType;
    }

    public void setLogoutType(String logoutType) {
        this.logoutType = logoutType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "LogoutLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", logoutTime=" + logoutTime +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", logoutType='" + logoutType + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}