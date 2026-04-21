package com.example.mapper;

import com.example.entity.LogoutLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 登出日志数据访问层
 */
@Mapper
public interface LogoutLogMapper {

    /**
     * 插入登出日志
     */
    @Insert("INSERT INTO logout_log (user_id, username, logout_time, ip_address, user_agent, logout_type, remark) " +
            "VALUES (#{userId}, #{username}, #{logoutTime}, #{ipAddress}, #{userAgent}, #{logoutType}, #{remark})")
    void insert(LogoutLog logoutLog);

    /**
     * 根据用户ID查询登出日志
     */
    @Select("SELECT * FROM logout_log WHERE user_id = #{userId} ORDER BY logout_time DESC")
    List<LogoutLog> selectByUserId(Integer userId);

    /**
     * 查询所有登出日志
     */
    @Select("SELECT * FROM logout_log ORDER BY logout_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<LogoutLog> selectAll(int limit, int offset);

    /**
     * 根据用户名查询登出日志
     */
    @Select("SELECT * FROM logout_log WHERE username = #{username} ORDER BY logout_time DESC")
    List<LogoutLog> selectByUsername(String username);
}