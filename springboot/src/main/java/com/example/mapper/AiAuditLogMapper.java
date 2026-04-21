package com.example.mapper;

import com.example.entity.AiAuditLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiAuditLogMapper {
    @Insert("INSERT INTO ai_audit_log(user_id, role_id, action_type, module, request_text, response_text, success, latency_ms, trace_id, created_time) " +
            "VALUES(#{userId}, #{roleId}, #{actionType}, #{module}, #{requestText}, #{responseText}, #{success}, #{latencyMs}, #{traceId}, #{createdTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiAuditLog log);

    @Delete("DELETE FROM ai_audit_log WHERE created_time < #{beforeTime}")
    int deleteBefore(@Param("beforeTime") LocalDateTime beforeTime);

    @Select("<script>" +
            "SELECT * FROM ai_audit_log WHERE user_id = #{userId} " +
            "<if test='roleId != null and roleId != \"\"'> AND role_id = #{roleId} </if>" +
            "<if test='success != null'> AND success = #{success} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (request_text LIKE CONCAT('%',#{keyword},'%') OR response_text LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='startTime != null'> AND created_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_time &lt;= #{endTime} </if>" +
            "ORDER BY created_time DESC, id DESC" +
            "</script>")
    List<AiAuditLog> selectList(@Param("userId") Integer userId,
                                @Param("roleId") String roleId,
                                @Param("success") Integer success,
                                @Param("keyword") String keyword,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
