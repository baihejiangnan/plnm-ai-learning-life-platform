package com.example.job;

import com.example.service.AiAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiAuditRetentionJob {

    @Autowired
    private AiAuditLogService aiAuditLogService;

    @Scheduled(cron = "${app.ai.audit.cleanup-cron:0 0 3 * * ?}")
    public void cleanup() {
        aiAuditLogService.cleanup();
    }
}
