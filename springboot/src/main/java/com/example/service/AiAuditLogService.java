package com.example.service;

import com.example.entity.AiAuditLog;
import com.example.mapper.AiAuditLogMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiAuditLogService {

    @Autowired
    private AiAuditLogMapper mapper;

    @Value("${app.ai.audit.retention-days:90}")
    private Integer retentionDays;

    public void record(AiAuditLog log) {
        if (log == null) return;
        if (log.getCreatedTime() == null) {
            log.setCreatedTime(LocalDateTime.now());
        }
        mapper.insert(log);
    }

    public int cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays == null ? 90 : retentionDays);
        return mapper.deleteBefore(cutoff);
    }

    public PageInfo<AiAuditLog> list(Integer userId, String roleId, Integer success, String keyword, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<AiAuditLog> list = mapper.selectList(userId, roleId, success, keyword, startTime, endTime);
        return new PageInfo<>(list);
    }
}
